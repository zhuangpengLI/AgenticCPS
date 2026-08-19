import $store from '@/sheep/store';
import { staticUrl } from '@/sheep/config';

const MALL_STATIC_PATH_PATTERN =
  /^(?:https?:\/\/cdn\.fastbee\.cn\/cps\/(?:qiji|yudao)-static\/mall\/|\/?)(static\/.*)$/i;
const LEGACY_DIY_ASSET_URL_PATTERN =
  /^https?:\/\/test\.qiji\.iocoder\.cn\/([0-9a-f]{64})\.[a-z0-9]+(?:[?#].*)?$/i;
const LEGACY_DIY_ASSET_PATHS = {
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
    'static/img/diy_quanpinlei/dibudaohang/category-1.gif',
};

const getLegacyDiyStaticPath = (url = '') => {
  const assetHash = url.match(LEGACY_DIY_ASSET_URL_PATTERN)?.[1]?.toLowerCase();
  return assetHash ? LEGACY_DIY_ASSET_PATHS[assetHash] || '' : '';
};

const getMallStaticPath = (url = '') => {
  const legacyStaticPath = getLegacyDiyStaticPath(url);
  if (legacyStaticPath) return legacyStaticPath;

  const match = url.match(MALL_STATIC_PATH_PATTERN);
  return match?.[1] || '';
};

const resolveMallStaticUrl = (staticPath) => {
  if (staticUrl === 'local') {
    return `/${staticPath}`;
  }
  if (!staticUrl) return '';
  return `${staticUrl.replace(/\/+$/, '')}/${staticPath}`;
};

const cdn = (url = '', cdnurl = '') => {
  if (!url) return '';
  const mallStaticPath = getMallStaticPath(url);
  if (mallStaticPath) {
    const resolvedMallStaticUrl = resolveMallStaticUrl(mallStaticPath);
    if (resolvedMallStaticUrl) return resolvedMallStaticUrl;
    url = `/${mallStaticPath}`;
  }
  if (url.indexOf('http') === 0) {
    return url;
  }
  if (cdnurl === '') {
    cdnurl = $store('app').info.cdnurl;
  }
  return cdnurl + url;
};
export default {
  // 添加cdn域名前缀
  cdn,
  // 对象存储自动剪裁缩略图
  thumb: (url = '', params) => {
    url = cdn(url);
    return append_thumbnail_params(url, params);
  },
  // 静态资源地址
  static: (url = '', staticurl = '') => {
    if (staticurl === '') {
      staticurl = staticUrl;
    }
    if (staticurl !== 'local') {
      url = cdn(url, staticurl);
    }
    return url;
  },
  // css背景图片地址
  css: (url = '', staticurl = '') => {
    if (staticurl === '') {
      staticurl = staticUrl;
    }
    if (staticurl !== 'local') {
      url = cdn(url, staticurl);
    }
    // #ifdef APP-PLUS
    if (staticurl === 'local') {
      url = plus.io.convertLocalFileSystemURL(url);
    }
    // #endif
    return `url(${url})`;
  },
};

/**
 * 追加对象存储自动裁剪/压缩参数
 *
 * @return string
 */
function append_thumbnail_params(url, params) {
  const filesystem = $store('app').info.filesystem;
  if (filesystem === 'public') {
    return url;
  }
  let width = params.width || '200'; // 宽度
  let height = params.height || '200'; // 高度
  let mode = params.mode || 'lfit'; // 缩放模式
  let quality = params.quality || 90; // 压缩质量
  let gravity = params.gravity || 'center'; // 剪裁质量
  let suffix = '';
  let crop_str = '';
  let quality_str = '';
  let size = width + 'x' + height;
  switch (filesystem) {
    case 'aliyun':
      // 裁剪
      if (!gravity && gravity != 'center') {
        // 指定了裁剪区域
        mode = 'mfit';
        crop_str =
          '/crop,g_' + gravityFormatter('aliyun', gravity) + ',w_' + width + ',h_' + height;
      }

      // 质量压缩
      if (quality > 0 && quality < 100) {
        quality_str = '/quality,q_' + quality;
      }

      // 缩放参数
      suffix = 'x-oss-process=image/resize,m_' + mode + ',w_' + width + ',h_' + height;

      // 拼接裁剪和质量压缩
      suffix += crop_str + quality_str;
      break;
    case 'qcloud':
      let mode_str = 'thumbnail';
      if (mode == 'fill' || (!gravity && gravity != 'center')) {
        // 指定了裁剪区域
        mode_str = 'crop';
        mode = 'fill';
        crop_str = '/gravity/' + gravityFormatter('qcloud', gravity);
      }

      // 质量压缩
      if (quality > 0 && quality < 100) {
        quality_str = '/rquality/' + quality;
      }

      switch (mode) {
        case 'lfit':
          size = '' + size + '>';
          break;
        case 'mfit':
          size = '!' + size + 'r';
        case 'fill':
          break;
        case 'pad':
          size = size + '/pad/1';
          break;
        case 'fixed':
          size = size + '!';
          break;
      }

      suffix = 'imageMogr2/' + mode_str + '/' + size + crop_str + quality_str;
      break;
    case 'qiniu':
      if (mode == 'fill' || (!gravity && gravity != 'center')) {
        // 指定了裁剪区域,全部转为 mfit
        mode = 'mfit';
        crop_str = '/gravity/' + gravityFormatter('qiniu', gravity) + '/crop/' + size;
      }
      // 质量压缩
      if (quality > 0 && quality < 100) {
        quality_str = '/quality/' + quality;
      }

      switch (mode) {
        case 'lfit':
        case 'pad': // 七牛不支持在缩放之后，尺寸不足时，填充背景色,所以这里和 lfit 模式一样
          size = size + '>';
          break;
        case 'mfit':
          size = '!' + size + 'r';
          break;
        case 'fill':
          // 会被转为 mfit
          break;
        case 'fixed':
          size = size + '!';
          break;
      }

      suffix = 'imageMogr2/thumbnail/' + size + crop_str + quality_str;
      break;
  }
  return url + '?' + suffix;
}

/**
 * 裁剪区域格式转换
 *
 * @param string $type aliyun|qcloud|qiniu
 * @param string $gravity 统一的裁剪区域字符
 *
 * @return string
 */
function gravityFormatter(type, gravity) {
  let gravityFormatMap = {
    aliyun: {
      north_west: 'nw', // 左上
      north: 'north', // 中上
      north_east: 'ne', // 右上
      west: 'west', // 左中
      center: 'center', // 中部
      east: 'east', // 右中
      south_west: 'sw', // 左下
      south: 'south', // 中下
      south_east: 'se', // 右下
    },
    qcloud: {
      northwest: 'nw', // 左上
      north: 'north', // 中上
      northeast: 'ne', // 右上
      west: 'west', // 左中
      center: 'center', // 中部
      east: 'east', // 右中
      southwest: 'sw', // 左下
      south: 'south', // 中下
      southeast: 'se', // 右下
    },
    qiniu: {
      NorthWest: 'nw', // 左上
      North: 'north', // 中上
      NorthEast: 'ne', // 右上
      West: 'west', // 左中
      Center: 'center', // 中部
      East: 'east', // 右中
      SouthWest: 'sw', // 左下
      South: 'south', // 中下
      SouthEast: 'se', // 右下
    },
  };

  return gravityFormatMap[type][gravity];
}
