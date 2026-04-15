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
      <el-form-item label="平台名称" prop="platformName">
        <el-input
          v-model="queryParams.platformName"
          placeholder="请输入平台名称"
          clearable
          class="!w-200px"
          @keyup.enter="handleQuery"
        />
      </el-form-item>
      <el-form-item label="状态" prop="status">
        <el-select v-model="queryParams.status" placeholder="请选择状态" clearable class="!w-120px">
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
        <el-button type="primary" @click="openForm()" v-hasPermi="['cps:platform:create']">
          <Icon icon="ep:plus" class="mr-5px" /> 新增平台
        </el-button>
      </el-form-item>
    </el-form>
  </ContentWrap>

  <!-- 列表 -->
  <ContentWrap>
    <el-table v-loading="loading" :data="list" stripe>
      <el-table-column label="ID" align="center" prop="id" width="70" />
      <el-table-column label="平台" align="center" width="140">
        <template #default="scope">
          <div class="flex items-center gap-2 justify-center">
            <el-avatar
              v-if="scope.row.platformLogo"
              :src="scope.row.platformLogo"
              :size="28"
              shape="square"
            />
            <span>{{ scope.row.platformName }}</span>
          </div>
        </template>
      </el-table-column>
      <el-table-column label="平台编码" align="center" prop="platformCode" width="120" />
      <el-table-column label="默认推广位" align="center" prop="defaultAdzoneId" min-width="160" show-overflow-tooltip />
      <el-table-column label="服务费率" align="center" width="90">
        <template #default="scope">
          {{ scope.row.platformServiceRate != null ? scope.row.platformServiceRate + '%' : '-' }}
        </template>
      </el-table-column>
      <el-table-column label="排序" align="center" prop="sort" width="70" />
      <el-table-column label="激活供应商" align="center" width="120">
        <template #default="scope">
          <el-tag v-if="scope.row.activeVendorCode" type="success" size="small">
            {{ vendorLabel(scope.row.activeVendorCode) }}
          </el-tag>
          <span v-else class="text-gray-400">未设置</span>
        </template>
      </el-table-column>
      <el-table-column label="状态" align="center" prop="status" width="90">
        <template #default="scope">
          <el-switch
            v-model="scope.row.status"
            :active-value="1"
            :inactive-value="0"
            @change="handleStatusChange(scope.row)"
            v-hasPermi="['cps:platform:update']"
          />
        </template>
      </el-table-column>
      <el-table-column label="创建时间" align="center" prop="createTime" width="160">
        <template #default="scope">{{ formatDate(scope.row.createTime) }}</template>
      </el-table-column>
      <el-table-column label="操作" align="center" fixed="right" width="150">
        <template #default="scope">
          <el-button
            link
            type="primary"
            @click="openForm(scope.row)"
            v-hasPermi="['cps:platform:update']"
          >
            编辑
          </el-button>
          <el-button
            link
            type="danger"
            @click="handleDelete(scope.row.id)"
            v-hasPermi="['cps:platform:delete']"
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
    v-model="dialogVisible"
    :title="formData.id ? '编辑平台配置' : '新增平台配置'"
    width="600px"
    :close-on-click-modal="false"
    destroy-on-close
  >
    <el-form ref="formRef" :model="formData" :rules="formRules" label-width="120px">
      <el-row :gutter="16">
        <el-col :span="12">
          <el-form-item label="平台编码" prop="platformCode">
            <el-input
              v-model="formData.platformCode"
              placeholder="如: taobao"
              :disabled="!!formData.id"
            />
          </el-form-item>
        </el-col>
        <el-col :span="12">
          <el-form-item label="平台名称" prop="platformName">
            <el-input v-model="formData.platformName" placeholder="如: 淘宝联盟" />
          </el-form-item>
        </el-col>
        <el-col :span="24">
          <el-form-item label="平台Logo URL" prop="platformLogo">
            <el-input v-model="formData.platformLogo" placeholder="请输入Logo图片地址（可选）" />
          </el-form-item>
        </el-col>
        <el-col :span="24">
          <el-form-item label="默认推广位ID" prop="defaultAdzoneId">
            <el-input v-model="formData.defaultAdzoneId" placeholder="请输入默认推广位ID（可选）" />
          </el-form-item>
        </el-col>
        <el-col :span="12">
          <el-form-item label="服务费率(%)" prop="platformServiceRate">
            <el-input-number
              v-model="formData.platformServiceRate"
              :min="0"
              :max="100"
              :precision="2"
              :step="0.1"
              placeholder="如: 6.00"
              class="w-full"
            />
          </el-form-item>
        </el-col>
        <el-col :span="12">
          <el-form-item label="排序" prop="sort">
            <el-input-number v-model="formData.sort" :min="0" :max="9999" class="w-full" />
          </el-form-item>
        </el-col>
        <el-col :span="12">
          <el-form-item label="状态" prop="status">
            <el-radio-group v-model="formData.status">
              <el-radio :label="1">启用</el-radio>
              <el-radio :label="0">禁用</el-radio>
            </el-radio-group>
          </el-form-item>
        </el-col>
        <el-col :span="12">
          <el-form-item label="激活供应商" prop="activeVendorCode">
            <el-select
              v-model="formData.activeVendorCode"
              placeholder="请选择供应商"
              clearable
              class="w-full"
            >
              <el-option
                v-for="item in VENDOR_CODE_OPTIONS"
                :key="item.value"
                :label="item.label"
                :value="item.value"
              />
            </el-select>
          </el-form-item>
        </el-col>
        <el-col :span="24">
          <el-form-item label="扩展配置" prop="extraConfig">
            <el-input
              v-model="formData.extraConfig"
              type="textarea"
              :rows="3"
              placeholder="JSON 格式扩展配置（可选）"
            />
          </el-form-item>
        </el-col>
        <el-col :span="24">
          <el-form-item label="备注" prop="remark">
            <el-input
              v-model="formData.remark"
              type="textarea"
              :rows="2"
              placeholder="请输入备注（可选）"
            />
          </el-form-item>
        </el-col>
      </el-row>
    </el-form>
    <template #footer>
      <el-button @click="dialogVisible = false">取 消</el-button>
      <el-button type="primary" :loading="formLoading" @click="handleSubmit">确 定</el-button>
    </template>
  </el-dialog>
