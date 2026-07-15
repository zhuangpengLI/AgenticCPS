import request from '@/sheep/request';

const CpsOrderApi = {
  getOrderPage: (params) => {
    return request({
      url: '/cps/order/page',
      method: 'GET',
      params,
      custom: {
        showLoading: false,
      },
    });
  },
  getOrder: (id) => {
    return request({
      url: `/cps/order/${id}`,
      method: 'GET',
      custom: {
        showLoading: false,
      },
    });
  },
};

export default CpsOrderApi;
