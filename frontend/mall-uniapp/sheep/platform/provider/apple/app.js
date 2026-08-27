// 后端当前没有 Apple 社交类型及对应登录接口。显式返回失败，避免调用不存在的 API
// 或向上层伪报登录成功；待后端完成 Apple 身份令牌校验后再接入系统登录。
const login = async () => {
  uni.showToast({
    icon: 'none',
    title: '暂不支持 Apple 登录，请使用手机号登录',
  });
  return false;
};

export default {
  login,
};
