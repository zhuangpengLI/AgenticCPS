// 登录
import AuthUtil from '@/sheep/api/member/auth';
import SocialApi from '@/sheep/api/member/social';
import sheep from '@/sheep';

const socialType = 32; // 社交类型 - 微信开放平台

const load = async () => {};

// 微信开放平台移动应用授权登陆
const login = async () => {
  try {
    const loginRes = await uni.login({
      provider: 'weixin',
      onlyAuthorize: true,
    });
    if (loginRes.errMsg !== 'login:ok' || !loginRes.code) {
      showLoginError(loginRes.errMsg);
      return false;
    }

    const loginResult = await AuthUtil.socialLogin(socialType, loginRes.code, 'default');
    if (loginResult.code !== 0) {
      showLoginError();
      return false;
    }

    await sheep.$store('user').establishSession(loginResult.data);
    setOpenid(loginResult.data.openid);
    return true;
  } catch (error) {
    showLoginError(error?.errMsg);
    return false;
  }
};

function showLoginError(detail) {
  uni.showToast({
    icon: 'none',
    title: detail ? `微信登录失败：${detail}` : '微信登录失败，请稍后重试',
  });
}

// 微信 App 解除绑定
const unbind = async (openid) => {
  const { code } = await SocialApi.socialUnbind(socialType, openid);
  return code === 0;
};

// 设置 openid 到本地存储，目前只有 pay 支付时会使用
function setOpenid(openid) {
  if (openid) {
    uni.setStorageSync('openid', openid);
  }
}

// 获得 openid
async function getOpenid(force = false) {
  let openid = uni.getStorageSync('openid');
  if (!openid && force) {
    const info = await getInfo();
    if (info?.openid) {
      openid = info.openid;
      setOpenid(openid);
    }
  }
  return openid;
}

// 获得社交信息
async function getInfo() {
  const { code, data } = await SocialApi.getSocialUser(socialType);
  if (code !== 0) {
    return undefined;
  }
  return data;
}

export default {
  load,
  login,
  unbind,
  getInfo,
  getOpenid,
};
