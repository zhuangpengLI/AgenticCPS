import DiyApi from '@/sheep/api/promotion/diy';
import { getTenantByWebsite } from '@/sheep/api/infra/tenant';
import {
  failAuthContextReady,
  getTenantId,
  markAuthContextReady,
  resetAuthContextReady,
} from '@/sheep/request';
import { defineStore } from 'pinia';
import $platform from '@/sheep/platform';
import $router from '@/sheep/router';
import user from './user';
import sys from './sys';
import { baseUrl, h5Url, tenantId as configuredTenantId } from '@/sheep/config';

const app = defineStore({
  id: 'app',
  state: () => ({
    paramsForTabbar: {}, // 为全局tabbar跳转传参用。原因是 tabbar 无法传参，只能通过全局状态传递
    info: {
      // 应用信息
      name: '', // 商城名称
      logo: '', // logo
      version: '', // 版本号
      copyright: '', // 版权信息 I
      copytime: '', // 版权信息 II

      cdnurl: '', // 云存储域名
      filesystem: '', // 云存储平台
    },
    platform: {
      share: {
        methods: [], // 支持的分享方式
        forwardInfo: {}, // 默认转发信息
        posterInfo: {}, // 海报信息
        linkAddress: '', // 复制链接地址
      },
      bind_mobile: 0, // 登陆后绑定手机号提醒 (弱提醒，可手动关闭)
    },
    template: {
      // 店铺装修模板
      basic: {}, // 基本信息
      home: {
        // 首页模板
        style: {},
        data: [],
      },
      user: {
        // 个人中心模板
        style: {},
        data: [],
      },
    },
    shareInfo: {}, // 全局分享信息
    has_wechat_trade_managed: 0, // 小程序发货信息管理  0 没有 || 1 有
  }),
  actions: {
    // 获取Shopro应用配置和模板
    async init(templateId = null) {
      resetAuthContextReady();
      try {
        // 检查网络
        const networkStatus = await $platform.checkNetwork();
        if (!networkStatus) {
          $router.error('NetworkError');
        }

        // 检查配置
        if (typeof baseUrl === 'undefined') {
          $router.error('EnvError');
        }

        // 加载租户
        const tenantReady = await adaptTenant();
        if (!tenantReady) throw new Error('租户上下文初始化失败');
      } catch (error) {
        user().resetUserData();
        failAuthContextReady(error);
        throw error;
      }
      markAuthContextReady();

      // 租户是身份安全边界：装修页面渲染前先清理旧缓存并验证持久化 token
      const userStore = user();
      userStore.clearLegacyUserCache();
      if (userStore.isLogin) {
        try {
          await userStore.getInfo();
        } catch (error) {
          // 401 会由请求层统一清理会话；网络失败不阻断公开页面使用
          if (error?.code !== 401) console.error('用户会话恢复失败:', error);
        }
      }

      // 加载装修配置
      await adaptTemplate(this.template, templateId);

      // TODO 芋艿：【初始化优化】未来支持管理后台可配；对应 https://api.shopro.sheepjs.com/shop/api/init
      if (true) {
        this.info = {
          name: 'AgenticCPS商城',
          logo: 'https://static.iocoder.cn/ruoyi-vue-pro-logo.png',
          version: '2026.01',
          copyright: '全部开源，个人与企业可 100% 免费使用',
          copytime: 'Copyright© 2018-2025',

          cdnurl: 'https://file.sheepjs.com', // 云存储域名
          filesystem: 'qcloud', // 云存储平台
        };
        this.platform = {
          share: {
            methods: ['forward', 'poster', 'link'],
            linkAddress: h5Url,
            posterInfo: {
              user_bg: '/static/img/shop/config/user-poster-bg.png',
              goods_bg: '/static/img/shop/config/goods-poster-bg.png',
              groupon_bg: '/static/img/shop/config/groupon-poster-bg.png',
            },
            forwardInfo: {
              title: '',
              image: '',
              desc: '',
            },
          },
          bind_mobile: 0,
        };
        this.has_wechat_trade_managed = 0;

        // 加载主题
        const sysStore = sys();
        sysStore.setTheme();

        // 身份已在租户适配后校验，这里只恢复购物车、分享等登录后副作用
        if (userStore.isLogin) {
          await userStore.loginAfter({ refreshUser: false });
        }
        return Promise.resolve(true);
      } else {
        $router.error('InitError', res.msg || '加载失败');
      }
    },
    // 设置 paramsForTabbar
    setParamsForTabbar(params = {}) {
      this.paramsForTabbar = params;
    },
    clearParamsForTabbar() {
      this.paramsForTabbar = {};
    },
  },
  persist: {
    enabled: true,
    strategies: [
      {
        key: 'app-store',
      },
    ],
  },
});

