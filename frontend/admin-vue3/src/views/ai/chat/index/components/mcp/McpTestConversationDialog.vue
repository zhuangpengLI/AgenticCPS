<template>
  <el-dialog v-model="visible" title="创建 CPS 自测会话" width="520px" append-to-body>
    <el-alert
      title="管理员自测会话可使用完整 CPS 工具能力；会员身份创建后不可更改。"
      type="info"
      :closable="false"
      class="mb-16px"
    />
    <el-form
      ref="formRef"
      :model="formData"
      :rules="rules"
      label-width="90px"
    >
      <el-form-item label="默认角色"><el-input model-value="CPS 联盟助手" disabled /></el-form-item>
      <el-form-item label="测试会员" prop="memberId">
        <el-select v-model="formData.memberId" filterable remote reserve-keyword clearable class="w-full"
          placeholder="搜索会员昵称、姓名或手机号" :remote-method="searchMembers" :loading="loading">
          <el-option v-for="item in members" :key="item.id" :value="item.id" :label="memberLabel(item)" />
        </el-select>
      </el-form-item>
    </el-form>
    <template #footer><el-button @click="visible = false">取消</el-button><el-button type="primary" :loading="submitting" @click="submit">创建管理员自测会话</el-button></template>
  </el-dialog>
</template>
<script setup lang="ts">
import { getUserPage, type UserVO } from '@/api/member/user'
import { ChatMcpTestApi } from '@/api/ai/chat/mcpTest'
const emit = defineEmits<{ success: [id: number] }>()
const visible = ref(false); const loading = ref(false); const submitting = ref(false); const formRef = ref()
const members = ref<UserVO[]>([]); const formData = reactive<{ memberId?: number }>({})
const rules = { memberId: [{ required: true, message: '请选择测试会员', trigger: 'change' }] }
const memberLabel = (item: UserVO) => `${item.nickname || item.name || '会员'}（${item.mobile || item.id}）`
const searchMembers = async (keyword = '') => { loading.value = true; try { const page: any = await getUserPage({ pageNo: 1, pageSize: 20, nickname: keyword, mobile: keyword }); members.value = page.list || [] } finally { loading.value = false } }
const open = () => { visible.value = true; formData.memberId = undefined; searchMembers() }
const submit = async () => { await formRef.value.validate(); submitting.value = true; try { const id = await ChatMcpTestApi.create({ memberId: formData.memberId! }); visible.value = false; emit('success', id) } finally { submitting.value = false } }
defineExpose({ open })
</script>
