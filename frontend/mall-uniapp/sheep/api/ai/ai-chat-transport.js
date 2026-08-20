import { apiPath, baseUrl, tenantId } from '@/sheep/config';
import { getAccessToken, getTenantId } from '@/sheep/request';
import AiChatApi from './chat';

function parseEventPayload(payload) {
  if (!payload) return null;
  const text = String(payload).trim().replace(/^data:\s*/i, '').trim();
  if (!text || text === '[DONE]') return null;
  try { return JSON.parse(text); } catch (error) { return null; }
}

function parseChunk(buffer, onEvent, flush = false) {
  const frames = buffer.split(/\r?\n\r?\n|\r?\n/);
  const rest = flush ? '' : frames.pop();
  frames.forEach((frame) => {
    const data = parseEventPayload(frame);
    if (data) onEvent(data);
  });
  return rest;
}

function normalizedResult(event) {
  return event?.data || event;
}

function decodeChunk(value, decoder) {
  if (typeof value === 'string') return value;
  if (value == null) return '';
  try {
    if (decoder) return decoder.decode(value, { stream: true });
    if (typeof TextDecoder !== 'undefined') return new TextDecoder('utf-8').decode(value);
  } catch (error) {
    // Fall through to the small UTF-8 decoder below for older App runtimes.
  }
  const bytes = value instanceof ArrayBuffer ? new Uint8Array(value) : value;
  let binary = '';
  for (let index = 0; index < bytes.length; index += 1) binary += String.fromCharCode(bytes[index]);
  try { return decodeURIComponent(binary.split('').map((char) => `%${char.charCodeAt(0).toString(16).padStart(2, '0')}`).join('')); } catch (error) { return binary; }
}

async function sendFetch(payload, onEvent, signal) {
  const response = await fetch(`${baseUrl}${apiPath}/ai/chat/message/send-stream`, {
    method: 'POST',
    headers: { Accept: 'text/event-stream', 'Content-Type': 'application/json', Authorization: `Bearer ${getAccessToken()}`, 'tenant-id': getTenantId() || tenantId },
    body: JSON.stringify(payload),
    signal,
  });
  if (!response.ok || !response.body) throw new Error('stream unsupported');
  const reader = response.body.getReader();
  const decoder = new TextDecoder('utf-8');
  let buffer = '';
  while (true) {
    const { done, value } = await reader.read();
    if (done) break;
    buffer += decoder.decode(value, { stream: true });
    buffer = parseChunk(buffer, onEvent);
  }
  parseChunk(buffer, onEvent, true);
}

export async function sendAiChatStream(payload, { onEvent, onError, signal } = {}) {
  const emit = (event) => {
    if (event?.code !== undefined && event.code !== 0) {
      onError?.(new Error(event.msg || 'AI 暂时无法响应'));
      return;
    }
    onEvent?.(normalizedResult(event));
  };
  // H5 uses the native Fetch stream. If it is unavailable or fails, the
  // request path below provides a non-streaming/chunked fallback.
  if (typeof fetch === 'function' && typeof window !== 'undefined' && typeof ReadableStream !== 'undefined') {
    try { await sendFetch(payload, emit, signal); return; } catch (error) { if (signal?.aborted) return; }
  }
  try {
    await new Promise((resolve, reject) => {
      let buffer = '';
      let chunkSupported = false;
      const decoder = typeof TextDecoder !== 'undefined' ? new TextDecoder('utf-8') : null;
      const task = uni.request({
        url: `${baseUrl}${apiPath}/ai/chat/message/send-stream`, method: 'POST', timeout: 90000,
        enableChunked: true,
        header: { Accept: 'text/event-stream', 'Content-Type': 'application/json', Authorization: `Bearer ${getAccessToken()}`, 'tenant-id': getTenantId() || tenantId },
        data: payload,
        success: (res) => {
          if (!chunkSupported) {
            const body = typeof res.data === 'string' ? res.data : JSON.stringify(res.data);
            parseChunk(body, emit, true);
          } else {
            const tail = decoder ? decoder.decode() : '';
            if (tail) buffer += tail;
            buffer = parseChunk(buffer, emit, true);
          }
          resolve();
        },
        fail: reject,
      });
      if (task?.onChunkReceived) {
        chunkSupported = true;
        task.onChunkReceived((res) => {
          const bytes = res?.data;
          let text = '';
          text = decodeChunk(bytes, decoder);
          buffer += text || '';
          buffer = parseChunk(buffer, emit);
        });
      }
      if (signal) signal.addEventListener?.('abort', () => task?.abort?.(), { once: true });
    });
  } catch (error) {
    if (signal?.aborted) return;
    try {
      const result = await AiChatApi.send(payload);
      if (result?.code === 0) {
        emit({ ...result.data, eventType: 'MESSAGE_COMPLETE' });
        return;
      }
      throw new Error(result?.msg || 'AI 暂时无法响应');
    } catch (fallbackError) { onError?.(fallbackError); }
  }
}
