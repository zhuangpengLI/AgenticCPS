<!-- 消息列表为空时，展示 prompt 列表 -->
<template>
  <div class="relative flex flex-row justify-center w-full h-full">
    <!-- title -->
    <div class="flex flex-col justify-center">
      <div class="text-28px font-bold text-center">AgenticCPS 联盟助手</div>
      <div class="grid grid-cols-2 gap-12px w-760px max-w-[calc(100vw-360px)] mt-20px">
        <div
          class="flex items-center min-h-64px px-14px leading-22px border border-solid border-[#e4e4e4] rounded-10px cursor-pointer hover:bg-[rgba(243,243,243,0.73)]"
          v-for="prompt in promptList"
          :key="prompt.prompt"
          @click="handlerPromptClick(prompt)"
        >
          {{ prompt.prompt }}
        </div>
      </div>
    </div>
  </div>
</template>
<script setup lang="ts">
import { RECOMMENDED_PROMPTS, type RecommendedPrompt } from '../../toolActions'

const promptList = RECOMMENDED_PROMPTS

const emits = defineEmits<{
  (e: 'onPrompt', value: RecommendedPrompt): void
}>()

/** 选中 prompt 点击 */
const handlerPromptClick = (recommendedPrompt: RecommendedPrompt) => {
  emits('onPrompt', recommendedPrompt)
}
</script>
