import request from '@/sheep/request';

const CpsWithdrawApi = {
  createWithdraw: ({
    amountCent,
    withdrawType,
    withdrawAccount,
    withdrawAccountName,
    idempotencyKey,
  }) => {
    return request({
      url: '/cps/withdraw/create',
      method: 'POST',
      data: {
        amountCent,
        withdrawType,
        withdrawAccount,
        withdrawAccountName,
        idempotencyKey,
      },
    });
  },
  getWithdrawPage: (params) => {
    return request({
      url: '/cps/withdraw/page',
      method: 'GET',
      params,
      custom: {
        showLoading: false,
      },
    });
  },
  getWithdraw: (id) => {
    return request({
      url: `/cps/withdraw/${id}`,
      method: 'GET',
      custom: {
        showLoading: false,
      },
    });
  },
};

export default CpsWithdrawApi;
