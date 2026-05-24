<template>
  <div class="result-editor">
    <div class="result-header">
      <div>
        <div class="result-title">推广文案</div>
        <div class="result-subtitle">支持继续编辑后复制，不回写转链记录</div>
      </div>
      <el-button :disabled="!content" @click="handleCopy">
        <Icon icon="ep:copy-document" class="mr-5px" /> 复制
      </el-button>
    </div>
    <el-input
      :model-value="content"
      type="textarea"
      :rows="13"
      resize="vertical"
      placeholder="转链成功后可在这里编辑推广文案"
      @update:model-value="emit('update:content', $event)"
    />
    <div class="result-actions">
      <el-button text :disabled="!content" @click="emit('update:content', '')">清空</el-button>
    </div>
  </div>
</template>

<script setup lang="ts">
import { useClipboard } from '@vueuse/core'

const props = defineProps<{
  content: string
}>()

const emit = defineEmits<{
  'update:content': [value: string]
}>()

const message = useMessage()
const { copy } = useClipboard()

const handleCopy = async () => {
  if (!props.content) return
  await copy(props.content)
  message.success('复制成功')
}
</script>

<style scoped>
.result-editor {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.result-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
}

.result-title {
  color: var(--el-text-color-primary);
  font-size: 15px;
  font-weight: 600;
}

.result-subtitle {
  margin-top: 3px;
  color: var(--el-text-color-secondary);
  font-size: 12px;
}

.result-actions {
  display: flex;
  justify-content: flex-end;
}
</style>
