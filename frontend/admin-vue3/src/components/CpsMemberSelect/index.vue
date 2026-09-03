<template>
  <el-select
    :model-value="modelValue"
    filterable
    remote
    reserve-keyword
    clearable
    :placeholder="placeholder"
    :loading="loading"
    :remote-method="searchMembers"
    :class="className"
    @update:model-value="handleUpdate"
    @visible-change="handleVisibleChange"
  >
    <el-option
      v-for="member in options"
      :key="member.id"
      :label="formatMemberLabel(member)"
      :value="member.id"
    />
  </el-select>
</template>

<script setup lang="ts">
import { getUser, getUserPage, type UserVO } from '@/api/member/user'

const props = withDefaults(
  defineProps<{
    modelValue?: number
    placeholder?: string
    className?: string
  }>(),
  {
    placeholder: '请选择会员',
    className: '!w-180px'
  }
)

const emit = defineEmits<{
  'update:modelValue': [value: number | undefined]
}>()

const loading = ref(false)
const options = ref<UserVO[]>([])

const formatMemberLabel = (member: UserVO) => {
  const name = member.nickname || member.name || `会员${member.id}`
  return `${name}（ID: ${member.id}）`
}

const searchMembers = async (keyword = '') => {
  loading.value = true
  try {
    const query = keyword.trim()
    const data = await getUserPage({
      pageNo: 1,
      pageSize: 20,
      mobile: /^\d+$/.test(query) ? query : undefined,
      nickname: query && !/^\d+$/.test(query) ? query : undefined
    })
    const nextOptions = data?.list || []
    const selected = options.value.find((member) => member.id === props.modelValue)
    if (selected && !nextOptions.some((member) => member.id === selected.id)) {
      nextOptions.unshift(selected)
    }
    options.value = nextOptions
  } finally {
    loading.value = false
  }
}

const handleVisibleChange = (visible: boolean) => {
  if (visible && options.value.length === 0) {
    searchMembers()
  }
}

const handleUpdate = (value: number | undefined) => {
  emit('update:modelValue', value)
}

watch(
  () => props.modelValue,
  async (value) => {
    if (value == null || options.value.some((member) => member.id === value)) return
    try {
      const member = await getUser(value)
      if (member && !options.value.some((item) => item.id === member.id)) {
        options.value.unshift(member)
      }
    } catch {
      // The selected value can still be cleared or replaced by the parent.
    }
  },
  { immediate: true }
)
</script>
