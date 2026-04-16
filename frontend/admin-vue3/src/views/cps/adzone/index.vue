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
          <el-option label="淘宝" value="taobao" />
          <el-option label="京东" value="jd" />
          <el-option label="拼多多" value="pdd" />
          <el-option label="抖音" value="douyin" />
        </el-select>
      </el-form-item>
      <el-form-item label="推广位名称" prop="adzoneName">
        <el-input
          v-model="queryParams.adzoneName"
          placeholder="请输入推广位名称"
          clearable
          class="!w-180px"
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
        <el-button type="primary" @click="openForm()" v-hasPermi="['cps:adzone:create']">
          <Icon icon="ep:plus" class="mr-5px" /> 新增推广位
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
      <el-table-column label="推广位ID" align="left" prop="adzoneId" min-width="180" show-overflow-tooltip />
      <el-table-column label="推广位名称" align="center" prop="adzoneName" width="140" show-overflow-tooltip>
        <template #default="scope">{{ scope.row.adzoneName || '-' }}</template>
      </el-table-column>
      <el-table-column label="类型" align="center" prop="adzoneType" width="110">
        <template #default="scope">
          <el-tag :type="adzoneTypeTagType(scope.row.adzoneType)" size="small">
            {{ adzoneTypeLabel(scope.row.adzoneType) }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="关联信息" align="center" prop="relationId" width="120" show-overflow-tooltip>
        <template #default="scope">
          <span v-if="scope.row.relationType === 'channel'">渠道 #{{ scope.row.relationId }}</span>
          <span v-else-if="scope.row.relationType === 'member'">用户 #{{ scope.row.relationId }}</span>
          <span v-else>-</span>
        </template>
      </el-table-column>
      <el-table-column label="是否默认" align="center" prop="isDefault" width="90">
        <template #default="scope">
          <el-tag :type="scope.row.isDefault === 1 ? 'success' : 'info'" size="small">
            {{ scope.row.isDefault === 1 ? '默认' : '非默认' }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="状态" align="center" prop="status" width="80">
        <template #default="scope">
          <el-tag :type="scope.row.status === 1 ? 'success' : 'danger'" size="small">
            {{ scope.row.status === 1 ? '启用' : '禁用' }}
          </el-tag>
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
            v-hasPermi="['cps:adzone:update']"
          >
            编辑
          </el-button>
          <el-button
            link
            type="danger"
            @click="handleDelete(scope.row.id)"
            v-hasPermi="['cps:adzone:delete']"
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
    :title="formData.id ? '编辑推广位' : '新增推广位'"
    width="500px"
    :close-on-click-modal="false"
    destroy-on-close
  >
    <el-form ref="formRef" :model="formData" :rules="formRules" label-width="100px">
      <el-form-item label="平台" prop="platformCode">
        <el-select v-model="formData.platformCode" placeholder="请选择平台" class="w-full">
          <el-option label="淘宝" value="taobao" />
          <el-option label="京东" value="jd" />
          <el-option label="拼多多" value="pdd" />
          <el-option label="抖音" value="douyin" />
        </el-select>
      </el-form-item>
      <el-form-item label="推广位ID" prop="adzoneId">
        <el-input v-model="formData.adzoneId" placeholder="请输入推广位ID（PID）" />
      </el-form-item>
      <el-form-item label="推广位名称" prop="adzoneName">
        <el-input v-model="formData.adzoneName" placeholder="请输入推广位名称（可选）" />
      </el-form-item>
      <el-form-item label="类型" prop="adzoneType">
        <el-select v-model="formData.adzoneType" placeholder="请选择推广位类型" class="w-full" @change="handleTypeChange">
          <el-option label="通用" value="general" />
          <el-option label="渠道专属" value="channel" />
          <el-option label="用户专属" value="member" />
        </el-select>
      </el-form-item>
      <el-form-item v-if="formData.adzoneType === 'channel'" label="关联渠道ID" prop="relationId">
        <el-input-number
          v-model="formData.relationId"
          :min="1"
          placeholder="请输入渠道ID"
          class="w-full"
          controls-position="right"
        />
      </el-form-item>
      <el-form-item v-if="formData.adzoneType === 'member'" label="关联用户" prop="relationId">
        <div class="flex items-center gap-2 w-full">
          <el-input
            :value="selectedMemberInfo"
            readonly
            placeholder="请点击右侧按钮选择用户"
            class="flex-1"
          />
          <el-button type="primary" @click="openMemberPicker">选择用户</el-button>
          <el-button v-if="formData.relationId" @click="clearMember">清除</el-button>
        </div>
      </el-form-item>
      <el-form-item label="是否默认" prop="isDefault">
        <el-switch
          v-model="formData.isDefault"
          :active-value="1"
          :inactive-value="0"
          active-text="默认推广位"
          inactive-text="非默认"
        />
      </el-form-item>
      <el-form-item label="状态" prop="status">
        <el-radio-group v-model="formData.status">
          <el-radio :label="1">启用</el-radio>
          <el-radio :label="0">禁用</el-radio>
        </el-radio-group>
      </el-form-item>
    </el-form>
    <template #footer>
      <el-button @click="dialogVisible = false">取 消</el-button>
      <el-button type="primary" :loading="formLoading" @click="handleSubmit">确 定</el-button>
    </template>
  </el-dialog>

  <!-- 用户选择弹窗 -->
  <el-dialog
    v-model="memberPickerVisible"
    title="选择会员用户"
    width="700px"
    :close-on-click-modal="false"
    append-to-body
  >
    <el-form :inline="true" class="mb-12px" @submit.prevent="searchMemberList">
      <el-form-item label="手机号">
        <el-input v-model="memberQuery.mobile" placeholder="请输入手机号" clearable class="!w-160px" />
      </el-form-item>
      <el-form-item label="昵称">
        <el-input v-model="memberQuery.nickname" placeholder="请输入昵称" clearable class="!w-160px" />
      </el-form-item>
      <el-form-item>
        <el-button type="primary" @click="searchMemberList">
          <Icon icon="ep:search" class="mr-5px" /> 搜索
        </el-button>
        <el-button @click="resetMemberQuery">重置</el-button>
      </el-form-item>
    </el-form>
    <el-table
      v-loading="memberListLoading"
      :data="memberList"
      stripe
      highlight-current-row
      @row-click="handleMemberSelect"
      style="cursor: pointer"
    >
      <el-table-column label="ID" prop="id" width="80" align="center" />
      <el-table-column label="头像" prop="avatar" width="70" align="center">
        <template #default="scope">
          <el-avatar :size="36" :src="scope.row.avatar" />
        </template>
      </el-table-column>
      <el-table-column label="手机号" prop="mobile" width="130" />
      <el-table-column label="昵称" prop="nickname" min-width="120" show-overflow-tooltip>
        <template #default="scope">{{ scope.row.nickname || '-' }}</template>
      </el-table-column>
      <el-table-column label="姓名" prop="name" width="100">
        <template #default="scope">{{ scope.row.name || '-' }}</template>
      </el-table-column>
      <el-table-column label="操作" width="80" align="center">
        <template #default="scope">
          <el-button type="primary" link @click.stop="handleMemberSelect(scope.row)">选择</el-button>
        </template>
      </el-table-column>
    </el-table>
    <Pagination
      :total="memberTotal"
      v-model:page="memberQuery.pageNo"
      v-model:limit="memberQuery.pageSize"
      @pagination="loadMemberList"
      class="mt-10px"
    />
  </el-dialog>
</template>

<script setup lang="ts">
import {
  CpsAdzoneApi,
  type CpsAdzoneVO,
  type CpsAdzoneSaveVO,
  type CpsAdzonePageReqVO
} from '@/api/cps/adzone'
import { getUserPage, type UserVO } from '@/api/member/user/index'
import { formatDate } from '@/utils/formatTime'
import { ElMessage, ElMessageBox } from 'element-plus'

defineOptions({ name: 'CpsAdzone' })

const loading = ref(false)
const list = ref<CpsAdzoneVO[]>([])
const total = ref(0)
const dialogVisible = ref(false)
const formLoading = ref(false)

const queryFormRef = ref()
const formRef = ref()

const queryParams = reactive<CpsAdzonePageReqVO>({
  pageNo: 1,
  pageSize: 10,
  platformCode: undefined,
  adzoneName: undefined,
  status: undefined
})

const defaultFormData = (): CpsAdzoneSaveVO => ({
  id: undefined,
  platformCode: '',
  adzoneId: '',
  adzoneName: undefined,
  adzoneType: undefined,
  relationType: undefined,
  relationId: undefined,
  isDefault: 0,
  status: 1
})

const formData = reactive<CpsAdzoneSaveVO>(defaultFormData())

const formRules = reactive({
  platformCode: [{ required: true, message: '平台不能为空', trigger: 'change' }],
  adzoneId: [{ required: true, message: '推广位ID不能为空', trigger: 'blur' }],
  status: [{ required: true, message: '状态不能为空', trigger: 'change' }],
  relationId: [{ required: true, message: '关联ID不能为空', trigger: 'blur', type: 'number' as const }]
})

/** 平台标签 */
const platformTagType = (code: string): 'danger' | 'primary' | 'warning' | 'info' | 'success' => {
  const map: Record<string, 'danger' | 'primary' | 'warning' | 'info' | 'success'> = { taobao: 'danger', jd: 'primary', pdd: 'warning', douyin: 'info' }
  return map[code] || 'info'
}
const platformLabel = (code: string) => {
  const map: Record<string, string> = { taobao: '淘宝', jd: '京东', pdd: '拼多多', douyin: '抖音' }
  return map[code] || code
}

/** 推广位类型标签 */
const adzoneTypeTagType = (type: string | undefined): 'danger' | 'primary' | 'warning' | 'info' | 'success' => {
  const map: Record<string, 'danger' | 'primary' | 'warning' | 'info' | 'success'> = { general: 'info', channel: 'warning', member: 'success' }
  return type ? (map[type] || 'info') : 'info'
}
const adzoneTypeLabel = (type: string | undefined) => {
  const map: Record<string, string> = { general: '通用', channel: '渠道专属', member: '用户专属' }
  return type ? (map[type] || type) : '-'
}

/** 类型变更时清空关联字段 */
const handleTypeChange = (val: string) => {
  if (val === 'general') {
    formData.relationType = undefined
    formData.relationId = undefined
    selectedMember.value = null
  } else {
    formData.relationType = val
  }
  if (val !== 'member') {
    selectedMember.value = null
  }
}

// ==================== 用户选择弹窗 ====================

const memberPickerVisible = ref(false)
const memberListLoading = ref(false)
const memberList = ref<UserVO[]>([])
const memberTotal = ref(0)
const selectedMember = ref<UserVO | null>(null)

const memberQuery = reactive({
  pageNo: 1,
  pageSize: 10,
  mobile: undefined as string | undefined,
  nickname: undefined as string | undefined
})

/** 已选会员显示信息 */
const selectedMemberInfo = computed(() => {
  if (!selectedMember.value) return ''
  const u = selectedMember.value
  const parts: string[] = []
  if (u.nickname) parts.push(u.nickname)
  if (u.name) parts.push(u.name)
  if (u.mobile) parts.push(u.mobile)
  return `ID:${u.id}  ${parts.join(' / ')}`
})

const openMemberPicker = async () => {
  memberPickerVisible.value = true
  await loadMemberList()
}

const loadMemberList = async () => {
  memberListLoading.value = true
  try {
    const data = await getUserPage(memberQuery)
    memberList.value = data.list
    memberTotal.value = data.total
  } finally {
    memberListLoading.value = false
  }
}

const searchMemberList = () => {
  memberQuery.pageNo = 1
  loadMemberList()
}

const resetMemberQuery = () => {
  memberQuery.mobile = undefined
  memberQuery.nickname = undefined
  memberQuery.pageNo = 1
  loadMemberList()
}

const handleMemberSelect = (row: UserVO) => {
  selectedMember.value = row
  formData.relationId = row.id
  formData.relationType = 'member'
  memberPickerVisible.value = false
}

const clearMember = () => {
  selectedMember.value = null
  formData.relationId = undefined
}

/** 查询列表 */
const getList = async () => {
  loading.value = true
  try {
    const data = await CpsAdzoneApi.getAdzonePage(queryParams)
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

/** 打开弹窗 */
const openForm = (row?: CpsAdzoneVO) => {
  Object.assign(formData, defaultFormData())
  if (row) {
    Object.assign(formData, row)
  }
  dialogVisible.value = true
}

/** 提交 */
const handleSubmit = async () => {
  await formRef.value?.validate()
  formLoading.value = true
  try {
    if (formData.id) {
      await CpsAdzoneApi.updateAdzone(formData)
      ElMessage.success('更新成功')
    } else {
      await CpsAdzoneApi.createAdzone(formData)
      ElMessage.success('创建成功')
    }
    dialogVisible.value = false
    getList()
  } finally {
    formLoading.value = false
  }
}

/** 删除 */
const handleDelete = async (id: number) => {
  await ElMessageBox.confirm('确定删除该推广位吗？', '提示', { type: 'warning' })
  await CpsAdzoneApi.deleteAdzone(id)
  ElMessage.success('删除成功')
  getList()
}

onMounted(getList)
</script>
