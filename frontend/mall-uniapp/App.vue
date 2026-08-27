<script setup>
  import { onLaunch, onShow, onError } from '@dcloudio/uni-app';
  import sheep, { ShoproInit } from './sheep';

  let initPromise = Promise.resolve();

  onLaunch((options = {}) => {
    // 隐藏原生导航栏 使用自定义底部导航
    uni.hideTabBar({
      fail: () => {},
    });

    // 尽早安装原生导航守卫，再恢复租户、登录会话与应用配置
    sheep.$router.installAuthGuards();
    initPromise = ShoproInit()
      .catch((error) => {
        console.error('应用初始化失败:', error);
      })
      .then(() => sheep.$router.guardEntry(options));
  });

  onShow((options = {}) => {
    initPromise.then(() => sheep.$router.guardEntry(options));
    // #ifdef APP-PLUS
    // 获取urlSchemes参数
    const args = plus.runtime.arguments;
    if (args) {
    }

    // 获取剪贴板
    uni.getClipboardData({
      success: (res) => {},
    });
    // #endif
  });
</script>

<style lang="scss">
  @import '@/sheep/scss/index.scss';
</style>
