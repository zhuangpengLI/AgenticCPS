import request from '@/sheep/request';

const CpsGoodsApi = {
  parseContent: ({ platformCode, originalContent }) => {
    return request({
      url: '/cps/goods/parse',
      method: 'POST',
      data: {
        platformCode,
        originalContent,
      },
      custom: {
        showLoading: false,
      },
    });
  },
  searchGoods: ({ keyword, platformCode, pageNo, pageSize, sortType, hasCoupon }) => {
    return request({
      url: '/cps/goods/search',
      method: 'GET',
      params: {
        keyword,
        platformCode,
        pageNo,
        pageSize,
        sortType,
        hasCoupon,
      },
      custom: {
        showLoading: false,
      },
    });
  },
  compareGoods: ({ keyword, pageSize, sortType, hasCoupon }) => {
    return request({
      url: '/cps/goods/compare',
      method: 'GET',
      params: {
        keyword,
        pageSize,
        sortType,
        hasCoupon,
      },
      custom: {
        showLoading: false,
      },
    });
  },
  getDetail: ({ platformCode, goodsId, goodsSign }) => {
    return request({
      url: '/cps/goods/detail',
      method: 'GET',
      params: {
        platformCode,
        goodsId,
        goodsSign,
      },
      custom: {
        showLoading: false,
      },
    });
  },
  generateLink: ({ platformCode, goodsId, goodsSign, adzoneId, originalContent }) => {
    return request({
      url: '/cps/goods/link',
      method: 'POST',
      data: {
        platformCode,
        goodsId,
        goodsSign,
        adzoneId,
        originalContent,
      },
      custom: {
        auth: true,
      },
    });
  },
};

export default CpsGoodsApi;
