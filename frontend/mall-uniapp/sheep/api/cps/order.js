import request from '@/sheep/request';

const CpsOrderApi = {
  getOrderPage: (params) => {
    return request({
      url: '/cps/order/page',
      method: 'GET',
      params,
      custom: {
        showLoading: false,
        auth: true,
      },
    });
  },
  getOrder: (id) => {
    return request({
      url: `/cps/order/${id}`,
      method: 'GET',
      custom: {
        showLoading: false,
        auth: true,
      },
    });
  },
  claimOrder: (data) => {
    return request({
      url: '/cps/order/claim',
      method: 'POST',
      data,
      custom: {
        auth: true,
      },
    });
  },
  getClaimList: () => {
    return request({
      url: '/cps/order/claim/list',
      method: 'GET',
      custom: {
        showLoading: false,
        auth: true,
      },
    });
  },
};

export default CpsOrderApi;
