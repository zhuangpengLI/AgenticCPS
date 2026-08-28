import { asrUrl } from '@/sheep/config';

let recorder;
let activeStream;

/** 跨 H5 / App / 小程序录音，并在结束后把文件交给 onStop。 */
export function startVoiceRecording({ onStart, onStop, onError } = {}) {
  // #ifdef H5
  if (!navigator.mediaDevices?.getUserMedia || !window.MediaRecorder) {
    onError?.(new Error('当前环境不支持录音'));
    return;
  }
  navigator.mediaDevices
    .getUserMedia({ audio: true })
    .then((stream) => {
      activeStream = stream;
      const mimeType = MediaRecorder.isTypeSupported?.('audio/webm;codecs=opus')
        ? 'audio/webm;codecs=opus'
        : '';
      recorder = mimeType ? new MediaRecorder(stream, { mimeType }) : new MediaRecorder(stream);
      const chunks = [];
      recorder.ondataavailable = (event) => event.data?.size && chunks.push(event.data);
      recorder.onerror = onError;
      recorder.onstop = () => {
        try {
          const blob = new Blob(chunks, { type: recorder.mimeType || 'audio/webm' });
          if (!blob.size) throw new Error('录音内容为空');
          const format = blob.type.includes('mp4') || blob.type.includes('mpeg') ? 'm4a' : 'webm';
          onStop?.({ blob, format, fileName: `voice.${format}` });
        } catch (error) {
          onError?.(error);
        } finally {
          activeStream?.getTracks?.().forEach((track) => track.stop());
          activeStream = null;
        }
      };
      recorder.start();
      onStart?.();
    })
    .catch(onError);
  return;
  // #endif

  // #ifndef H5
  recorder = uni.getRecorderManager();
  recorder.offStart?.();
  recorder.offError?.();
  recorder.offStop?.();
  recorder.onStart(() => onStart?.());
  recorder.onError(onError);
  recorder.onStop((result) => {
    const filePath = result?.tempFilePath;
    if (!filePath) {
      onError?.(new Error('录音内容为空'));
      return;
    }
    onStop?.({ filePath, format: 'mp3', fileName: 'voice.mp3' });
  });
  recorder.start({ duration: 60000, format: 'mp3', sampleRate: 16000, numberOfChannels: 1 });
  // #endif
}

export function stopVoiceRecording() {
  recorder?.stop?.();
}

function parseResponse(raw) {
  let data = raw;
  if (typeof raw === 'string') {
    try {
      data = JSON.parse(raw);
    } catch (error) {
      throw new Error('语音识别返回格式错误');
    }
  }
  const first = Array.isArray(data?.result) ? data.result[0] : data?.result;
  const text =
    first?.text || first?.clean_text || first?.raw_text || data?.text || data?.data?.text || '';
  if (typeof text !== 'string') throw new Error('语音识别返回格式错误');
  return text.trim();
}

function uploadWithUni(filePath) {
  return new Promise((resolve, reject) => {
    uni.uploadFile({
      url: asrUrl,
      filePath,
      name: 'file',
      header: { Accept: 'application/json' },
      success: (response) => {
        if (response.statusCode && (response.statusCode < 200 || response.statusCode >= 300)) {
          reject(new Error(`语音识别服务异常（${response.statusCode}）`));
          return;
        }
        try {
          resolve(parseResponse(response.data));
        } catch (error) {
          reject(error);
        }
      },
      fail: (error) => reject(new Error(error?.errMsg || '语音识别请求失败')),
    });
  });
}

async function uploadWithFetch(blob, fileName) {
  if (typeof fetch === 'undefined' || typeof FormData === 'undefined')
    throw new Error('当前环境不支持上传录音');
  const form = new FormData();
  form.append('file', blob, fileName);
  const response = await fetch(asrUrl, {
    method: 'POST',
    body: form,
    headers: { Accept: 'application/json' },
  });
  if (!response.ok) throw new Error(`语音识别服务异常（${response.status}）`);
  return parseResponse(await response.json());
}

/** 直接调用可配置的 SenseVoice 接口，字段名固定为 file。 */
export async function transcribeVoice(file) {
  if (!file) throw new Error('录音文件为空');
  if (file.blob) return uploadWithFetch(file.blob, file.fileName || 'voice.webm');
  if (file.filePath) return uploadWithUni(file.filePath);
  throw new Error('录音文件为空');
}
