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
      <el-form-item label="会员ID" prop="memberId">
        <el-input
          v-model.number="queryParams.memberId"
          placeholder="请输入会员ID"
          clearable
          class="!w-160px"
          @keyup.enter="handleQuery"
        />
      </el-form-item>
      <el-form-item label="状态" prop="status">
        <el-select v-model="queryParams.status" placeholder="请选择状态" clearable class="!w-140px">
          <el-option label="待冻结" value="pending" />
          <el-option label="已冻结" value="frozen" />
          <el-option label="解冻中" value="unfreezing" />
          <el-option label="已解冻" value="unfreezed" />
        </el-select>
      </el-form-item>
      <el-form-item label="创建时间" prop="createTime">
        <el-date-picker
          v-model="queryParams.createTime"
          value-format="YYYY-MM-DD HH:mm:ss"
          type="daterange"
          start-placeholder="开始日期"
          end-placeholder="结束日期"
          :default-time="[new Date('1 00:00:00'), new Date('1 23:59:59')]"
          class="!w-240px"
        />
      </el-form-item>
      <el-form-item>
        <el-button @click="handleQuery"><Icon icon="ep:search" class="mr-5px" /> 搜索</el-button>
        <el-button @click="resetQuery"><Icon icon="ep:refresh" class="mr-5px" /> 重置</el-button>
      </el-form-item>
    </el-form>
  </ContentWrap>

  <!-- 列表 -->
  <ContentWrap>
    <el-table v-loading="loading" :data="list">
      <el-table-column label="ID" align="center" prop="id" width="80" />
      <el-table-column label="会员ID" align="center" prop="memberId" width="100" />
      <el-table-column label="订单ID" align="center" prop="orderId" width="100" />
      <el-table-column label="平台订单号" align="center" prop="platformOrderId" width="160" show-overflow-tooltip />
      <el-table-column label="冻结金额" align="center" prop="freezeAmount" width="120">
        <template #default="scope">
          <span class="text-orange-500 font-bold">￥{{ scope.row.freezeAmount }}</span>
        </template>
      </el-table-column>
      <el-table-column label="计划解冻时间" align="center" prop="unfreezeTime" width="160">
        <template #default="scope">{{ scope.row.unfreezeTime ? formatDate(scope.row.unfreezeTime) : '-' }}</template>
      </el-table-column>
      <el-table-column label="实际解冻时间" align="center" prop="actualUnfreezeTime" width="160">
        <template #default="scope">{{ scope.row.actualUnfreezeTime ? formatDate(scope.row.actualUnfreezeTime) : '-' }}</template>
      </el-table-column>
      <el-table-column label="状态" align="center" prop="status" width="100">
        <template #default="scope">
          <el-tag :type="getStatusTagType(scope.row.status)">{{ getStatusLabel(scope.row.status) }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="创建时间" align="center" prop="createTime" width="160">
        <template #default="scope">{{ formatDate(scope.row.createTime) }}</template>
      </el-table-column>
      <el-table-column label="操作" align="center" width="120">
        <template #default="scope">
          <el-button
            v-if="scope.row.status === 'frozen'"
            link
            type="primary"
            @click="handleManualUnfreeze(scope.row.id)"
          >
            手动解冻
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
</template>

<script setup lang="ts">
import {
  getCpsFreezeRecordPage,
  manualUnfreeze,
  type CpsFreezeRecordPageReqVO
} from '@/api/cps/freeze'
import { formatDate } from '@/utils/formatTime'
import { ElMessage, ElMessageBox } from 'element-plus'

defineOptions({ name: 'CpsFreezeRecord' })

const loading = ref(true)
const list = ref([])
const total = ref(0)

const queryParams = reactive<CpsFreezeRecordPageReqVO>({
  pageNo: 1,
  pageSize: 10,
  memberId: undefined,
  status: undefined,
  createTime: undefined
})

const queryFormRef = ref()

const statusMap: Record<string, { label: string; type: string }> = {
  pending: { label: '待冻结', type: 'info' },
  frozen: { label: '已冻结', type: 'warning' },
  unfreezing: { label: '解冻中', type: '' },
  unfreezed: { label: '已解冻', type: 'success' }
}

const getStatusLabel = (status: string) => statusMap[status]?.label ?? status
const getStatusTagType = (status: string) => statusMap[status]?.type ?? ''

const getList = async () => {
  loading.value = true
  try {
    const data = await getCpsFreezeRecordPage(queryParams)
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

const handleManualUnfreeze = async (id: number) => {
  await ElMessageBox.confirm('确定手动解冻该记录吗？解冻后将释放对应的返利余额。', '手动解冻', {
    type: 'warning',
    confirmButtonText: '确定解冻'
  })
  await manualUnfreeze(id)
  ElMessage.success('解冻成功')
  getList()
}

onMounted(() => {
  getList()
})
</script>
