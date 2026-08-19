const MALL_STATIC_PATH_PATTERN =
  /^(?:https?:\/\/cdn\.fastbee\.cn\/cps\/(?:qiji|yudao)-static\/mall\/|\/?)(static\/.*)$/i
const LEGACY_PREVIEW_HOSTS = new Set(['test.qiji.iocoder.cn', 'file.sheepjs.com'])
const LEGACY_DIY_ASSET_URL_PATTERN =
  /^https?:\/\/test\.qiji\.iocoder\.cn\/([0-9a-f]{64})\.[a-z0-9]+(?:[?#].*)?$/i

const LEGACY_DIY_ASSET_PATHS: Record<string, string> = {
  '039699725191165d0f1502b13d4ed2f877bd83434294bf818862c2215bf25f95':
    'static/img/diy_quanpinlei/dibudaohang/index-0.png',
  '13b226445efa6911d44067432fd3f91c94c44dea85cd0a1ead27fda938cbdd60':
    'static/img/diy_quanpinlei/caidandaohang/category-02.png',
  '142fa67043f954f2f628dd8e3af4fbd42a5bed48fb7a76735f710a1635d9fa07':
    'static/img/diy_quanpinlei/caidandaohang/category-05.png',
  '1cbc95a49a939997d37f9595a96470a03f5c33930ce310125267ddea43297072':
    'static/img/diy_quanpinlei/biaoti/weinituijian.png',
  '2205efb3a00230b12c960b4f2c1d4ffa8447bb4818f2b33327077be34c729a52':
    'static/img/diy_quanpinlei/dibudaohang/cart-0.png',
  '24ac96ede822e8db7e286c9ec52b932d023c5219fee679bc885a103ed0647753':
    'static/img/diy_quanpinlei/caidandaohang/category-07.png',
  '2b6cee793a7e58073fc4e5e82bfa60c5bfb00376a3f3cb3a688ddfb6e0f79199':
    'static/img/diy_quanpinlei/dibudaohang/user-1.gif',
  '2f9fbc5f514d60f636f4ae778d89f525889971c33206448dc0ba1dd77ec30319':
    'static/img/diy_quanpinlei/dibudaohang/category-0.png',
  '2fe096a1587bde458bd016dd7c9400fc831a5d01f418c3a62c234a614c63b804':
    'static/img/diy_quanpinlei/caidandaohang/category-03.png',
  '44ce6219308a42674cdd1928b7f5a4ead7c87f8e13b2c71403bbc95925e884b4':
    'static/img/diy_quanpinlei/lunbo/banner01.png',
  '5d26b27bff090603aa2f1f9ff037daf8fe2bf3eaab99f0c317c7d6155b5ade58':
    'static/img/diy_quanpinlei/biaoti/youhuijuan-02.png',
  '5e84271fb87c3213d5746c03ea8eba3f92d8dd9b23088639142725249e3b7799':
    'static/img/diy_quanpinlei/biaoti/jinribaopin.png',
  '6042b470deac3739e83a04213866f9c59f0a06501709e3d8b828d14b5828241d':
    'static/img/diy_quanpinlei/caidandaohang/category-01.png',
  '70a63a29e8c1150afdf0646e1c82b2d52611dc4e8724b1cf1b83484280e10142':
    'static/img/diy_quanpinlei/caidandaohang/category-06.png',
  '7a1af06389a08522c37566ba3e8cbda2efe1a506781d1b126e80ddb4d2533241':
    'static/img/diy_quanpinlei/biaoti/youhuijuan-03.png',
  '813a60dc6a84bd2c497d44ebcfa642787c0f18101585aa77172dde015949e873':
    'static/img/diy_quanpinlei/caidandaohang/sign.png',
  '82eb49a8bc77a958afa0c275ef99a218abb3ba63877aae73cf48488ef207c0f3':
    'static/img/diy_quanpinlei/guanggaomofang/right-01.png',
  '92b9f85e1b1059872898e74ace7157064e0556c04012fc93e722ec4dd53f8745':
    'static/img/diy_quanpinlei/dibudaohang/index-1.gif',
  '943a5fa1d85cc69b9ffe4068a9db9e62d1d9a297981efe42e625b48dbeba9425':
    'static/img/diy_quanpinlei/guanggaomofang/right-02.png',
  '94e2acead757b397366ca41e34128820a752d0410aaa3494fd8710f06b2f647e':
    'static/img/diy_quanpinlei/caidandaohang/seckill.png',
  '9d7864ac4be0d205c8d1f051fd0c53d9a75f81e5f0b2ebf9e463b0bdc4e16d84':
    'static/img/diy_quanpinlei/caidandaohang/category-04.png',
  a02819e7346f6d2dbe1d9fe12fdde3ab967cc6e84e4265c2376ce0299b29f598:
    'static/img/diy_quanpinlei/guanggaomofang/left.png',
  a4ad5a5c03ed5db31b6ba9e6e43e2dedcc9c9c883ead1ad58490669057888dab:
    'static/img/diy_quanpinlei/biaoti/youhuijuan.png',
  be1fa2f4ff6e0d77f2dd2b37e227ee22baab7df626c1ab317fc3d6d242b091eb:
    'static/img/diy_quanpinlei/shangpinkapian/cart.png',
  c3135f6de0a65b2705584c26521fb3ed130ae16af95e03dc23dfac6671b7e70f:
    'static/img/diy_quanpinlei/dingbudaohang/juan.png',
  dc71069fada14b85e17b054255a380b25d15a280b1eae28b59c4216b85c07da8:
    'static/img/diy_quanpinlei/caidandaohang/groupon.png',
  e36541fd2a07ad89940223640d95fbc9e8a67d73c3871e8258c03dc784fcd7b8:
    'static/img/diy_quanpinlei/dibudaohang/cart-1.gif',
  fc473f54aaa6e46b0c2e7f6fb0dcbf3a16ee14256b293b95ffa75e7829f82753:
    'static/img/diy_quanpinlei/dibudaohang/user-0.png',
  ff3addf5365ce58945b14a1219f2d490daae2145d6bf6e5c56ecfcf7e856a442:
    'static/img/diy_quanpinlei/dibudaohang/category-1.gif'
}

const trimTrailingSlash = (value: string) => value.replace(/\/+$/, '')

const getLegacyDiyStaticPath = (url: string) => {
  const assetHash = url.match(LEGACY_DIY_ASSET_URL_PATTERN)?.[1]?.toLowerCase()
  return assetHash ? LEGACY_DIY_ASSET_PATHS[assetHash] || '' : ''
}

const getMallStaticPath = (url: string) => {
  const legacyStaticPath = getLegacyDiyStaticPath(url)
  if (legacyStaticPath) return legacyStaticPath

  const match = url.match(MALL_STATIC_PATH_PATTERN)
  return match?.[1] || ''
}

/** Resolve bundled mall assets from the admin origin or a configured CDN base URL. */
export const resolveMallStaticUrl = (url = '') => {
  if (!url) return ''

  const mallStaticPath = getMallStaticPath(url)
  if (!mallStaticPath) return url

  const staticPath = `/${mallStaticPath.replace(/^\/+/, '')}`
  const staticBaseUrl = import.meta.env.VITE_MALL_STATIC_URL?.trim()
  if (!staticBaseUrl || staticBaseUrl === 'local') {
    return staticPath
  }
  return `${trimTrailingSlash(staticBaseUrl)}${staticPath}`
}

/** Resolve all known mall asset URLs in a DIY property tree without touching business URLs. */
export const resolveDiyAssetUrls = <T>(value: T): T => {
  if (typeof value === 'string') {
    return resolveMallStaticUrl(value) as T
  }
  if (Array.isArray(value)) {
    return value.map((item) => resolveDiyAssetUrls(item)) as T
  }
  if (value && typeof value === 'object') {
    return Object.fromEntries(
      Object.entries(value).map(([key, item]) => [key, resolveDiyAssetUrls(item)])
    ) as T
  }
  return value
}

const isLegacyPreviewUrl = (url: string) => {
  try {
    return LEGACY_PREVIEW_HOSTS.has(new URL(url).hostname)
  } catch {
    return false
  }
}

const getLocalTemplatePreview = (name = '') => {
  if (/AgenticCPS.*返利|返利专用/.test(name)) return '/static/img/cps-rebate-template-preview.svg'
  if (/端午/.test(name)) return '/static/img/diy_duanwujie/lunbo/banner01.png'
  if (/全品类/.test(name)) return '/static/img/diy_quanpinlei/lunbo/banner01.png'
  if (/3C|数码/i.test(name)) return '/static/img/diy_3c/lunbo/banner01.png'
  if (/演示|默认|测试/.test(name)) return '/static/img/diy/banner-01.png'
  return ''
}

/** Replace known expired demo preview URLs while leaving uploaded business images untouched. */
export const resolveDiyPreviewUrls = (
  name: string,
  urls: string[] | null | undefined = []
) => {
  const normalizedUrls = Array.isArray(urls) ? urls : []
  const availableUrls = normalizedUrls.filter(Boolean).filter((url) => !isLegacyPreviewUrl(url))
  if (availableUrls.length > 0) {
    return availableUrls.map(resolveMallStaticUrl)
  }

  const localPreview = getLocalTemplatePreview(name)
  return localPreview ? [resolveMallStaticUrl(localPreview)] : []
}
