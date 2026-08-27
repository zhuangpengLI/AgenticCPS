import $store from '@/sheep/store';
import $helper from '@/sheep/helper';
import dayjs from 'dayjs';
import { ref } from 'vue';
import test from '@/sheep/helper/test.js';
import AuthUtil from '@/sheep/api/member/auth';

const LOGIN_AUTH_TYPES = new Set(['accountLogin', 'smsLogin', 'resetPassword']);

// 打开授权弹框
export function showAuthModal(type = 'smsLogin') {
  const modal = $store('modal');
  const currentAuthType = modal.auth;
  const isDefaultLoginRequest = arguments.length === 0;
  // 路由守卫与并发 API 可能同时请求登录；已有登录流程时不要重开弹窗或重置表单
  if (
    currentAuthType === type ||
    (isDefaultLoginRequest && LOGIN_AUTH_TYPES.has(currentAuthType))
  ) {
    return;
  }
  // 登录方式属于同一个弹窗，直接替换内容。H5 若先关闭再延迟打开，
  // su-popup 的 close 回调会与延迟任务竞态，导致“账号登录”等切换后弹窗消失。
  modal.$patch((state) => {
    state.auth = type;
  });
}

// 关闭授权弹框
export function closeAuthModal() {
  $store('modal').$patch((state) => {
    state.auth = '';
  });
}

// 打开分享弹框
export function showShareModal() {
  $store('modal').$patch((state) => {
    state.share = true;
  });
}

// 关闭分享弹框
export function closeShareModal() {
  $store('modal').$patch((state) => {
    state.share = false;
  });
}

// 打开快捷菜单
export function showMenuTools() {
  $store('modal').$patch((state) => {
    state.menu = true;
  });
}

// 关闭快捷菜单
export function closeMenuTools() {
  $store('modal').$patch((state) => {
    state.menu = false;
  });
}

// 发送短信验证码  60秒
export function getSmsCode(event, mobile) {
  const modalStore = $store('modal');
  const lastSendTimer = modalStore.lastTimer[event];
  if (typeof lastSendTimer === 'undefined') {
    $helper.toast('短信发送事件错误');
    return;
  }

  const duration = dayjs().unix() - lastSendTimer;
  const canSend = duration >= 60;
  if (!canSend) {
    $helper.toast('请稍后再试');
    return;
  }
  // 只有 mobile 非空时才校验。因为部分场景（修改密码），不需要输入手机
  if (mobile && !test.mobile(mobile)) {
    $helper.toast('手机号码格式不正确');
    return;
  }

  // 发送验证码 + 更新上次发送验证码时间
  let scene = -1;
  switch (event) {
    case 'resetPassword':
      scene = 4;
      break;
    case 'changePassword':
      scene = 3;
      break;
    case 'changeMobile':
      scene = 2;
      break;
    case 'smsLogin':
      scene = 1;
      break;
  }
  AuthUtil.sendSmsCode(mobile, scene).then((res) => {
    if (res.code === 0) {
      modalStore.$patch((state) => {
        state.lastTimer[event] = dayjs().unix();
      });
    }
  });
}

// 获取短信验证码倒计时 -- 60秒
export function getSmsTimer(event, mobile = '') {
  const modalStore = $store('modal');
  const lastSendTimer = modalStore.lastTimer[event];

  if (typeof lastSendTimer === 'undefined') {
    $helper.toast('短信发送事件错误');
    return;
  }

  const duration = ref(dayjs().unix() - lastSendTimer - 60);
  const canSend = duration.value >= 0;

  if (canSend) {
    return '获取验证码';
  }

  if (!canSend) {
    setTimeout(() => {
      duration.value++;
    }, 1000);
    return -duration.value.toString() + ' 秒';
  }
}

// 记录广告弹框历史
export function saveAdvHistory(adv) {
  const modal = $store('modal');

  modal.$patch((state) => {
    if (!state.advHistory.includes(adv.imgUrl)) {
      state.advHistory.push(adv.imgUrl);
    }
  });
}
