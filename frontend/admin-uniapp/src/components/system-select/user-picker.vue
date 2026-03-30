<template>
  <wd-select-picker
    v-if="useDefaultSlot"
    v-model="selectedId"
    :label="label"
    :label-width="label ? '180rpx' : '0'"
    :columns="userList"
    value-key="id"
    label-key="nickname"
    :type="type"
    :prop="prop"
    use-default-slot
    filterable
    :placeholder="placeholder"
    @confirm="handleConfirm"
  >
    <slot />
  </wd-select-picker>

  <wd-select-picker
    v-else
    v-model="selectedId"
    :label="label"
    :label-width="label ? '180rpx' : '0'"
    :columns="userList"
    value-key="id"
    label-key="nickname"
    :type="type"
    :prop="prop"
    filterable
    :placeholder="placeholder"
    @confirm="handleConfirm"
  />
</template>

<script lang="ts" setup>
import type { User } from '@/api/system/user'
import { onMounted, ref, watch } from 'vue'
import { getSimpleUserList } from '@/api/system/user'

const props = withDefaults(defineProps<{
  modelValue?: number | number[]
  type?: 'radio' | 'checkbox'
  label?: string
  placeholder?: string
  prop?: string
  useDefaultSlot?: boolean
}>(), {
  type: 'checkbox',
  label: '',
  placeholder: '请选择',
  prop: '',
  useDefaultSlot: false,
})

const emit = defineEmits<{
  (e: 'update:modelValue', value: number | number[] | undefined): void
  (e: 'confirm', users: User[]): void
}>()

const userList = ref<User[]>([])
const selectedId = ref<number | string | number[]>([])

/** 根据用户 ID 获取昵称 */
function getUserNickname(userId: number | undefined): string {
  if (!userId) {
    return ''
  }
  const user = userList.value.find(u => u.id === userId)
  return user?.nickname || ''
}

defineExpose({
  getUserNickname,
})

watch(
  () => props.modelValue,
  (val) => {
    if (props.type === 'radio') {
      // 单选时，如果值为 undefined，使用空字符串避免警告
      selectedId.value = val !== undefined ? val : ''
    } else {
      // 多选时，确保是数组
      selectedId.value = Array.isArray(val) ? val : []
    }
  },
  { immediate: true },
)

/** 加载用户列表 */
async function loadUserList() {
  userList.value = await getSimpleUserList()
}

/** 选择确认 */
function handleConfirm({ value }: { value: any }) {
  emit('update:modelValue', value)

  // 发出包含完整用户对象的 confirm 事件
  if (Array.isArray(value)) {
    const selectedUsers = userList.value.filter(user => value.includes(user.id))
    emit('confirm', selectedUsers)
  } else if (value) {
    const selectedUser = userList.value.find(user => user.id === value)
    emit('confirm', selectedUser ? [selectedUser] : [])
  } else {
    emit('confirm', [])
  }
}

/** 初始化 */
onMounted(() => {
  loadUserList()
})
</script>
