import $store from '@/sheep/store';
import { showAuthModal, showShareModal } from '@/sheep/hooks/useModal';
import { isNumber, isString, isEmpty, startsWith, isObject, isNil, clone } from 'lodash-es';
import throttle from '@/sheep/helper/throttle';

let authGuardsInstalled = false;
let pendingAuthRoute = '';

function normalizeLocalUrl(value = '') {
  if (!isString(value) || !value) return '';
  if (startsWith(value, 'http') || startsWith(value, 'action:') || startsWith(value, 'plugin-')) {
    return '';
  }
  const [pathWithQuery, hash = ''] = value.split('#');
  const [rawPath, query = ''] = pathWithQuery.split('?');
  const path = rawPath.startsWith('/') ? rawPath : `/${rawPath}`;
  return `${path}${query ? `?${query}` : ''}${hash ? `#${hash}` : ''}`;
}

function getRouteByUrl(value = '') {
  const url = normalizeLocalUrl(value);
  if (!url) return null;
  return ROUTES_MAP[url.split(/[?#]/)[0]] || null;
}

function guardRoute(value, { remember = true } = {}) {
  const url = normalizeLocalUrl(value);
  const route = getRouteByUrl(url);
  // 未知路径、外链和公开页面全部放行；进入已知公开页代表放弃之前被拦截的目标
  if (!route?.meta?.auth) {
    if (remember && route) pendingAuthRoute = '';
    return true;
  }
  if ($store('user').isLogin) return true;

  if (remember) pendingAuthRoute = url;
  showAuthModal();
  return false;
}

function restorePendingAuthRoute() {
  if (!pendingAuthRoute || !$store('user').isLogin) return;
  const target = pendingAuthRoute;
  pendingAuthRoute = '';
  setTimeout(() => {
    uni.reLaunch({ url: target });
  }, 0);
}

function installAuthGuards() {
  if (authGuardsInstalled) return;
  authGuardsInstalled = true;

  ['navigateTo', 'redirectTo', 'reLaunch', 'switchTab'].forEach((method) => {
    uni.addInterceptor(method, {
      invoke(args = {}) {
        if (!guardRoute(args.url)) return false;
      },
    });
  });
  uni.$on('auth:login', restorePendingAuthRoute);
  uni.$on('auth:required', guardCurrentRoute);
  uni.$on('auth:logout', () => {
    pendingAuthRoute = '';
  });
}

function queryObjectToString(query = {}) {
  return Object.keys(query)
    .map((key) => `${encodeURIComponent(key)}=${encodeURIComponent(query[key] ?? '')}`)
    .join('&');
}

function guardEntry(options = {}) {
  const path = options?.path || options?.route;
  if (!path) return guardCurrentRoute();
  const query = queryObjectToString(options.query || {});
  return guardRoute(`${path}${query ? `?${query}` : ''}`);
}

function guardCurrentRoute() {
  const pages = getCurrentPages();
  const currentPage = pages[pages.length - 1];
  if (!currentPage) return true;
  const path = currentPage.route || currentPage.$page?.route || currentPage.$page?.fullPath;
  const query = queryObjectToString(currentPage.options || currentPage.$page?.options || {});
  return guardRoute(`${path || ''}${query ? `?${query}` : ''}`);
}

const _go = (
  path,
  params = {},
  options = {
    redirect: false,
  },
) => {
  let page = ''; // 跳转页面
  let query = ''; // 页面参数
  let url = ''; // 跳转页面完整路径

  if (isString(path)) {
    // 判断跳转类型是 path ｜ 还是http
    if (startsWith(path, 'http')) {
      // #ifdef H5
      window.location = path;
      return;
      // #endif
      // #ifndef H5
      page = `/pages/public/webview`;
      query = `url=${encodeURIComponent(path)}`;
      // #endif
    } else if (startsWith(path, 'action:')) {
      handleAction(path);
      return;
    } else {
      [page, query] = path.split('?');
    }
    if (!isEmpty(params)) {
      let query2 = paramsToQuery(params);
      if (isEmpty(query)) {
        query = query2;
      } else {
        query += '&' + query2;
      }
    }
  }

  if (isObject(path)) {
    page = path.url;
    if (!isNil(path.params)) {
      query = paramsToQuery(path.params);
    }
  }

  const nextRoute = ROUTES_MAP[page];

  // 未找到指定跳转页面
  // mark: 跳转404页
  if (!nextRoute) {
    console.log(`%c跳转路径参数错误<${page || 'EMPTY'}>`, 'color:red;background:yellow');
    return;
  }

  url = page;
  if (!isEmpty(query)) {
    url += `?${query}`;
  }

  // 页面登录拦截统一复用全局守卫
  if (!guardRoute(url)) return;

  // 跳转底部导航
  if (TABBAR.includes(page)) {
    // wx.switchTab: url 不支持 queryString
    // 设置全局变量
    const params = queryToParams(query);
    $store('app').setParamsForTabbar(params);
    // 请记得在业务代码里使用完后，清理掉全局状态，避免影响下次跳转
    uni.switchTab({
      url: page,
    });
    return;
  }

  // 使用redirect跳转
  if (options.redirect) {
    uni.redirectTo({
      url,
    });
    return;
  }

  uni.navigateTo({
    url,
  });
};

// 限流 防止重复点击跳转
function go(...args) {
  throttle(() => {
    _go(...args);
  });
}

function paramsToQuery(params) {
  if (isEmpty(params)) {
    return '';
  }
  // return new URLSearchParams(Object.entries(params)).toString();
  let query = [];
  for (let key in params) {
    query.push(key + '=' + params[key]);
  }

  return query.join('&');
}

function queryToParams(query) {
  if (isEmpty(query)) {
    return {};
  }
  let params = {};
  let pairs = query.split('&');
  for (let i = 0; i < pairs.length; i++) {
    let pair = pairs[i].split('=');
    params[decodeURIComponent(pair[0])] = decodeURIComponent(pair[1] || '');
  }
  return params;
}

function back() {
  // #ifdef H5
  history.back();
  // #endif

  // #ifndef H5
  uni.navigateBack();
  // #endif
}

function redirect(path, params = {}) {
  go(path, params, {
    redirect: true,
  });
}

// 检测是否有浏览器历史
function hasHistory() {
  // #ifndef H5
  const pages = getCurrentPages();
  if (pages.length > 1) {
    return true;
  }
  return false;
  // #endif

  // #ifdef H5
  return !!history.state.back;
  // #endif
}

function getCurrentRoute(field = '') {
  let currentPage = getCurrentPage();
  // #ifdef MP
  currentPage.$page['route'] = currentPage.route;
  currentPage.$page['options'] = currentPage.options;
  // #endif
  if (field !== '') {
    return currentPage.$page[field];
  } else {
    return currentPage.$page;
  }
}

function getCurrentPage() {
  let pages = getCurrentPages();
  return pages[pages.length - 1];
}

function handleAction(path) {
  const action = path.split(':');
  switch (action[1]) {
    case 'showShareModal':
      showShareModal();
      break;
  }
}

function error(errCode, errMsg = '') {
  redirect('/pages/public/error', {
    errCode,
    errMsg,
  });
}

export default {
  go,
  back,
  hasHistory,
  redirect,
  getCurrentPage,
  getCurrentRoute,
  guardCurrentRoute,
  guardEntry,
  guardRoute,
  installAuthGuards,
  error,
};
