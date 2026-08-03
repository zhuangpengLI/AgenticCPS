/**
 * @typedef {{
 *   status: string,
 *   goods?: { platformCode?: string, goodsId?: string, goodsSign?: string }
 * }} TransferRow
 */

/**
 * @param {TransferRow} row
 * @returns {string | undefined}
 */
const getGoodsKey = (row) => {
  const goodsIdentity = row.goods?.goodsId || row.goods?.goodsSign
  if (!goodsIdentity) return undefined
  return `${row.goods?.platformCode || ''}:${goodsIdentity}`
}

/**
 * 解析失败仅在全部输入均解析失败时展示；其余结果按商品去重且成功优先。
 *
 * @template {TransferRow} T
 * @param {T[]} rows
 * @returns {T[]}
 */
export const selectVisibleTransferRows = (rows) => {
  if (rows.length === 0 || rows.every((row) => row.status === 'PARSE_FAILED')) return rows

  const parsedRows = rows.filter((row) => row.status !== 'PARSE_FAILED')
  /** @type {Map<string, T>} */
  const preferredRowsByGoods = new Map()
  parsedRows.forEach((row) => {
    const goodsKey = getGoodsKey(row)
    if (!goodsKey) return
    const previous = preferredRowsByGoods.get(goodsKey)
    if (!previous || (previous.status !== 'SUCCESS' && row.status === 'SUCCESS')) {
      preferredRowsByGoods.set(goodsKey, row)
    }
  })

  return parsedRows.filter((row) => {
    const goodsKey = getGoodsKey(row)
    if (!goodsKey) return true
    return preferredRowsByGoods.get(goodsKey) === row
  })
}
