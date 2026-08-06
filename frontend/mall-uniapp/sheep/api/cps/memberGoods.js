import request from '@/sheep/request';

const authOptions = { auth: true, showLoading: false };

const CpsMemberGoodsApi = {
  recordHistory: (data) =>
    request({
      url: '/cps/browse-history/record',
      method: 'POST',
      data,
      custom: authOptions,
    }),
  getHistoryPage: (params) =>
    request({
      url: '/cps/browse-history/page',
      method: 'GET',
      params,
      custom: authOptions,
    }),
  cleanHistory: () =>
    request({
      url: '/cps/browse-history/clean',
      method: 'DELETE',
      custom: authOptions,
    }),
  createFavorite: (data) =>
    request({
      url: '/cps/favorite/create',
      method: 'POST',
      data,
      custom: authOptions,
    }),
  deleteFavorite: (data) =>
    request({
      url: '/cps/favorite/delete',
      method: 'DELETE',
      data,
      custom: authOptions,
    }),
  getFavoritePage: (params) =>
    request({
      url: '/cps/favorite/page',
      method: 'GET',
      params,
      custom: authOptions,
    }),
};

export default CpsMemberGoodsApi;
