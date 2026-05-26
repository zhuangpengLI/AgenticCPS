<template>
  <ContentWrap>
    <div class="page-toolbar">
      <div>
        <div class="text-18px font-600">平台对接中心</div>
        <div class="mt-4px text-12px text-gray-500">
          维护平台档案、支持的计费方式、回调地址和导入模板，供任务和资讯复用
        </div>
      </div>
      <el-button type="primary" @click="openForm()">
        <Icon icon="ep:plus" class="mr-5px" /> 新建平台档案
      </el-button>
    </div>

    <div class="mt-18px grid gap-12px sm:grid-cols-2 xl:grid-cols-3">
      <div v-for="item in list" :key="item.id" class="rounded-8px border border-gray-200 p-16px">
        <div class="flex items-center justify-between gap-8px">
          <div class="flex items-center gap-10px min-w-0">
            <el-avatar :src="item.platformLogo" shape="square" :size="32">
              {{ item.platformName?.slice(0, 1) }}
            </el-avatar>
            <div class="min-w-0">
              <div class="font-600 truncate">{{ item.platformName }}</div>
              <div class="text-xs text-gray-500">{{ item.platformCode }}</div>
            </div>
          </div>
          <el-tag :type="item.status === 1 ? 'success' : 'info'" effect="plain">
            {{ item.status === 1 ? '启用' : '禁用' }}
          </el-tag>
        </div>
        <div class="mt-12px text-sm text-gray-700">
          <div>支持模型：{{ item.supportedMethods || '未配置' }}</div>
          <div class="mt-4px">健康状态：{{ item.healthStatus || 'unknown' }}</div>
          <div class="mt-4px truncate">API：{{ item.apiBaseUrl || '-' }}</div>
          <div class="mt-4px truncate">回调：{{ item.callbackUrl || '-' }}</div>
        </div>
        <div class="mt-12px flex items-center justify-between">
          <span class="text-xs text-gray-400">{{ item.importTemplate || '暂无导入模板' }}</span>
          <div>
            <el-button link type="primary" @click="copyPlatformCode(item.platformCode)">
              复制编码
            </el-button>
            <el-button link type="primary" @click="openForm(item)">编辑</el-button>
          </div>
        </div>
      </div>
    </div>
  </ContentWrap>

  <el-dialog v-model="dialogVisible" :title="formData.id ? '编辑平台档案' : '新建平台档案'" width="760px">
    <el-form ref="formRef" :model="formData" :rules="formRules" label-width="110px">
      <el-row :gutter="16">
        <el-col :xs="24" :sm="12">
          <el-form-item label="平台编码" prop="platformCode">
            <el-input v-model="formData.platformCode" placeholder="taobao / jd / pdd / douyin" />
          </el-form-item>
        </el-col>
        <el-col :xs="24" :sm="12">
          <el-form-item label="平台名称" prop="platformName">
            <el-input v-model="formData.platformName" placeholder="请输入平台名称" />
          </el-form-item>
        </el-col>
        <el-col :span="24">
          <el-form-item label="平台 Logo">
            <el-input v-model="formData.platformLogo" placeholder="https://..." />
          </el-form-item>
        </el-col>
        <el-col :span="24">
          <el-form-item label="支持方式">
            <el-input v-model="formData.supportedMethods" placeholder="CPS,CPA,CPL,CPM,CPC,OCPA,OCPC" />
          </el-form-item>
        </el-col>
        <el-col :span="24">
          <el-form-item label="API 地址">
            <el-input v-model="formData.apiBaseUrl" placeholder="https://openapi.example.com" />
          </el-form-item>
        </el-col>
        <el-col :span="24">
          <el-form-item label="回调地址">
            <el-input v-model="formData.callbackUrl" placeholder="https://.../callback" />
          </el-form-item>
        </el-col>
        <el-col :span="24">
          <el-form-item label="导入模板">
            <el-input v-model="formData.importTemplate" placeholder="CSV / JSON / Excel 模板说明" />
          </el-form-item>
        </el-col>
        <el-col :xs="24" :sm="12">
          <el-form-item label="健康状态">
            <el-input v-model="formData.healthStatus" placeholder="healthy / degraded / offline" />
          </el-form-item>
        </el-col>
        <el-col :xs="24" :sm="12">
          <el-form-item label="状态">
            <el-radio-group v-model="formData.status">
              <el-radio :label="1">启用</el-radio>
              <el-radio :label="0">禁用</el-radio>
            </el-radio-group>
          </el-form-item>
        </el-col>
        <el-col :span="24">
          <el-form-item label="备注">
            <el-input v-model="formData.remark" type="textarea" :rows="3" />
          </el-form-item>
        </el-col>
        <el-col :span="24">
          <el-form-item label="扩展配置">
            <el-input v-model="formData.extraConfig" type="textarea" :rows="4" placeholder="JSON 配置" />
          </el-form-item>
        </el-col>
      </el-row>
    </el-form>
    <template #footer>
      <el-button @click="dialogVisible = false">取消</el-button>
      <el-button type="primary" :loading="submitLoading" @click="handleSubmit">确定</el-button>
    </template>
  </el-dialog>
</template>

<script setup lang="ts">
import { CpxTaskApi, type CpxPlatformProfileSaveVO } from '@/api/cpx/task'
import { ElMessage } from 'element-plus'

defineOptions({ name: 'CpxPlatformProfileIndex' })

const loading = ref(false)
const submitLoading = ref(false)
const list = ref<any[]>([])
const dialogVisible = ref(false)
const formRef = ref()

const formData = reactive<Partial<CpxPlatformProfileSaveVO>>({
  platformCode: '',
  platformName: '',
  platformLogo: '',
  supportedMethods: 'CPS,CPA,CPL,CPM,CPC,OCPA,OCPC',
  apiBaseUrl: '',
  callbackUrl: '',
  importTemplate: '',
  healthStatus: 'healthy',
  status: 1,
  remark: '',
  extraConfig: ''
})

const formRules = {
  platformCode: [{ required: true, message: '请输入平台编码', trigger: 'blur' }],
  platformName: [{ required: true, message: '请输入平台名称', trigger: 'blur' }]
}

const getList = async () => {
  loading.value = true
  try {
    list.value = await CpxTaskApi.listPlatformProfiles()
  } finally {
    loading.value = false
  }
}

const openForm = (row?: any) => {
  if (row) {
    Object.assign(formData, row)
  } else {
    Object.assign(formData, {
      id: undefined,
      platformCode: '',
      platformName: '',
      platformLogo: '',
      supportedMethods: 'CPS,CPA,CPL,CPM,CPC,OCPA,OCPC',
      apiBaseUrl: '',
      callbackUrl: '',
      importTemplate: '',
      healthStatus: 'healthy',
      status: 1,
      remark: '',
      extraConfig: ''
    })
  }
  dialogVisible.value = true
}

const handleSubmit = async () => {
  await formRef.value?.validate()
  submitLoading.value = true
  try {
    if (formData.id) {
      await CpxTaskApi.updatePlatformProfile(formData as CpxPlatformProfileSaveVO)
      ElMessage.success('平台档案已更新')
    } else {
      await CpxTaskApi.createPlatformProfile(formData as CpxPlatformProfileSaveVO)
      ElMessage.success('平台档案已创建')
    }
    dialogVisible.value = false
    getList()
  } finally {
    submitLoading.value = false
  }
}

const copyPlatformCode = async (value?: string) => {
  if (!value) {
    return
  }
  await navigator.clipboard.writeText(value)
  ElMessage.success('已复制平台编码')
}

onMounted(getList)
</script>
