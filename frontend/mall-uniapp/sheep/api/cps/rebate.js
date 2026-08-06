import request from '@/sheep/request';

const CpsRebateApi = {
  getAccount: () => {
    return request({
      url: '/cps/rebate/account',
      method: 'GET',
      custom: {
        showLoading: false,
        auth: true,
      },
    });
  },
  getRecordPage: (params) => {
    return request({
      url: '/cps/rebate/record/page',
      method: 'GET',
      params,
      custom: {
        showLoading: false,
        auth: true,
      },
    });
  },
  getDebtSummary: () => {
    return request({
      url: '/cps/rebate/debt/summary',
      method: 'GET',
      custom: {
        showLoading: false,
        auth: true,
      },
    });
  },
  getDebtRepaymentPage: (params) => {
    return request({
      url: '/cps/rebate/debt/repayment/page',
      method: 'GET',
      params,
      custom: {
        showLoading: false,
        auth: true,
      },
    });
  },
};

export default CpsRebateApi;
