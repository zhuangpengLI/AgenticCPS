const CPS_EVENT_FIELDS = Object.freeze({
  cps_goods_search: ['platformCode', 'result', 'resultCount', 'hasCoupon', 'sortType'],
  cps_goods_detail: ['platformCode', 'result'],
  cps_promotion_link: ['platformCode', 'result', 'actionType'],
  cps_order_claim: ['platformCode', 'result', 'claimStatus'],
  cps_withdraw_submit: ['withdrawType', 'result'],
});

export function trackCpsEvent(eventName, payload = {}) {
  const allowedFields = CPS_EVENT_FIELDS[eventName];
  if (!allowedFields || typeof uni === 'undefined' || typeof uni.report !== 'function') return;

  const safePayload = {};
  allowedFields.forEach((field) => {
    const fieldValue = payload[field];
    if (fieldValue !== undefined && fieldValue !== null && fieldValue !== '') {
      safePayload[field] = fieldValue;
    }
  });

  try {
    uni.report(eventName, JSON.stringify(safePayload));
  } catch (error) {
    // 统计不可用时不影响用户主流程。
  }
}
