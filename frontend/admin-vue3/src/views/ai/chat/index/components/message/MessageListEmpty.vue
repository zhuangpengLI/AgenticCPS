<!-- 消息列表为空时，展示 prompt 列表 -->
<template>
  <div class="relative flex flex-row justify-center w-full h-full">
    <!-- title -->
    <div class="flex flex-col justify-center">
      <div class="text-28px font-bold text-center">AgenticCPS 联盟助手</div>
      <div class="flex flex-row flex-wrap items-center justify-center w-460px mt-20px">
        <div
          class="flex justify-center w-180px leading-50px border border-solid border-[#e4e4e4] rounded-10px m-10px cursor-pointer hover:bg-[rgba(243,243,243,0.73)]"
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
const promptList = [
  {
    prompt: '帮我搜索高性价比商品'
  },
  {
    prompt: '比较这款商品在各平台的价格和返利'
  },
  {
    prompt: '查询我的订单和返利进度'
  },
  {
    prompt: '推荐一个热门选品主题'
  }
] // prompt 列表

const emits = defineEmits(['onPrompt'])

/** 选中 prompt 点击 */
const handlerPromptClick = async ({ prompt }) => {
  emits('onPrompt', prompt)
}
</script>