function requireTenantId(value) {
  const tenantId = Number(value);
  if (!Number.isSafeInteger(tenantId) || tenantId <= 0) {
    throw new Error('租户编号无效');
  }
  return tenantId;
}

/** 初始化租户编号 */
const adaptTenant = async () => {
  // 1. 获取当前租户 ID
  const oldTenantId = getTenantId();
  let newTenantId = null;

  try {
    // 2.1 情况一：H5：根据 url 参数、域名来获取新的租户ID
    // #ifdef H5
    // H5 环境下的处理逻辑
    if (window?.location) {
      // 优先从 URL 查询参数获取 tenantId
      const urlParams = new URLSearchParams(window.location.search);
      newTenantId = urlParams.get('tenantId');
      if (newTenantId) newTenantId = requireTenantId(newTenantId);

      // 如果 URL 参数中没有，则通过 host 获取
      if (!newTenantId && window.location.host) {
        const tenantResult = await getTenantByWebsite(window.location.host);
        if (!tenantResult || tenantResult.code !== 0) throw new Error('租户解析失败');
        if (tenantResult.data?.id) {
          newTenantId = requireTenantId(tenantResult.data.id);
        } else if (
          process.env.NODE_ENV === 'development' &&
          /^(localhost|127\.0\.0\.1)(:\d+)?$/.test(window.location.host)
        ) {
          newTenantId = requireTenantId(configuredTenantId);
        } else {
          throw new Error('租户解析结果为空');
        }
      }
    }
    // #endif

    // 2.2 情况二：微信小程序：小程序环境下的处理逻辑 - 根据 appId 获取租户
    // #ifdef MP
    const appId = uni.getAccountInfoSync()?.miniProgram?.appId;
    if (!appId) throw new Error('无法获取小程序 AppId');
    const tenantResult = await getTenantByWebsite(appId);
    if (!tenantResult || tenantResult.code !== 0) throw new Error('租户解析失败');
    newTenantId = requireTenantId(tenantResult.data?.id);
    // #endif

    // 3. 如果是新租户（不相等），则进行切换
    // noinspection EqualityComparisonWithCoercionJS
    if (newTenantId && newTenantId != oldTenantId) {
      // 租户身份发生变化时，必须清理令牌及所有会员缓存，避免跨租户展示旧身份
      const userStore = user();
      userStore.resetUserData();

      // 设置新的 tenantId 到本地存储
      uni.setStorageSync('tenant-id', newTenantId);
      console.log('租户 ID 已更新:', `${oldTenantId} -> ${newTenantId}`);
    }
    return true;
  } catch (error) {
    console.error('adaptTenant 执行失败:', error);
    return false;
  }
};

/** 初始化装修模版 */
const adaptTemplate = async (appTemplate, templateId) => {
  const { data: diyTemplate } = templateId
    ? // 查询指定模板，一般是预览时使用
      await DiyApi.getDiyTemplate(templateId)
    : await DiyApi.getUsedDiyTemplate();
  // 模板不存在
  if (!diyTemplate) {
    $router.error('TemplateError');
    return;
  }

  const tabBar = diyTemplate?.property?.tabBar;
  if (tabBar) {
    appTemplate.basic.tabbar = tabBar;
    // TODO 商城装修没有对 tabBar 进行角标配置，测试角标需打开以下注释
    // appTemplate.basic.tabbar.items.forEach((tabBar) => {
    //   tabBar.dot = false
    //   tabBar.badge = 100
    // })
    // appTemplate.basic.tabbar.badgeStyle = {
    //   backgroundColor: '#882222',
    // }
    if (tabBar?.theme) {
      appTemplate.basic.theme = tabBar?.theme;
    }
  }
  appTemplate.home = diyTemplate?.home;
  appTemplate.user = diyTemplate?.user;
};

export default app;
