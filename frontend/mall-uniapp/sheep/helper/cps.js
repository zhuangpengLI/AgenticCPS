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

export function promotionUrl(link = {}) {
  return link.mobileUrl || link.shortUrl || link.longUrl || link.promotionUrl || '';
}

/**
 * Open a platform promotion URL in the current runtime. APP-PLUS delegates
 * to the operating system so a platform deep link can launch its native app;
 * H5 opens a new tab. Callers can fall back to copying the URL when the
 * runtime or platform cannot handle it.
 */
export function openPromotionUrl(url) {
  if (!url) return Promise.reject(new Error('EMPTY_PROMOTION_VALUE'));
  // #ifdef H5
  if (typeof window !== 'undefined') {
    const opened = Boolean(window.open(url, '_blank'));
    if (opened) return Promise.resolve({ opened: true });
    // A delayed API response may be outside the popup-allowlist window. A
    // same-tab navigation still provides the requested purchase jump.
    window.location.assign(url);
    return Promise.resolve({ opened: true });
  }
  // #endif
  // #ifdef APP-PLUS
  if (typeof plus !== 'undefined' && plus.runtime?.openURL) {
    return new Promise((resolve) => {
      plus.runtime.openURL(url, () => resolve({ opened: true }), () => resolve({ opened: false }));
    });
  }
  // #endif
  return Promise.resolve({ opened: false });
}

export async function executePromotionAction(action) {
  if (!action?.value) {
    throw new Error('EMPTY_PROMOTION_VALUE');
  }
  if (action.type === 'tpwd') {
    await copyPromotionValue(action.value);
    return { copied: true, opened: false };
  }
  const opened = await openPromotionUrl(action.value);
  if (opened.opened) return { copied: false, opened: true };
  await copyPromotionValue(action.value || action.fallbackValue);
  return { copied: true, opened: false };
}

export function createCpsIdempotencyKey(prefix) {
  const random = Math.random().toString(36).slice(2, 10);
  return `${prefix}:${Date.now()}:${random}`;
}
