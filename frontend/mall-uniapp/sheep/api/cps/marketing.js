import request from '@/sheep/request';

const CpsMarketingApi = {
  getActivityCenter: (params = {}) =>
    request({
      url: '/cps/marketing/activity-center',
      method: 'GET',
      params,
      custom: { showLoading: false },
    }),
  getSelectionThemes: (params = {}) =>
    request({
      url: '/cps/marketing/selection-themes',
      method: 'GET',
      params,
      custom: { showLoading: false },
    }),
  getSelectionThemeItems: (themeId) =>
    request({
      url: '/cps/marketing/selection-theme-items',
      method: 'GET',
      params: { themeId },
      custom: { showLoading: false },
    }),
  generateActivityPromotion: ({ activityId, channelTag }) =>
    request({
      url: '/cps/rebate-activity/promotion',
      method: 'POST',
      data: { activityId, channelTag },
      custom: { auth: true },
    }),
};

export default CpsMarketingApi;
