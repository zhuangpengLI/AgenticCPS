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
      custom: {
        auth: true,
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
        auth: true,
      },
    });
  },
  getWithdraw: (id) => {
    return request({
      url: `/cps/withdraw/${id}`,
      method: 'GET',
      custom: {
        showLoading: false,
        auth: true,
      },
    });
  },
};

export default CpsWithdrawApi;
