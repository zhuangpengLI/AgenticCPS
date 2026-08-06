<template>
  <s-layout title="返利收藏" navbar="inner">
    <view class="page">
      <view class="notice">收藏保存的是商品线索，价格与返利以进入实时详情后的结果为准。</view>
      <view v-if="state.loading && !state.list.length" class="loading">正在加载收藏...</view>
      <view v-else-if="state.error && !state.list.length" class="state-card">
        <text>{{ state.error }}</text
        ><button class="retry" @tap="loadPage(true)">重新加载</button>
      </view>
      <s-empty
        v-else-if="!state.list.length"
        text="暂未收藏返利商品"
        icon="/static/goods-empty.png"
      />
      <growth-goods-list
        v-else
        :items="state.list"
        removable
        @select="goDetail"
        @remove="removeFavorite"
      />
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
    removingId: null,
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
      const res = await CpsMemberGoodsApi.getFavoritePage({
        pageNo: state.pageNo,
        pageSize: state.pageSize,
      });
      const page = unwrap(res, '收藏加载失败') || {};
      const rows = Array.isArray(page.list) ? page.list : [];
      state.list = reset ? rows : state.list.concat(rows);
      state.total = Number(page.total || state.list.length);
      state.finished = rows.length < state.pageSize || state.list.length >= state.total;
      if (!state.finished) state.pageNo += 1;
    } catch (error) {
      state.error = error?.msg || error?.message || '收藏加载失败';
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

  async function removeFavorite(item) {
    const key = item.id || `${item.platformCode}-${item.goodsId}`;
    if (state.removingId) return;
    state.removingId = key;
    try {
      unwrap(
        await CpsMemberGoodsApi.deleteFavorite({
          id: item.id,
          platformCode: item.platformCode,
          goodsId: item.goodsId,
          goodsSign: item.goodsSign || undefined,
        }),
        '移除失败',
      );
      state.list = state.list.filter((row) => row !== item);
      sheep.$helper.toast('已移出收藏');
    } catch (error) {
      sheep.$helper.toast(error?.msg || error?.message || '移除失败');
    } finally {
      state.removingId = null;
    }
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
  .notice {
    padding: 22rpx 24rpx;
    color: #8b8e98;
    background: #fff8eb;
    font-size: 22rpx;
    line-height: 1.5;
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
