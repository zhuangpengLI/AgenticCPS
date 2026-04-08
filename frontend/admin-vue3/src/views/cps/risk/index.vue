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
      <el-form-item label="规则类型" prop="ruleType">
        <el-select v-model="queryParams.ruleType" placeholder="请选择类型" clearable class="!w-160px">
          <el-option label="频率限制" value="rate_limit" />
          <el-option label="黑名单" value="blacklist" />
        </el-select>
      </el-form-item>
      <el-form-item label="状态" prop="status">
        <el-select v-model="queryParams.status" placeholder="请选择状态" clearable class="!w-120px">
          <el-option label="启用" :value="1" />
          <el-option label="禁用" :value="0" />
        </el-select>
      </el-form-item>
      <el-form-item>
        <el-button @click="handleQuery"><Icon icon="ep:search" class="mr-5px" /> 搜索</el-button>
        <el-button @click="resetQuery"><Icon icon="ep:refresh" class="mr-5px" /> 重置</el-button>
        <el-button type="primary" @click="openForm(undefined)">
          <Icon icon="ep:plus" class="mr-5px" /> 新增规则
        </el-button>
      </el-form-item>
    </el-form>
  </ContentWrap>

  <!-- 列表 -->
  <ContentWrap>
    <el-alert
      title="风控说明：频率限制规则对所有会员生效；黑名单规则通过目标值（会员ID或IP）精确拦截。"
      type="info"
      show-icon
      :closable="false"
      class="mb-15px"
    />
    <el-table v-loading="loading" :data="list">
      <el-table-column label="ID" align="center" prop="id" width="80" />
      <el-table-column label="规则类型" align="center" prop="ruleType" width="120">
        <template #default="scope">
          <el-tag :type="scope.row.ruleType === 'rate_limit' ? 'primary' : 'danger'">
            {{ scope.row.ruleType === 'rate_limit' ? '频率限制' : '黑名单' }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="目标类型" align="center" prop="targetType" width="100">
        <template #default="scope">
          {{ scope.row.targetType === 'member' ? '会员' : 'IP' }}
        </template>
      </el-table-column>
      <el-table-column label="目标值" align="center" prop="targetValue" width="160">
        <template #default="scope">
          <span>{{ scope.row.targetValue || '全量（不限定目标）' }}</span>
        </template>
      </el-table-column>
      <el-table-column label="限制次数/天" align="center" prop="limitCount" width="120">
        <template #default="scope">
          <span v-if="scope.row.limitCount">{{ scope.row.limitCount }} 次</span>
          <span v-else>-</span>
        </template>
      </el-table-column>
      <el-table-column label="状态" align="center" prop="status" width="80">
        <template #default="scope">
          <el-switch
            v-model="scope.row.status"
            :active-value="1"
            :inactive-value="0"
            @change="handleStatusChange(scope.row)"
          />
        </template>
      </el-table-column>
      <el-table-column label="备注" align="center" prop="remark" min-width="160" show-overflow-tooltip />
      <el-table-column label="创建时间" align="center" prop="createTime" width="160">
        <template #default="scope">{{ formatDate(scope.row.createTime) }}</template>
      </el-table-column>
      <el-table-column label="操作" align="center" width="140">
        <template #default="scope">
          <el-button link type="primary" @click="openForm(scope.row)">编辑</el-button>
          <el-button link type="danger" @click="handleDelete(scope.row.id)">删除</el-button>
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
    v-model="dialogVisible"
    :title="formData.id ? '编辑风控规则' : '新增风控规则'"
    width="520px"
    :close-on-click-modal="false"
  >
    <el-form ref="formRef" :model="formData" :rules="formRules" label-width="110px">
      <el-form-item label="规则类型" prop="ruleType">
        <el-radio-group v-model="formData.ruleType">
          <el-radio label="rate_limit">频率限制</el-radio>
          <el-radio label="blacklist">黑名单</el-radio>
        </el-radio-group>
      </el-form-item>
      <el-form-item label="目标类型" prop="targetType">
        <el-radio-group v-model="formData.targetType">
          <el-radio label="member">会员</el-radio>
          <el-radio label="ip">IP地址</el-radio>
        </el-radio-group>
      </el-form-item>
      <el-form-item
        v-if="formData.ruleType === 'blacklist'"
        label="目标值"
        prop="targetValue"
      >
        <el-input
          v-model="formData.targetValue"
          :placeholder="formData.targetType === 'member' ? '请输入会员ID' : '请输入IP地址'"
          clearable
        />
      </el-form-item>
      <el-form-item
        v-if="formData.ruleType === 'rate_limit'"
        label="每日限制次数"
        prop="limitCount"
      >
        <el-input-number v-model="formData.limitCount" :min="1" :max="99999" />
        <span class="ml-10px text-gray-500">次/天</span>
      </el-form-item>
      <el-form-item label="状态" prop="status">
        <el-radio-group v-model="formData.status">
          <el-radio :label="1">启用</el-radio>
          <el-radio :label="0">禁用</el-radio>
        </el-radio-group>
      </el-form-item>
      <el-form-item label="备注" prop="remark">
        <el-input v-model="formData.remark" type="textarea" :rows="2" placeholder="请输入备注" />
      </el-form-item>
    </el-form>
    <template #footer>
      <el-button @click="dialogVisible = false">取消</el-button>
      <el-button type="primary" :loading="formLoading" @click="handleSubmit">确定</el-button>
    </template>
  </el-dialog>
</template>

<script setup lang="ts">
import {
  getCpsRiskRulePage,
  createCpsRiskRule,
  updateCpsRiskRule,
  deleteCpsRiskRule,
  type CpsRiskRulePageReqVO,
  type CpsRiskRuleSaveVO
} from '@/api/cps/risk'
import { formatDate } from '@/utils/formatTime'
import { ElMessage, ElMessageBox } from 'element-plus'

defineOptions({ name: 'CpsRiskRule' })

const loading = ref(true)
const list = ref([])
const total = ref(0)
const dialogVisible = ref(false)
const formLoading = ref(false)

const queryParams = reactive<CpsRiskRulePageReqVO>({
  pageNo: 1,
  pageSize: 10,
  ruleType: undefined,
  status: undefined
})

const formData = reactive<CpsRiskRuleSaveVO>({
  id: undefined,
  ruleType: 'rate_limit',
  targetType: 'member',
  targetValue: undefined,
  limitCount: 100,
  status: 1,
  remark: undefined
})

const formRules = {
  ruleType: [{ required: true, message: '规则类型不能为空', trigger: 'change' }],
  targetType: [{ required: true, message: '目标类型不能为空', trigger: 'change' }],
  status: [{ required: true, message: '状态不能为空', trigger: 'change' }]
}

const queryFormRef = ref()
const formRef = ref()

const getList = async () => {
  loading.value = true
  try {
    const data = await getCpsRiskRulePage(queryParams)
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
  queryFormRef.value?.resetFields()
  handleQuery()
}

const openForm = (row: any) => {
  if (row) {
    Object.assign(formData, row)
  } else {
    Object.assign(formData, {
      id: undefined,
      ruleType: 'rate_limit',
      targetType: 'member',
      targetValue: undefined,
      limitCount: 100,
      status: 1,
      remark: undefined
    })
  }
  dialogVisible.value = true
}

const handleSubmit = async () => {
  await formRef.value?.validate()
  formLoading.value = true
  try {
    if (formData.id) {
      await updateCpsRiskRule(formData)
      ElMessage.success('更新成功')
    } else {
      await createCpsRiskRule(formData)
      ElMessage.success('创建成功')
    }
    dialogVisible.value = false
    getList()
  } finally {
    formLoading.value = false
  }
}

const handleStatusChange = async (row: any) => {
  await updateCpsRiskRule({ id: row.id, ruleType: row.ruleType, targetType: row.targetType, status: row.status })
  ElMessage.success(row.status === 1 ? '已启用' : '已禁用')
}

const handleDelete = async (id: number) => {
  await ElMessageBox.confirm('确定删除该风控规则吗？', '提示', { type: 'warning' })
  await deleteCpsRiskRule(id)
  ElMessage.success('删除成功')
  getList()
}

onMounted(() => {
  getList()
})
</script>
