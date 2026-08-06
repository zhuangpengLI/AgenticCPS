const PLATFORM_NAMES = {
  taobao: '淘宝',
  jd: '京东',
  pdd: '拼多多',
  douyin: '抖音',
  meituan: '美团',
  eleme: '饿了么',
  didi: '滴滴',
  vip: '唯品会',
};

export function toNumber(value) {
  const number = Number(value || 0);
  return Number.isFinite(number) ? number : 0;
}

export function formatMoney(value) {
  return toNumber(value).toFixed(2);
}

export function formatCentMoney(value) {
  return (toNumber(value) / 100).toFixed(2);
}

export function platformText(platformCode) {
  return PLATFORM_NAMES[platformCode] || platformCode || '未知平台';
}

/**
 * @typedef {Object} CpsPromotionAction
 * @property {string} platformCode
 * @property {'tpwd'|'url'} type
 * @property {string} value
 * @property {string} fallbackValue
 * @property {string} displayText
 */

export function createPromotionAction(link = {}, platformCode = '') {
  const code = platformCode || link.platformCode || '';
  const preferredUrl = link.mobileUrl || link.shortUrl || link.longUrl || '';
  if (code === 'taobao' && link.tpwd) {
    return {
      platformCode: code,
      type: 'tpwd',
      value: link.tpwd,
      fallbackValue: preferredUrl,
      displayText: '口令已复制，去淘宝打开',
    };
  }
  const fallbackValue = link.shortUrl || link.longUrl || '';
  return {
    platformCode: code,
    type: 'url',
    value: preferredUrl || fallbackValue,
    fallbackValue,
    displayText: `已准备${platformText(code)}购买链接`,
  };
}

export function copyPromotionValue(value) {
  if (!value) {
    return Promise.reject(new Error('EMPTY_PROMOTION_VALUE'));
  }
  return new Promise((resolve, reject) => {
    uni.setClipboardData({ data: value, success: resolve, fail: reject });
  });
}

export async function executePromotionAction(action) {
  if (!action?.value) {
    throw new Error('EMPTY_PROMOTION_VALUE');
  }
  if (action.type === 'tpwd') {
    await copyPromotionValue(action.value);
    return { copied: true, opened: false };
  }

  // #ifdef H5
  const opened = Boolean(window.open(action.value, '_blank'));
  if (opened) {
    return { copied: false, opened: true };
  }
  // #endif

  await copyPromotionValue(action.value || action.fallbackValue);
  return { copied: true, opened: false };
}

export function createCpsIdempotencyKey(prefix) {
  const random = Math.random().toString(36).slice(2, 10);
  return `${prefix}:${Date.now()}:${random}`;
}
