import packageInfo from '@/package.json';

const { version } = packageInfo;

// 开发环境配置
export let baseUrl;
if (process.env.NODE_ENV === 'development') {
  baseUrl = import.meta.env.SHOPRO_DEV_BASE_URL;
} else {
  baseUrl = import.meta.env.SHOPRO_BASE_URL;
}
if (typeof baseUrl === 'undefined') {
  console.error('请检查.env配置文件是否存在');
} else {
  console.log(`[AgenticCPS商城 ${version}]  https://doc.iocoder.cn`);
}

export const apiPath = import.meta.env.SHOPRO_API_PATH;
export const staticUrl = import.meta.env.SHOPRO_STATIC_URL;
export const tenantId = import.meta.env.SHOPRO_TENANT_ID;
export const websocketPath = import.meta.env.SHOPRO_WEBSOCKET_PATH;
// 语音识别服务地址。可在 .env* 中覆盖，默认使用项目自建 SenseVoice 服务。
export const asrUrl =
  process.env.NODE_ENV === 'development' && typeof window !== 'undefined'
    ? '/asr-proxy'
    : import.meta.env.SHOPRO_ASR_URL ||
      'http://47.109.140.45:8080/SenseVoice/v1/audio/transcriptions';
// 开发环境固定使用 localhost:3000；生产 H5 与管理端共用域名，通过 /h5/ 子路径访问。
export const h5Url =
  process.env.NODE_ENV === 'development'
    ? import.meta.env.SHOPRO_H5_URL
    : typeof window !== 'undefined'
    ? `${window.location.origin}/h5/`
    : '/h5/';

export default {
  baseUrl,
  apiPath,
  staticUrl,
  tenantId,
  websocketPath,
  asrUrl,
  h5Url,
};
