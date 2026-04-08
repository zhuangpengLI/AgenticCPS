# CPS 前端管理页面开发

## 现状分析

### 已存在的页面（无需开发）
| 路径 | 状态 |
|------|------|
| `views/cps/order/index.vue` | 完整 |
| `views/cps/rebate/config/index.vue` | 完整 |
| `views/cps/rebate/record/index.vue` | 完整 |
| `views/cps/freeze/config/index.vue` | 完整 |
| `views/cps/freeze/record/index.vue` | 完整 |
| `views/cps/risk/index.vue` | 完整 |
| `views/cps/statistics/index.vue` | 完整 |
| `views/cps/transfer/index.vue` | 完整 |

### 缺失的页面（需要开发）
| 需创建 | 对应后端 Controller 路径 |
|--------|--------------------------|
| `api/cps/platform.ts` | `/admin-api/cps/platform/*` |
| `views/cps/platform/index.vue` | 平台配置 CRUD |
| `api/cps/adzone.ts` | `/admin-api/cps/adzone/*` |
| `views/cps/adzone/index.vue` | 推广位管理 CRUD |
| `api/cps/withdraw.ts` | `/admin-api/cps/withdraw/*` |
| `views/cps/withdraw/index.vue` | 提现审核管理 |

### 需要修复的问题
`api/cps/rebate.ts` 中的返利配置 API 路径错误（缺少 `/admin-api` 前缀）：
- `/cps/rebate-config/create` → `/admin-api/cps/rebate-config/create`
- `/cps/rebate-config/update` → `/admin-api/cps/rebate-config/update`
- 等其他4个接口

---

## Task 1: 修复 rebate.ts API 路径错误

文件：`frontend/admin-vue3/src/api/cps/rebate.ts`

将第72-98行所有 `/cps/rebate-config/` 替换为 `/admin-api/cps/rebate-config/`。

---

## Task 2: 创建平台配置 API 文件

新建 `frontend/admin-vue3/src/api/cps/platform.ts`

基于后端 VO（`CpsPlatformRespVO`、`CpsPlatformSaveReqVO`、`CpsPlatformPageReqVO`）定义类型和 API：

```typescript
export interface CpsPlatformVO {
  id: number
  platformCode: string
  platformName: string
  platformLogo?: string
  appKey?: string
  apiBaseUrl?: string
  defaultAdzoneId?: string
  platformServiceRate?: number
  sort?: number
  status: number
  extraConfig?: string
  remark?: string
  createTime: Date
}
export interface CpsPlatformSaveVO {
  id?: number
  platformCode: string
  platformName: string
  appKey: string
  appSecret: string
  defaultAdzoneId: string
  status: number
  // ...其余可选字段
}
export const CpsPlatformApi = {
  createPlatform: (data) => request.post({ url: '/admin-api/cps/platform/create', data }),
  updatePlatform: (data) => request.put({ url: '/admin-api/cps/platform/update', data }),
  deletePlatform: (id) => request.delete({ url: '/admin-api/cps/platform/delete', params: { id } }),
  getPlatform: (id) => request.get({ url: '/admin-api/cps/platform/get', params: { id } }),
  getPlatformPage: (params) => request.get({ url: '/admin-api/cps/platform/page', params }),
  getEnabledPlatformList: () => request.get({ url: '/admin-api/cps/platform/list-enabled' }),
}
```

---

## Task 3: 创建平台配置页面

新建 `frontend/admin-vue3/src/views/cps/platform/index.vue`

功能要点（参考 `freeze/config/index.vue` 风格）：
- 搜索栏：平台名称（文本）、状态（下拉）
- 列表：ID、平台编码、平台名称、AppKey（脱敏）、默认推广位、服务费率、排序、状态（Tag + 开关）、创建时间、操作
- 操作按钮：编辑、删除（需权限 `cps:platform:create/update/delete`）
- 新增/编辑弹窗（Dialog）：平台编码（新增可编辑、编辑禁用）、平台名称、AppKey、AppSecret（编辑时留空=不修改）、默认推广位ID、服务费率、状态、扩展配置(JSON)、备注
- 表单验证：platformCode、platformName、appKey、appSecret（新增必填）、defaultAdzoneId、status 必填

