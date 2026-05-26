<template>
  <div class="tool-panel">
    <el-alert
      class="mb-14px"
      type="info"
      show-icon
      :closable="false"
      title="当前提供淘礼金计划、活动模板和补贴预算测算，不调用真实发放接口。"
    />

    <el-form ref="formRef" :model="formData" :rules="formRules" label-width="96px">
      <el-row :gutter="12">
        <el-col :xs="24" :sm="12" :lg="8">
          <el-form-item label="模板" prop="templateCode">
            <el-select v-model="formData.templateCode" class="w-full">
              <el-option label="新人首单补贴" value="new-user" />
              <el-option label="限时爆品冲量" value="flash-sale" />
              <el-option label="私域社群专享" value="private-domain" />
            </el-select>
          </el-form-item>
        </el-col>
        <el-col :xs="24" :sm="12" :lg="8">
          <el-form-item label="计划名称" prop="campaignName">
            <el-input v-model="formData.campaignName" placeholder="例如：618 爆品补贴" />
          </el-form-item>
        </el-col>
        <el-col :xs="24" :sm="12" :lg="8">
          <el-form-item label="平台">
            <el-select v-model="formData.platformCode" class="w-full">
              <el-option label="淘宝" value="taobao" />
              <el-option label="京东（仅计划）" value="jd" />
              <el-option label="拼多多（仅计划）" value="pdd" />
              <el-option label="抖音（仅计划）" value="douyin" />
            </el-select>
          </el-form-item>
        </el-col>
        <el-col :xs="24" :sm="12" :lg="8">
          <el-form-item label="商品ID">
            <el-input v-model="formData.goodsId" clearable />
          </el-form-item>
        </el-col>
        <el-col :xs="24" :sm="12" :lg="16">
          <el-form-item label="商品标题">
            <el-input v-model="formData.title" clearable />
          </el-form-item>
        </el-col>
        <el-col :xs="24" :sm="8">
          <el-form-item label="总预算" prop="budgetAmount">
            <el-input-number v-model="formData.budgetAmount" :min="0.01" :precision="2" class="w-full" />
          </el-form-item>
        </el-col>
        <el-col :xs="24" :sm="8">
          <el-form-item label="单份金额" prop="giftAmount">
            <el-input-number v-model="formData.giftAmount" :min="0.01" :precision="2" class="w-full" />
          </el-form-item>
        </el-col>
        <el-col :xs="24" :sm="8">
          <el-form-item label="发放份数" prop="totalQuantity">
            <el-input-number v-model="formData.totalQuantity" :min="1" class="w-full" />
          </el-form-item>
        </el-col>
        <el-col :xs="24" :sm="8">
          <el-form-item label="每人限领">
            <el-input-number v-model="formData.perUserLimit" :min="1" class="w-full" />
          </el-form-item>
        </el-col>
        <el-col :xs="24" :sm="16">
          <el-form-item label="活动时间">
            <el-date-picker
              v-model="dateRange"
              type="datetimerange"
              class="w-full"
              start-placeholder="开始时间"
              end-placeholder="结束时间"
              value-format="YYYY-MM-DDTHH:mm:ss"
            />
          </el-form-item>
        </el-col>
      </el-row>
    </el-form>

    <div class="panel-actions">
      <el-button type="primary" :loading="loading" @click="handlePlan">
        <Icon icon="ep:present" class="mr-5px" /> 生成计划
      </el-button>
      <el-button :disabled="!result?.promotionContent" @click="emitCopy">
        <Icon icon="ep:edit-pen" class="mr-5px" /> 写入文案区
      </el-button>
    </div>

    <div v-if="result" class="plan-grid">
      <el-card shadow="never">
        <template #header>预算测算</template>
        <el-descriptions :column="1">
          <el-descriptions-item label="状态">
            <el-tag :type="result.planStatus === 'READY' ? 'success' : 'warning'" effect="plain">
              {{ result.planStatus === 'READY' ? '可进入发放准备' : '需处理风险' }}
            </el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="总预算">￥{{ result.budgetAmount }}</el-descriptions-item>
          <el-descriptions-item label="单份金额">￥{{ result.giftAmount }}</el-descriptions-item>
          <el-descriptions-item label="发放份数">{{ result.totalQuantity }}</el-descriptions-item>
          <el-descriptions-item label="预算缺口">￥{{ result.budgetGap }}</el-descriptions-item>
        </el-descriptions>
      </el-card>
      <el-card shadow="never">
        <template #header>上线检查</template>
        <el-check-tag v-for="item in result.checklist" :key="item" class="check-item" checked>
          {{ item }}
        </el-check-tag>
      </el-card>
    </div>

    <el-alert
      v-for="item in result?.warnings || []"
      :key="item"
      class="mt-10px"
      type="warning"
      show-icon
      :closable="false"
      :title="item"
    />
  </div>
</template>

<script setup lang="ts">
import type { FormInstance, FormRules } from 'element-plus'
import {
  CpsRebateToolboxApi,
  type CpsGoodsCashGiftPlanRespVO
} from '@/api/cps/rebateToolbox'

const emit = defineEmits<{
  promotion: [value: string]
}>()

const message = useMessage()
const formRef = ref<FormInstance>()
const loading = ref(false)
const result = ref<CpsGoodsCashGiftPlanRespVO>()
const dateRange = ref<[string, string]>()

const formData = reactive({
  templateCode: 'new-user',
  campaignName: '社群专享补贴',
  platformCode: 'taobao',
  goodsId: '',
  title: '',
  budgetAmount: 100,
  giftAmount: 2,
  totalQuantity: 50,
  perUserLimit: 1
})

const formRules = reactive<FormRules>({
  templateCode: [{ required: true, message: '请选择模板', trigger: 'change' }],
  campaignName: [{ required: true, message: '请输入计划名称', trigger: 'blur' }],
  budgetAmount: [{ required: true, message: '请输入总预算', trigger: 'blur' }],
  giftAmount: [{ required: true, message: '请输入单份金额', trigger: 'blur' }],
  totalQuantity: [{ required: true, message: '请输入发放份数', trigger: 'blur' }]
})

const handlePlan = async () => {
  await formRef.value?.validate()
  loading.value = true
  try {
    result.value = await CpsRebateToolboxApi.planCashGift({
      ...formData,
      goodsId: formData.goodsId || undefined,
      title: formData.title || undefined,
      startTime: dateRange.value?.[0],
      endTime: dateRange.value?.[1]
    })
    if (result.value.planStatus === 'READY') message.success('淘礼金计划已生成')
    else message.warning('计划已生成，请处理风险提示')
  } finally {
    loading.value = false
  }
}

const emitCopy = () => {
  if (result.value?.promotionContent) emit('promotion', result.value.promotionContent)
}
</script>

<style scoped>
.tool-panel {
  display: flex;
  flex-direction: column;
}

.panel-actions {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
}

.plan-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(260px, 1fr));
  gap: 12px;
  margin-top: 16px;
}

.check-item {
  display: block;
  height: auto;
  margin-bottom: 8px;
  white-space: normal;
}
</style>
