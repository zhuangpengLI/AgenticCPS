import request from '@/sheep/request';

const CpsExchangeApi = {
  preview: (amount) =>
    request({
      url: '/cps/rebate/token-exchange/preview',
      method: 'POST',
      data: { amount },
      custom: { auth: true, showLoading: false },
    }),
  submit: ({ amount, idempotencyKey }) =>
    request({
      url: '/cps/rebate/token-exchange/submit',
      method: 'POST',
      data: { amount, idempotencyKey },
      custom: { auth: true },
    }),
  getStatus: (exchangeOrderNo) =>
    request({
      url: `/cps/rebate/token-exchange/${encodeURIComponent(exchangeOrderNo)}`,
      method: 'GET',
      custom: { auth: true, showLoading: false },
    }),
};

export default CpsExchangeApi;
