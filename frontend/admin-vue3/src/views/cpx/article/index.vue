<template>
  <ContentWrap>
    <div class="page-toolbar">
      <div>
        <div class="text-18px font-600">CPX 资讯中心</div>
        <div class="mt-4px text-12px text-gray-500">
          发布平台接入说明、任务攻略、玩法教程和行业资讯，服务 CPS 主线和多计费扩展
        </div>
      </div>
      <el-button type="primary" @click="openForm()">
        <Icon icon="ep:plus" class="mr-5px" /> 发布资讯
      </el-button>
    </div>

    <el-form :model="queryParams" class="mt-18px" label-width="84px" inline>
      <el-form-item label="关键词">
        <el-input
          v-model="queryParams.keyword"
          placeholder="标题/摘要/标签"
          clearable
          class="!w-220px"
          @keyup.enter="handleQuery"
        />
      </el-form-item>
      <el-form-item label="分类">
        <el-input
          v-model="queryParams.category"
          placeholder="攻略 / 教程 / 资讯"
          clearable
          class="!w-180px"
          @keyup.enter="handleQuery"
        />
      </el-form-item>
      <el-form-item label="计费方式">
        <el-select v-model="queryParams.promotionMethod" clearable class="!w-180px">
          <el-option
            v-for="item in CPX_PROMOTION_METHOD_OPTIONS"
            :key="item.value"
            :label="item.label"
            :value="item.value"
          />
        </el-select>
      </el-form-item>
      <el-form-item>
        <el-button type="primary" :loading="loading" @click="handleQuery">
          <Icon icon="ep:search" class="mr-5px" /> 查询
        </el-button>
        <el-button @click="resetQuery">
          <Icon icon="ep:refresh" class="mr-5px" /> 重置
        </el-button>
      </el-form-item>
    </el-form>
  </ContentWrap>

  <ContentWrap>
    <el-table v-loading="loading" :data="list" stripe>
      <el-table-column label="标题" min-width="220">
        <template #default="scope">
          <div class="font-600">{{ scope.row.title }}</div>
          <div class="text-xs text-gray-500">{{ scope.row.summary || '-' }}</div>
        </template>
      </el-table-column>
      <el-table-column label="分类" prop="category" width="120" />
      <el-table-column label="平台" prop="platformCode" width="120">
        <template #default="scope">
          <el-tag type="info" effect="plain">{{ scope.row.platformCode || '全平台' }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="计费方式" width="120">
        <template #default="scope">
          <el-tag>{{ promotionMethodLabel(scope.row.promotionMethod) }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="发布时间" width="170">
        <template #default="scope">
          {{ formatTime(scope.row.publishTime) }}
        </template>
      </el-table-column>
      <el-table-column label="状态" width="90">
        <template #default="scope">
          <el-tag :type="scope.row.status === 1 ? 'success' : 'info'">
            {{ scope.row.status === 1 ? '已发布' : '草稿' }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="标签" min-width="180">
        <template #default="scope">
          {{ scope.row.tags || '-' }}
        </template>
      </el-table-column>
      <el-table-column label="操作" width="100" fixed="right">
        <template #default="scope">
          <el-button link type="primary" @click="openForm(scope.row)">编辑</el-button>
        </template>
      </el-table-column>
    </el-table>
  </ContentWrap>

  <el-dialog v-model="dialogVisible" :title="formData.id ? '编辑资讯' : '发布资讯'" width="860px">
    <el-form ref="formRef" :model="formData" :rules="formRules" label-width="110px">
      <el-row :gutter="16">
        <el-col :xs="24" :sm="12">
          <el-form-item label="标题" prop="title">
            <el-input v-model="formData.title" placeholder="请输入资讯标题" />
          </el-form-item>
        </el-col>
        <el-col :xs="24" :sm="12">
          <el-form-item label="分类">
            <el-input v-model="formData.category" placeholder="攻略 / 教程 / 资讯" />
          </el-form-item>
        </el-col>
        <el-col :xs="24" :sm="12">
          <el-form-item label="平台编码">
            <el-input v-model="formData.platformCode" placeholder="全平台可留空" />
          </el-form-item>
        </el-col>
        <el-col :xs="24" :sm="12">
          <el-form-item label="计费方式">
            <el-select v-model="formData.promotionMethod" clearable class="w-full">
              <el-option
                v-for="item in CPX_PROMOTION_METHOD_OPTIONS"
                :key="item.value"
                :label="item.label"
                :value="item.value"
              />
            </el-select>
          </el-form-item>
        </el-col>
        <el-col :xs="24" :sm="12">
          <el-form-item label="状态">
            <el-radio-group v-model="formData.status">
              <el-radio :label="0">草稿</el-radio>
              <el-radio :label="1">发布</el-radio>
            </el-radio-group>
          </el-form-item>
        </el-col>
        <el-col :xs="24" :sm="12">
          <el-form-item label="封面图">
            <el-input v-model="formData.coverUrl" placeholder="https://..." />
          </el-form-item>
        </el-col>
        <el-col :span="24">
          <el-form-item label="摘要">
            <el-input v-model="formData.summary" maxlength="180" show-word-limit />
          </el-form-item>
        </el-col>
        <el-col :span="24">
          <el-form-item label="正文内容">
            <el-input v-model="formData.content" type="textarea" :rows="8" placeholder="可填 Markdown 或纯文本内容" />
          </el-form-item>
        </el-col>
        <el-col :span="24">
          <el-form-item label="关联任务">
            <el-input v-model.number="formData.relatedTaskId" placeholder="可选任务 ID" />
          </el-form-item>
        </el-col>
        <el-col :span="24">
          <el-form-item label="标签">
            <el-input v-model="formData.tags" placeholder="多个标签用逗号分隔" />
          </el-form-item>
        </el-col>
        <el-col :span="24">
          <el-form-item label="发布时间">
            <el-date-picker
              v-model="formData.publishTime"
              type="datetime"
              value-format="YYYY-MM-DDTHH:mm:ss"
              class="w-full"
            />
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
import {
  CpxTaskApi,
  CPX_PROMOTION_METHOD_OPTIONS,
  type CpxArticleSaveVO,
  type CpxPromotionMethod
} from '@/api/cpx/task'
import { ElMessage } from 'element-plus'

defineOptions({ name: 'CpxArticleIndex' })

const loading = ref(false)
const submitLoading = ref(false)
const list = ref<any[]>([])
const dialogVisible = ref(false)
const formRef = ref()

const queryParams = reactive({
  keyword: '',
  category: '',
  promotionMethod: '' as CpxPromotionMethod | ''
})

const formData = reactive<Partial<CpxArticleSaveVO>>({
  title: '',
  category: '',
  summary: '',
  coverUrl: '',
  content: '',
  platformCode: '',
  promotionMethod: 'CPS',
  relatedTaskId: undefined,
  tags: '',
  status: 1,
  publishTime: new Date().toISOString().slice(0, 19)
})

const formRules = {
  title: [{ required: true, message: '请输入标题', trigger: 'blur' }]
}

const getList = async () => {
  loading.value = true
  try {
    list.value = await CpxTaskApi.listArticles({
      keyword: queryParams.keyword,
      category: queryParams.category,
      promotionMethod: queryParams.promotionMethod || undefined
    })
  } finally {
    loading.value = false
  }
}

const handleQuery = () => getList()

const resetQuery = () => {
  queryParams.keyword = ''
  queryParams.category = ''
  queryParams.promotionMethod = ''
  getList()
}

const openForm = (row?: any) => {
  if (row) {
    Object.assign(formData, row)
  } else {
    Object.assign(formData, {
      id: undefined,
      title: '',
      category: '',
      summary: '',
      coverUrl: '',
      content: '',
      platformCode: '',
      promotionMethod: 'CPS',
      relatedTaskId: undefined,
      tags: '',
      status: 1,
      publishTime: new Date().toISOString().slice(0, 19)
    })
  }
  dialogVisible.value = true
}

const handleSubmit = async () => {
  await formRef.value?.validate()
  submitLoading.value = true
  try {
    if (formData.id) {
      await CpxTaskApi.updateArticle(formData as CpxArticleSaveVO)
      ElMessage.success('资讯已更新')
    } else {
      await CpxTaskApi.createArticle(formData as CpxArticleSaveVO)
      ElMessage.success('资讯已发布')
    }
    dialogVisible.value = false
    getList()
  } finally {
    submitLoading.value = false
  }
}

const promotionMethodLabel = (value?: string) =>
  CPX_PROMOTION_METHOD_OPTIONS.find((item) => item.value === value)?.label ?? value ?? '-'

const formatTime = (value?: string | Date) => value ? String(value).replace('T', ' ').slice(0, 16) : '-'

onMounted(getList)
</script>
