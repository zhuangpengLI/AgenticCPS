import FileApi from '@/sheep/api/infra/file';
import { apiPath, baseUrl, tenantId } from '@/sheep/config';
import { getAccessToken } from '@/sheep/request';
import AiChatApi from './chat';

let recorder;
let recordingPath = '';

export function startVoiceRecording({ onStart, onStop, onError } = {}) {
  // #ifdef H5
  if (!navigator.mediaDevices?.getUserMedia || !window.MediaRecorder) { onError?.(new Error('当前环境不支持录音')); return; }
  navigator.mediaDevices.getUserMedia({ audio: true }).then((stream) => {
    recorder = new MediaRecorder(stream);
    const chunks = [];
    recorder.ondataavailable = (event) => event.data?.size && chunks.push(event.data);
    recorder.onerror = onError;
    recorder.onstop = async () => {
      try {
        const blob = new Blob(chunks, { type: 'audio/webm' });
        if (!blob.size) throw new Error('录音内容为空');
        onStop?.(await uploadBlob(blob, 'voice.webm'));
      } catch (error) {
        onError?.(error);
      } finally {
        stream.getTracks().forEach((track) => track.stop());
      }
    };
    recorder.start(); onStart?.();
  }).catch(onError);
  return;
  // #endif
  // #ifndef H5
  recorder = uni.getRecorderManager();
  // Avoid accumulating callbacks when a user records more than once. These
  // off* methods are available on current mini-program/App runtimes and are
  // optional for older runtimes.
  recorder.offStart?.();
  recorder.offError?.();
  recorder.offStop?.();
  recorder.onStart(() => onStart?.());
  recorder.onError(onError);
  recorder.onStop(async (result) => {
    try {
      recordingPath = result.tempFilePath;
      if (!recordingPath) throw new Error('录音内容为空');
      onStop?.(await uploadPath(recordingPath));
    } catch (error) {
      onError?.(error);
    }
  });
  recorder.start({ duration: 60000, format: 'mp3', sampleRate: 16000, numberOfChannels: 1 });
  // #endif
}

export function stopVoiceRecording() {
  recorder?.stop?.();
}

async function uploadPath(path) {
  const result = await FileApi.uploadFile(path, 'ai/voice');
  return result?.data ? { url: result.data, format: 'mp3' } : null;
}

async function uploadBlob(blob, name) {
  // uni.uploadFile expects a filesystem path and cannot receive a browser File
  // object. Use the same backend endpoint with a standard multipart request on
  // H5, keeping authentication and tenant headers consistent with FileApi.
  if (typeof fetch === 'undefined' || typeof FormData === 'undefined') return null;
  const form = new FormData();
  form.append('file', blob, name);
  form.append('directory', 'ai/voice');
  const response = await fetch(`${baseUrl}${apiPath}/infra/file/upload`, {
    method: 'POST',
    headers: { Accept: '*/*', 'tenant-id': tenantId || '', Authorization: `Bearer ${getAccessToken()}` },
    body: form,
  });
  if (!response.ok) throw new Error('录音上传失败');
  const result = await response.json();
  return result?.data ? { url: result.data, format: 'webm' } : null;
}

export async function transcribeVoice(file, durationMs = 0) {
  if (!file?.url) throw new Error('录音文件为空');
  const result = await AiChatApi.transcribe({ audioUrl: file.url, format: file.format, durationMs });
  if (result?.code !== 0) throw new Error(result?.msg || '语音转写失败');
  if (result.data?.status === 'COMPLETED') return result.data.text || '';
  const requestId = result.data?.requestId;
  if (!requestId) throw new Error('语音转写未返回任务编号');
  for (let attempt = 0; attempt < 12; attempt += 1) {
    await new Promise((resolve) => setTimeout(resolve, 1000));
    const status = await AiChatApi.transcribeStatus(requestId);
    if (status?.data?.status === 'COMPLETED') return status.data.text || '';
    if (status?.data?.status === 'FAILED') throw new Error(status.data.errorMessage || '语音转写失败');
  }
  throw new Error('语音转写超时，请改用文字输入');
}
