<template>
  <s-layout
    title="返利中心"
    navbar="inner"
    tabbar="/pages/cps/index"
    :bgStyle="{ color: '#f4513b' }"
  >
    <!-- 返利中心保留原有页面，首页使用独立的当前返利首页布局。 -->
    <s-cps-rebate-center />
  </s-layout>
</template>
