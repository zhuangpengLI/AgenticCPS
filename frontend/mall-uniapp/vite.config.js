import { loadEnv } from 'vite';
import uni from '@dcloudio/vite-plugin-uni';
import path from 'path';
// import viteCompression from 'vite-plugin-compression';
import uniReadPagesV3Plugin from './sheep/router/utils/uni-read-pages-v3';
import mpliveMainfestPlugin from './sheep/libs/mplive-manifest-plugin';

// https://vitejs.dev/config/
export default (command, mode) => {
  const env = loadEnv(mode, __dirname, 'SHOPRO_');
  return {
    envPrefix: 'SHOPRO_',
    plugins: [
      uni(),
      // viteCompression({
      // 	verbose: false
      // }),
      uniReadPagesV3Plugin({
        pagesJsonDir: path.resolve(__dirname, './pages.json'),
        includes: ['path', 'aliasPath', 'name', 'meta'],
      }),
      mpliveMainfestPlugin(env.SHOPRO_MPLIVE_ON),
    ],
    server: {
      host: true,
      // open: true,
      port: env.SHOPRO_DEV_PORT,
      // H5 开发环境通过 Vite 代理转发 ASR，浏览器只请求 localhost，避免跨域。
      proxy: {
        '/asr-proxy': {
          target: env.SHOPRO_ASR_TARGET || 'http://47.109.140.45:8080',
          changeOrigin: true,
          rewrite: (requestPath) =>
            requestPath.replace(/^\/asr-proxy/, '/SenseVoice/v1/audio/transcriptions'),
        },
      },
      headers: {
        'Cache-Control': 'no-store',
      },
      hmr: {
        overlay: true,
      },
    },
  };
};
