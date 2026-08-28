import { asrUrl } from '@/sheep/config';

let recorder;
let activeStream;
let h5Recorder;

function encodeWav(samples, sampleRate) {
  const buffer = new ArrayBuffer(44 + samples.length * 2);
  const view = new DataView(buffer);
  const writeString = (offset, value) => {
    for (let index = 0; index < value.length; index += 1) {
      view.setUint8(offset + index, value.charCodeAt(index));
    }
  };
  writeString(0, 'RIFF');
  view.setUint32(4, 36 + samples.length * 2, true);
  writeString(8, 'WAVE');
  writeString(12, 'fmt ');
  view.setUint32(16, 16, true);
  view.setUint16(20, 1, true);
  view.setUint16(22, 1, true);
  view.setUint32(24, sampleRate, true);
  view.setUint32(28, sampleRate * 2, true);
  view.setUint16(32, 2, true);
  view.setUint16(34, 16, true);
  writeString(36, 'data');
  view.setUint32(40, samples.length * 2, true);
  for (let index = 0; index < samples.length; index += 1) {
    const sample = Math.max(-1, Math.min(1, samples[index]));
    view.setInt16(44 + index * 2, sample < 0 ? sample * 0x8000 : sample * 0x7fff, true);
  }
  return new Blob([view], { type: 'audio/wav' });
}

function resample(samples, fromRate, toRate) {
  if (fromRate === toRate) return samples;
  const outputLength = Math.round(samples.length * toRate / fromRate);
  const output = new Float32Array(outputLength);
  const ratio = fromRate / toRate;
  for (let index = 0; index < outputLength; index += 1) {
    const sourceIndex = index * ratio;
    const left = Math.floor(sourceIndex);
    const right = Math.min(left + 1, samples.length - 1);
    const weight = sourceIndex - left;
    output[index] = samples[left] * (1 - weight) + samples[right] * weight;
  }
  return output;
}

function startH5WavRecording(stream, onStop, onError) {
  const AudioContextClass = window.AudioContext || window.webkitAudioContext;
  if (!AudioContextClass) return false;
  const context = new AudioContextClass({ sampleRate: 16000 });
  const source = context.createMediaStreamSource(stream);
  const processor = context.createScriptProcessor(4096, 1, 1);
  const chunks = [];
  processor.onaudioprocess = (event) => {
    const input = event.inputBuffer.getChannelData(0);
    chunks.push(new Float32Array(input));
  };
  const mute = context.createGain();
  mute.gain.value = 0;
  const resumePromise = context.resume?.();
  resumePromise?.catch?.(() => {});
  source.connect(processor);
  processor.connect(mute);
  mute.connect(context.destination);
  h5Recorder = {
    stop: () => {
      try {
        source.disconnect();
        processor.disconnect();
        mute.disconnect();
        const samples = new Float32Array(chunks.reduce((total, chunk) => total + chunk.length, 0));
        let offset = 0;
        chunks.forEach((chunk) => {
          samples.set(chunk, offset);
          offset += chunk.length;
        });
        if (!samples.length) throw new Error('录音内容为空');
        const wavSamples = resample(samples, context.sampleRate, 16000);
        onStop?.({ blob: encodeWav(wavSamples, 16000), format: 'wav', fileName: 'voice.wav' });
      } catch (error) {
        onError?.(error);
      } finally {
        context.close?.();
        activeStream?.getTracks?.().forEach((track) => track.stop());
        activeStream = null;
        h5Recorder = null;
      }
    },
  };
  return true;
}

/** 跨 H5 / App / 小程序录音，并在结束后把文件交给 onStop。 */
export function startVoiceRecording({ onStart, onStop, onError } = {}) {
  // #ifdef H5
  if (!navigator.mediaDevices?.getUserMedia || !(window.AudioContext || window.webkitAudioContext)) {
    onError?.(new Error('当前环境不支持录音'));
    return;
  }
  navigator.mediaDevices
    .getUserMedia({ audio: true })
    .then((stream) => {
      activeStream = stream;
      if (!startH5WavRecording(stream, onStop, onError)) {
        // MediaRecorder is retained as a capability check for older runtimes;
        // its webm output is not accepted by the SenseVoice endpoint.
        onError?.(new Error('当前浏览器不支持 WAV 录音，请改用文字输入'));
        stream.getTracks?.().forEach((track) => track.stop());
        return;
      }
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
  h5Recorder?.stop?.();
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
