<template>
  <ContentWrap>
    <!-- 搜索栏 -->
    <el-form
      class="-mb-15px"
      :model="queryParams"
      ref="queryFormRef"
      :inline="true"
      label-width="90px"
    >
      <el-form-item label="平台" prop="platformCode">
        <el-select
          v-model="queryParams.platformCode"
          placeholder="请选择平台"
          clearable
          class="!w-160px"
        >
          <el-option label="淘宝" value="taobao" />
          <el-option label="京东" value="jd" />
          <el-option label="拼多多" value="pdd" />
          <el-option label="抖音" value="douyin" />
        </el-select>
      </el-form-item>
      <el-form-item label="会员ID" prop="memberId">
        <el-input
          v-model.number="queryParams.memberId"
          placeholder="请输入会员ID"
          clearable
          class="!w-160px"
          @keyup.enter="handleQuery"
        />
      </el-form-item>
      <el-form-item label="商品标题" prop="itemTitle">
        <el-input
          v-model="queryParams.itemTitle"
          placeholder="请输入商品关键词"
          clearable
          class="!w-200px"
          @keyup.enter="handleQuery"
        />
      </el-form-item>
      <el-form-item label="状态" prop="status">
        <el-select
          v-model="queryParams.status"
          placeholder="请选择状态"
          clearable
          class="!w-120px"
        >
          <el-option label="有效" :value="1" />
          <el-option label="无效" :value="0" />
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
        <el-button @click="handleQuery">
          <Icon icon="ep:search" class="mr-5px" /> 搜索
        </el-button>
        <el-button @click="resetQuery">
          <Icon icon="ep:refresh" class="mr-5px" /> 重置
        </el-button>
      </el-form-item>
    </el-form>
  </ContentWrap>

  <!-- 列表 -->
  <ContentWrap>
    <el-table v-loading="loading" :data="list">
      <el-table-column label="ID" align="center" prop="id" width="80" />
      <el-table-column label="会员ID" align="center" prop="memberId" width="100" />
      <el-table-column label="平台" align="center" prop="platformCode" width="100">
        <template #default="scope">
          <el-tag>{{ scope.row.platformCode }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="商品标题" align="center" prop="itemTitle" min-width="180" show-overflow-tooltip />
      <el-table-column label="推广链接" align="center" prop="promotionUrl" min-width="200" show-overflow-tooltip>
        <template #default="scope">
          <a v-if="scope.row.promotionUrl" :href="scope.row.promotionUrl" target="_blank" class="text-blue-500">
            查看链接
          </a>
          <span v-else>-</span>
        </template>
      </el-table-column>
      <el-table-column label="淘口令" align="center" prop="taoCommand" width="120" show-overflow-tooltip />
      <el-table-column label="关联订单号" align="center" prop="platformOrderId" width="160" show-overflow-tooltip />
      <el-table-column label="失效时间" align="center" prop="expireTime" width="160">
        <template #default="scope">
          <span>{{ scope.row.expireTime ? formatDate(scope.row.expireTime) : '-' }}</span>
        </template>
      </el-table-column>
      <el-table-column label="状态" align="center" prop="status" width="80">
        <template #default="scope">
          <el-tag :type="scope.row.status === 1 ? 'success' : 'danger'">
            {{ scope.row.status === 1 ? '有效' : '无效' }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="创建时间" align="center" prop="createTime" width="160">
        <template #default="scope">
          <span>{{ formatDate(scope.row.createTime) }}</span>
        </template>
      </el-table-column>
    </el-table>
    <!-- 分页 -->
    <Pagination
      :total="total"
      v-model:page="queryParams.pageNo"
      v-model:limit="queryParams.pageSize"
      @pagination="getList"
    />
  </ContentWrap>
</template>

<script setup lang="ts">
import { getCpsTransferRecordPage, type CpsTransferRecordPageReqVO } from '@/api/cps/transfer'
import { formatDate } from '@/utils/formatTime'

defineOptions({ name: 'CpsTransferRecord' })

const loading = ref(true)
const list = ref([])
const total = ref(0)

const queryParams = reactive<CpsTransferRecordPageReqVO>({
  pageNo: 1,
  pageSize: 10,
  memberId: undefined,
  platformCode: undefined,
  itemTitle: undefined,
  status: undefined,
  createTime: undefined
})

const queryFormRef = ref()

/** 查询列表 */
const getList = async () => {
  loading.value = true
  try {
    const data = await getCpsTransferRecordPage(queryParams)
    list.value = data.list
    total.value = data.total
  } finally {
    loading.value = false
  }
}

/** 搜索按钮 */
const handleQuery = () => {
  queryParams.pageNo = 1
  getList()
}

/** 重置按钮 */
const resetQuery = () => {
  queryFormRef.value?.resetFields()
  handleQuery()
}

onMounted(() => {
  getList()
})
</script>
