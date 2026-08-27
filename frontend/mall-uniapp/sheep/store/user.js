import { defineStore } from 'pinia';
import $share from '@/sheep/platform/share';
import { clone, cloneDeep } from 'lodash-es';
import cart from './cart';
import app from './app';
import { showAuthModal } from '@/sheep/hooks/useModal';
import UserApi from '@/sheep/api/member/user';
import PayWalletApi from '@/sheep/api/pay/wallet';
import OrderApi from '@/sheep/api/trade/order';
import CouponApi from '@/sheep/api/promotion/coupon';

// 默认用户信息
const defaultUserInfo = {
  avatar: '', // 头像
  nickname: '', // 昵称
  gender: 0, // 性别
  mobile: '', // 手机号
  point: 0, // 积分
};

// 默认钱包信息
const defaultUserWallet = {
  balance: 0, // 余额
};

// 默认订单、优惠券等其他资产信息
const defaultNumData = {
  unusedCouponCount: 0,
  orderCount: {
    allCount: 0,
    unpaidCount: 0,
    undeliveredCount: 0,
    deliveredCount: 0,
    uncommentedCount: 0,
    afterSaleCount: 0,
  },
};

const user = defineStore({
  id: 'user',
  state: () => ({
    userInfo: clone(defaultUserInfo), // 用户信息
    userWallet: clone(defaultUserWallet), // 用户钱包信息
    authRevision: 0, // 登录令牌变更版本，用于让 token 派生 getter 保持响应式
    numData: cloneDeep(defaultNumData), // 用户其他数据
    lastUpdateTime: 0, // 上次更新时间
  }),

  getters: {
    // token 是登录状态的唯一事实源；authRevision 仅用于令牌变更后使 getter 失效重算
    isLogin: (state) => {
      void state.authRevision;
      return Boolean(uni.getStorageSync('token'));
    },
  },

  actions: {
    // 清理旧版本持久化的整份用户资料；新版本只持久化 token
    clearLegacyUserCache() {
      uni.removeStorageSync('user-store');
      // #ifdef H5
      window.sessionStorage?.removeItem('user-store');
      // #endif
    },

    // 获取用户信息
    async getInfo() {
      const { code, data } = await UserApi.getUserInfo();
      if (code !== 0) {
        return;
      }
      this.userInfo = data;
      return Promise.resolve(data);
    },

    // 获得用户钱包
    async getWallet() {
      const { code, data } = await PayWalletApi.getPayWallet();
      if (code !== 0) {
        return;
      }
      this.userWallet = data;
    },

    // 获取订单、优惠券等其他资产信息
    getNumData() {
      OrderApi.getOrderCount().then((res) => {
        if (res.code === 0) {
          this.numData.orderCount = res.data;
        }
      });
      CouponApi.getUnusedCouponCount().then((res) => {
        if (res.code === 0) {
          this.numData.unusedCouponCount = res.data;
        }
      });
    },

    // 设置 token：这里只负责持久化，不触发登录后请求。
    // 登录接口必须通过 establishSession 完成会员身份校验后，才对外宣布登录成功。
    setToken(token = '', refreshToken = '') {
      if (token === '') {
        uni.removeStorageSync('token');
        uni.removeStorageSync('refresh-token');
      } else {
        uni.setStorageSync('token', token);
        uni.setStorageSync('refresh-token', refreshToken);
      }
      this.authRevision += 1;
      if (token === '') uni.$emit('auth:logout');
      return this.isLogin;
    },

    // 建立可用的会员会话：保存令牌后必须验证 /member/user/get，验证成功才关闭登录流程。
    async establishSession(tokenData = {}) {
      const accessToken =
        typeof tokenData.accessToken === 'string' ? tokenData.accessToken.trim() : '';
      if (!accessToken) {
        throw new Error('登录响应缺少访问令牌');
      }

      this.setToken(accessToken, tokenData.refreshToken || '');
      this.lastUpdateTime = 0;
      try {
        const userInfo = await this.getInfo();
        if (!userInfo) throw new Error('会员登录状态校验失败');
        await this.loginAfter({ refreshUser: false });
        uni.$emit('auth:login');
        return userInfo;
      } catch (error) {
        this.resetUserData();
        throw error;
      }
    },

    // 更新用户相关信息 (手动限流，5 秒之内不刷新)
    async updateUserData() {
      if (!this.isLogin) {
        this.resetUserData();
        return;
      }
      // 防抖，5 秒之内不刷新
      const nowTime = new Date().getTime();
      if (this.lastUpdateTime + 5000 > nowTime) {
        return;
      }
      this.lastUpdateTime = nowTime;

      // 获取最新信息
      await this.getInfo();
      this.getWallet();
      this.getNumData();
      return this.userInfo;
    },

    // 重置用户默认数据
    resetUserData() {
      // 清空 token
      this.setToken();
      this.clearLegacyUserCache();
      // 清空用户相关的缓存
      this.userInfo = clone(defaultUserInfo);
      this.userWallet = clone(defaultUserWallet);
      this.numData = cloneDeep(defaultNumData);
      this.lastUpdateTime = 0;
      // 清空购物车的缓存
      cart().emptyList();
    },

    // 登录后，加载各种信息
    async loginAfter({ refreshUser = true } = {}) {
      if (refreshUser) await this.updateUserData();

      // 加载购物车
      cart().getList();
      // 登录后设置全局分享参数
      $share.getShareInfo();

      // 提醒绑定手机号
      if (app().platform.bind_mobile && !this.userInfo.mobile) {
        showAuthModal('changeMobile');
      }

      // 绑定推广员
      $share.bindBrokerageUser();
    },

    // 登出系统
    async logout() {
      this.resetUserData();
      return !this.isLogin;
    },
  },
});

export default user;