---

## Task 4: 创建推广位管理 API 文件

新建 `frontend/admin-vue3/src/api/cps/adzone.ts`

基于后端 VO 定义类型和 API：
```typescript
export interface CpsAdzoneVO { id, platformCode, adzoneId, adzoneName, adzoneType, isDefault, status, createTime }
export interface CpsAdzoneSaveVO { id?, platformCode, adzoneId, adzoneName?, adzoneType?, isDefault?, status }
export const CpsAdzoneApi = {
  createAdzone, updateAdzone, deleteAdzone, getAdzonePage, getAdzoneListByPlatform
}
// 接口路径：/admin-api/cps/adzone/*
```

---

## Task 5: 创建推广位管理页面

新建 `frontend/admin-vue3/src/views/cps/adzone/index.vue`

功能要点：
- 搜索栏：平台（下拉：淘宝/京东/拼多多/抖音）、推广位名称（文本）、状态（下拉）
- 列表：ID、平台（Tag）、推广位ID、推广位名称、类型、是否默认（Tag）、状态（Tag）、创建时间、操作
- 操作：编辑、删除（权限 `cps:adzone:create/update/delete`）
- 弹窗表单：平台编码（下拉选择）、推广位ID、推广位名称、类型、是否默认（开关）、状态（单选）

---

## Task 6: 创建提现管理 API 文件

新建 `frontend/admin-vue3/src/api/cps/withdraw.ts`

根据 `CpsWithdrawDO`（字段：memberId, withdrawNo, withdrawType, withdrawAccount, amount, feeAmount, actualAmount, status, auditTime, reviewNote, transferStatus, transferTime）：

```typescript
export interface CpsWithdrawVO {
  id, memberId, withdrawNo, withdrawType, withdrawAccount, withdrawAccountName,
  amount, feeAmount, actualAmount, status, auditTime, reviewNote,
  transferStatus, transferTime, createTime
}
export const CpsWithdrawApi = {
  getWithdrawPage: (params) => request.get({ url: '/admin-api/cps/withdraw/page', params }),
  getWithdraw: (id) => request.get({ url: '/admin-api/cps/withdraw/get', params: { id } }),
  approveWithdraw: (id, note?) => request.put({ url: '/admin-api/cps/withdraw/approve', params: { id }, data: { note } }),
  rejectWithdraw: (id, note) => request.put({ url: '/admin-api/cps/withdraw/reject', params: { id }, data: { note } }),
}
```

---

## Task 7: 创建提现管理页面

新建 `frontend/admin-vue3/src/views/cps/withdraw/index.vue`

功能要点（区别于纯 CRUD，包含审核流程）：
- 搜索栏：会员ID、提现类型（支付宝/微信/银行卡）、状态（待审核/审核通过/已驳回/已到账）、创建时间范围
- 列表：ID、提现单号、会员ID、提现方式、提现账户、提现金额、手续费、实际到账、状态（Tag）、申请时间、审核时间、操作
- 操作：查看详情、审核通过（仅 pending 状态）、驳回（仅 pending 状态）
- 详情弹窗：显示所有字段
- 审核弹窗：支持填写审核备注，通过/驳回
- 状态 Tag 映射：created→待审核、approved→审核通过、rejected→已驳回、transferred→已到账、failed→打款失败
- 权限：`cps:withdraw:query`、`cps:withdraw:update`

---

## 文件清单

| 操作 | 文件 |
|------|------|
| 修改 | `src/api/cps/rebate.ts` |
| 新建 | `src/api/cps/platform.ts` |
| 新建 | `src/views/cps/platform/index.vue` |
| 新建 | `src/api/cps/adzone.ts` |
| 新建 | `src/views/cps/adzone/index.vue` |
| 新建 | `src/api/cps/withdraw.ts` |
| 新建 | `src/views/cps/withdraw/index.vue` |

注意：所有后端 Controller 路径均以 `/admin-api` 开头（后端 `RequestMapping` 为 `/cps/platform`，通过 Spring 全局前缀 `/admin-api` 拼接）。
