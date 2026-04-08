<template>
  <ContentWrap>
    <!-- 搜索栏 -->
    <el-form
      class="-mb-15px"
      :model="queryParams"
      ref="queryFormRef"
      :inline="true"
      label-width="80px"
    >
      <el-form-item label="平台" prop="platformCode">
        <el-select
          v-model="queryParams.platformCode"
          placeholder="请选择平台"
          clearable
          class="!w-160px"
        >
          <el-option label="全平台" value="" />
          <el-option label="淘宝" value="taobao" />
          <el-option label="京东" value="jd" />
          <el-option label="拼多多" value="pdd" />
          <el-option label="抖音" value="douyin" />
        </el-select>
      </el-form-item>
      <el-form-item label="状态" prop="status">
        <el-select
          v-model="queryParams.status"
          placeholder="请选择状态"
          clearable
          class="!w-120px"
        >
          <el-option label="启用" :value="1" />
          <el-option label="禁用" :value="0" />
        </el-select>
      </el-form-item>
      <el-form-item>
        <el-button @click="handleQuery">
          <Icon icon="ep:search" class="mr-5px" /> 搜索
        </el-button>
        <el-button @click="resetQuery">
          <Icon icon="ep:refresh" class="mr-5px" /> 重置
        </el-button>
        <el-button type="primary" @click="openForm()" v-hasPermi="['cps:rebate-config:create']">
          <Icon icon="ep:plus" class="mr-5px" /> 新增配置
        </el-button>
      </el-form-item>
    </el-form>
  </ContentWrap>

  <!-- 列表 -->
  <ContentWrap>
    <el-table v-loading="loading" :data="list" stripe>
      <el-table-column label="ID" align="center" prop="id" width="70" />
      <el-table-column label="平台" align="center" prop="platformCode" width="100">
        <template #default="scope">
          <el-tag :type="platformTagType(scope.row.platformCode)" size="small">
            {{ platformLabel(scope.row.platformCode) }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="会员等级" align="center" prop="memberLevelId" width="100">
        <template #default="scope">
          {{ scope.row.memberLevelId ? `等级 ${scope.row.memberLevelId}` : '不限' }}
        </template>
      </el-table-column>
      <el-table-column label="返利比例" align="center" width="100">
        <template #default="scope">
          <span class="text-green-600 font-medium">{{ scope.row.rebateRate }}%</span>
        </template>
      </el-table-column>
      <el-table-column label="最小返利" align="center" width="100">
        <template #default="scope">
          {{ scope.row.minRebateAmount ? `¥${formatAmount(scope.row.minRebateAmount)}` : '不限' }}
        </template>
      </el-table-column>
      <el-table-column label="最大返利" align="center" width="100">
        <template #default="scope">
          {{ scope.row.maxRebateAmount ? `¥${formatAmount(scope.row.maxRebateAmount)}` : '不限' }}
        </template>
      </el-table-column>
      <el-table-column label="优先级" align="center" prop="priority" width="80" />
      <el-table-column label="状态" align="center" width="80">
        <template #default="scope">
          <el-switch
            v-model="scope.row.status"
            :active-value="1"
            :inactive-value="0"
            @change="handleStatusChange(scope.row)"
            v-hasPermi="['cps:rebate-config:update']"
          />
        </template>
      </el-table-column>
      <el-table-column
        label="创建时间"
        align="center"
        prop="createTime"
        width="165"
        :formatter="dateFormatter"
      />
      <el-table-column label="操作" align="center" fixed="right" width="130">
        <template #default="scope">
          <el-button
            type="primary"
            link
            @click="openForm(scope.row)"
            v-hasPermi="['cps:rebate-config:update']"
          >
            编辑
          </el-button>
          <el-button
            type="danger"
            link
            @click="handleDelete(scope.row.id)"
            v-hasPermi="['cps:rebate-config:delete']"
          >
            删除
          </el-button>
        </template>
      </el-table-column>
    </el-table>
    <Pagination
      :total="total"
      v-model:page="queryParams.pageNo"
      v-model:limit="queryParams.pageSize"
      @pagination="getList"
    />
  </ContentWrap>

  <!-- 新增/编辑弹窗 -->
  <el-dialog
    v-model="formVisible"
    :title="formTitle"
    width="500px"
    destroy-on-close
    @closed="resetForm"
  >
    <el-form
      ref="formRef"
      :model="formData"
      :rules="formRules"
      label-width="120px"
    >
      <el-form-item label="平台" prop="platformCode">
        <el-select v-model="formData.platformCode" placeholder="留空表示全平台" clearable class="w-full">
          <el-option label="淘宝" value="taobao" />
          <el-option label="京东" value="jd" />
          <el-option label="拼多多" value="pdd" />
          <el-option label="抖音" value="douyin" />
        </el-select>
        <div class="el-form-item-hint text-gray-400 text-xs mt-1">不选则对所有平台生效</div>
      </el-form-item>
      <el-form-item label="会员等级ID" prop="memberLevelId">
        <el-input
          v-model.number="formData.memberLevelId"
          placeholder="留空表示不限等级"
          clearable
        />
        <div class="el-form-item-hint text-gray-400 text-xs mt-1">不填则对所有等级生效</div>
      </el-form-item>
      <el-form-item label="返利比例 (%)" prop="rebateRate">
        <el-input-number
          v-model="formData.rebateRate"
          :min="0"
          :max="100"
          :precision="2"
          :step="0.1"
          class="w-full"
        />
      </el-form-item>
      <el-form-item label="最小返利金额" prop="minRebateAmount">
        <el-input-number
          v-model="formData.minRebateAmount"
          :min="0"
          :precision="2"
          :step="0.01"
          placeholder="0 表示不限"
          class="w-full"
        />
      </el-form-item>
      <el-form-item label="最大返利金额" prop="maxRebateAmount">
        <el-input-number
          v-model="formData.maxRebateAmount"
          :min="0"
          :precision="2"
          :step="0.01"
          placeholder="0 表示不限"
          class="w-full"
        />
      </el-form-item>
      <el-form-item label="优先级" prop="priority">
        <el-input-number
          v-model="formData.priority"
          :min="0"
          :max="999"
          :step="1"
          class="w-full"
        />
        <div class="el-form-item-hint text-gray-400 text-xs mt-1">数字越大优先级越高</div>
      </el-form-item>
      <el-form-item label="状态" prop="status">
        <el-radio-group v-model="formData.status">
          <el-radio :value="1">启用</el-radio>
          <el-radio :value="0">禁用</el-radio>
        </el-radio-group>
      </el-form-item>
    </el-form>
    <template #footer>
      <el-button @click="formVisible = false">取 消</el-button>
      <el-button type="primary" @click="handleSubmit" :loading="submitLoading">确 定</el-button>
    </template>
  </el-dialog>
</template>

<script setup lang="ts">
import { dateFormatter } from '@/utils/formatTime'
import * as RebateApi from '@/api/cps/rebate'
import type { CpsRebateConfigVO, CpsRebateConfigSaveVO, CpsRebateConfigPageReqVO } from '@/api/cps/rebate'

defineOptions({ name: 'CpsRebateConfig' })

const message = useMessage()

const loading = ref(false)
const total = ref(0)
const list = ref<CpsRebateConfigVO[]>([])
const queryFormRef = ref()

const queryParams = reactive<CpsRebateConfigPageReqVO>({
  pageNo: 1,
  pageSize: 10,
  platformCode: undefined,
  status: undefined
})

const platformTagType = (code?: string) => {
  if (!code) return 'info'
  const map: Record<string, string> = { taobao: 'danger', jd: 'primary', pdd: 'warning', douyin: '' }
  return map[code] || 'info'
}
const platformLabel = (code?: string) => {
  if (!code) return '全平台'
  const map: Record<string, string> = { taobao: '淘宝', jd: '京东', pdd: '拼多多', douyin: '抖音' }
  return map[code] || code
}
const formatAmount = (val?: number) => {
  if (val == null) return '0.00'
  return Number(val).toFixed(2)
}

/** 查询列表 */
const getList = async () => {
  loading.value = true
  try {
    const data = await RebateApi.getCpsRebateConfigPage(queryParams)
    list.value = data.list
    total.value = data.total
  } finally {
    loading.value = false
  }
}

const handleQuery = () => {
  queryParams.pageNo = 1
  getList()
}
const resetQuery = () => {
  queryFormRef.value.resetFields()
  handleQuery()
}

/** 状态切换 */
const handleStatusChange = async (row: CpsRebateConfigVO) => {
  try {
    await RebateApi.updateCpsRebateConfig({
      id: row.id,
      rebateRate: row.rebateRate,
      status: row.status,
      platformCode: row.platformCode,
      memberLevelId: row.memberLevelId,
      maxRebateAmount: row.maxRebateAmount,
      minRebateAmount: row.minRebateAmount,
      priority: row.priority
    })
    message.success(row.status === 1 ? '已启用' : '已禁用')
  } catch {
    // 回滚状态
    row.status = row.status === 1 ? 0 : 1
  }
}

/** 删除 */
const handleDelete = async (id: number) => {
  try {
    await message.delConfirm()
    await RebateApi.deleteCpsRebateConfig(id)
    message.success('删除成功')
    await getList()
  } catch {}
}

/** 表单弹窗 */
const formVisible = ref(false)
const formTitle = ref('')
const submitLoading = ref(false)
const formRef = ref()

const formData = reactive<CpsRebateConfigSaveVO>({
  id: undefined,
  platformCode: undefined,
  memberLevelId: undefined,
  rebateRate: 0,
  minRebateAmount: undefined,
  maxRebateAmount: undefined,
  status: 1,
  priority: 0
})

const formRules = {
  rebateRate: [{ required: true, message: '返利比例不能为空', trigger: 'blur' }],
  status: [{ required: true, message: '状态不能为空', trigger: 'change' }]
}

const openForm = (row?: CpsRebateConfigVO) => {
  resetForm()
  if (row) {
    formTitle.value = '编辑返利配置'
    Object.assign(formData, {
      id: row.id,
      platformCode: row.platformCode,
      memberLevelId: row.memberLevelId,
      rebateRate: row.rebateRate,
      minRebateAmount: row.minRebateAmount,
      maxRebateAmount: row.maxRebateAmount,
      status: row.status,
      priority: row.priority
    })
  } else {
    formTitle.value = '新增返利配置'
  }
  formVisible.value = true
}

const resetForm = () => {
  formData.id = undefined
  formData.platformCode = undefined
  formData.memberLevelId = undefined
  formData.rebateRate = 0
  formData.minRebateAmount = undefined
  formData.maxRebateAmount = undefined
  formData.status = 1
  formData.priority = 0
  formRef.value?.resetFields()
}

const handleSubmit = async () => {
  await formRef.value.validate()
  submitLoading.value = true
  try {
    if (formData.id) {
      await RebateApi.updateCpsRebateConfig(formData)
      message.success('更新成功')
    } else {
      await RebateApi.createCpsRebateConfig(formData)
      message.success('创建成功')
    }
    formVisible.value = false
    await getList()
  } finally {
    submitLoading.value = false
  }
}

onMounted(getList)
</script>