</template>

<script setup lang="ts">
import { CpsPlatformApi, type CpsPlatformVO, type CpsPlatformSaveVO, type CpsPlatformPageReqVO } from '@/api/cps/platform'
import { VENDOR_CODE_OPTIONS } from '@/api/cps/apiVendor'
import { formatDate } from '@/utils/formatTime'
import { ElMessage, ElMessageBox } from 'element-plus'

defineOptions({ name: 'CpsPlatform' })

const loading = ref(false)
const list = ref<CpsPlatformVO[]>([])
const total = ref(0)
const dialogVisible = ref(false)
const formLoading = ref(false)

const queryFormRef = ref()
const formRef = ref()

const queryParams = reactive<CpsPlatformPageReqVO>({
  pageNo: 1,
  pageSize: 10,
  platformName: undefined,
  status: undefined
})

const defaultFormData = (): CpsPlatformSaveVO => ({
  id: undefined,
  platformCode: '',
  platformName: '',
  platformLogo: undefined,
  defaultAdzoneId: undefined,
  platformServiceRate: undefined,
  sort: 0,
  status: 1,
  extraConfig: undefined,
  remark: undefined,
  activeVendorCode: undefined
})

const formData = reactive<CpsPlatformSaveVO>(defaultFormData())

const formRules = computed(() => ({
  platformCode: [{ required: true, message: '平台编码不能为空', trigger: 'blur' }],
  platformName: [{ required: true, message: '平台名称不能为空', trigger: 'blur' }],
  status: [{ required: true, message: '状态不能为空', trigger: 'change' }]
}))

/** 供应商名称文本 */
const vendorLabel = (code?: string) => {
  return VENDOR_CODE_OPTIONS.find((item) => item.value === code)?.label ?? code ?? '-'
}

/** 查询列表 */
const getList = async () => {
  loading.value = true
  try {
    const data = await CpsPlatformApi.getPlatformPage(queryParams)
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

/** 打开新增/编辑弹窗 */
const openForm = (row?: CpsPlatformVO) => {
  Object.assign(formData, defaultFormData())
  if (row) {
    Object.assign(formData, {
      id: row.id,
      platformCode: row.platformCode,
      platformName: row.platformName,
      platformLogo: row.platformLogo,
      defaultAdzoneId: row.defaultAdzoneId,
      platformServiceRate: row.platformServiceRate,
      sort: row.sort,
      status: row.status,
      extraConfig: row.extraConfig,
      remark: row.remark,
      activeVendorCode: row.activeVendorCode
    })
  }
  dialogVisible.value = true
}

/** 提交表单 */
const handleSubmit = async () => {
  await formRef.value?.validate()
  formLoading.value = true
  try {
    if (formData.id) {
      await CpsPlatformApi.updatePlatform(formData)
      ElMessage.success('更新成功')
    } else {
      await CpsPlatformApi.createPlatform(formData)
      ElMessage.success('创建成功')
    }
    dialogVisible.value = false
    getList()
  } finally {
    formLoading.value = false
  }
}

/** 状态快速切换 */
const handleStatusChange = async (row: CpsPlatformVO) => {
  const text = row.status === 1 ? '启用' : '禁用'
  try {
    await CpsPlatformApi.updatePlatform({
      id: row.id,
      platformCode: row.platformCode,
      platformName: row.platformName,
      defaultAdzoneId: row.defaultAdzoneId,
      status: row.status
    })
    ElMessage.success(`已${text}平台：${row.platformName}`)
  } catch {
    // 还原状态
    row.status = row.status === 1 ? 0 : 1
  }
}

/** 删除 */
const handleDelete = async (id: number) => {
  await ElMessageBox.confirm('确定删除该平台配置吗？删除后不可恢复！', '警告', { type: 'warning' })
  await CpsPlatformApi.deletePlatform(id)
  ElMessage.success('删除成功')
  getList()
}

onMounted(getList)
</script>
