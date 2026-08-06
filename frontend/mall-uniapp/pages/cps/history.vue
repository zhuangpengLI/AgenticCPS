<template>
  <s-layout title="返利足迹" navbar="inner">
    <view class="page">
      <view class="toolbar">
        <text>浏览记录仅用于帮助你再次查找商品</text>
        <text v-if="state.list.length" class="clean" @tap="confirmClean">清空</text>
      </view>
      <view v-if="state.loading && !state.list.length" class="loading">正在加载足迹...</view>
      <view v-else-if="state.error && !state.list.length" class="state-card">
        <text>{{ state.error }}</text
        ><button class="retry" @tap="loadPage(true)">重新加载</button>
      </view>
      <s-empty
        v-else-if="!state.list.length"
        text="暂无返利商品足迹"
        icon="/static/goods-empty.png"
      />
      <growth-goods-list v-else :items="state.list" @select="goDetail" />
      <view v-if="state.loading && state.list.length" class="footer">加载中...</view>
      <view v-else-if="state.finished && state.list.length" class="footer">没有更多了</view>
    </view>
  </s-layout>
</template>

<script setup>
  import { reactive } from 'vue';
  import { onLoad, onPullDownRefresh, onReachBottom } from '@dcloudio/uni-app';
  import sheep from '@/sheep';
  import CpsMemberGoodsApi from '@/sheep/api/cps/memberGoods';
  import GrowthGoodsList from './components/growth-goods-list.vue';

  const state = reactive({
    list: [],
    pageNo: 1,
    pageSize: 20,
    total: 0,
    loading: false,
    finished: false,
    error: '',
  });
  const unwrap = (res, fallback) => {
    if (!res || res.code !== 0) throw new Error(res?.msg || fallback);
    return res.data;
  };

  async function loadPage(reset = false) {
    if (state.loading || (!reset && state.finished)) return;
    if (reset) {
      state.pageNo = 1;
      state.finished = false;
      state.error = '';
    }
    state.loading = true;
    try {
      const res = await CpsMemberGoodsApi.getHistoryPage({
        pageNo: state.pageNo,
        pageSize: state.pageSize,
      });
      const page = unwrap(res, '足迹加载失败') || {};
      const rows = Array.isArray(page.list) ? page.list : [];
      state.list = reset ? rows : state.list.concat(rows);
      state.total = Number(page.total || state.list.length);
      state.finished = rows.length < state.pageSize || state.list.length >= state.total;
      if (!state.finished) state.pageNo += 1;
    } catch (error) {
      state.error = error?.msg || error?.message || '足迹加载失败';
    } finally {
      state.loading = false;
      uni.stopPullDownRefresh();
    }
  }

  function goDetail(item) {
    if (!item.platformCode || !item.goodsId) {
      sheep.$helper.toast('商品标识已失效，请重新搜索');
      return;
    }
    sheep.$router.go('/pages/cps/goods-detail', {
      platformCode: item.platformCode,
      goodsId: item.goodsId,
      goodsSign: item.goodsSign || '',
    });
  }

  function confirmClean() {
    uni.showModal({
      title: '清空返利足迹',
      content: '清空后无法恢复，不会影响订单和返利记录。',
      success: async ({ confirm }) => {
        if (!confirm) return;
        try {
          unwrap(await CpsMemberGoodsApi.cleanHistory(), '清空失败');
          state.list = [];
          state.finished = true;
          sheep.$helper.toast('足迹已清空');
        } catch (error) {
          sheep.$helper.toast(error?.msg || error?.message || '清空失败');
        }
      },
    });
  }

  onLoad(() => loadPage(true));
  onPullDownRefresh(() => loadPage(true));
  onReachBottom(() => loadPage());
</script>

<style lang="scss" scoped>
  .page {
    min-height: 100vh;
    background: #f6f7fb;
  }
  .toolbar {
    display: flex;
    align-items: center;
    justify-content: space-between;
    padding: 24rpx;
    color: #8b8e98;
    background: #fff;
    font-size: 23rpx;
  }
  .clean {
    color: #ff4d4f;
  }
  .loading,
  .state-card,
  .footer {
    padding: 100rpx 24rpx;
    color: #9699a2;
    text-align: center;
  }
  .footer {
    padding: 20rpx;
    font-size: 22rpx;
  }
  .retry {
    width: 230rpx;
    margin-top: 24rpx;
    border: 1rpx solid #ff8a79;
    border-radius: 34rpx;
    color: #ff4d4f;
    background: #fff;
    font-size: 24rpx;
  }
</style>
