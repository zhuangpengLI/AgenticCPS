import request from '@/sheep/request';

const CpsRebateApi = {
  getAccount: () => {
    return request({
      url: '/cps/rebate/account',
      method: 'GET',
      custom: {
        showLoading: false,
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
      },
    });
  },
  getDebtSummary: () => {
    return request({
      url: '/cps/rebate/debt/summary',
      method: 'GET',
      custom: {
        showLoading: false,
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
      },
    });
  },
};

export default CpsRebateApi;
