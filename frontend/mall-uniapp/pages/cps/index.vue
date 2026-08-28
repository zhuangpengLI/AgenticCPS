<template>
  <s-layout
    title="返利工作台"
    navbar="inner"
    tabbar="/pages/cps/index"
    :bgStyle="{ color: '#f4513b' }"
  >
    <!-- 工作台只承载资产和返利工具；查券与活动统一从首页进入。 -->
    <!-- 返利转链：/pages/cps/transfer；返利 AI 对话：/pages/cps/ai-chat。 -->
    <!-- 为你精选：工作台内集中展示精选返利商品。 -->
    <s-cps-rebate-center />
  </s-layout>
</template>
