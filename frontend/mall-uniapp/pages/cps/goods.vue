<template>
  <s-layout title="查券返利" navbar="inner">
    <view class="search-panel">
      <view class="search-row">
        <uni-easyinput
          v-model="state.keyword"
          :inputBorder="false"
          placeholder="输入商品关键词"
          @confirm="onSearch"
        />
        <button class="ss-reset-button primary-button" @tap="onSearch">搜索</button>
      </view>

      <view v-if="!state.searched && state.recentKeywords.length" class="recent-row">
        <view class="minor-title">最近搜索</view>
        <view class="tag-list">
          <button
            v-for="item in state.recentKeywords"
            :key="item"
            class="ss-reset-button tag"
            @tap="searchRecent(item)"
            >{{ item }}</button
          >
          <button class="ss-reset-button clear-tag" @tap="clearRecent">清空</button>
        </view>
      </view>

      <view class="filter-scroll">
        <button
          v-for="item in platformTabs"
          :key="item.name"
          class="ss-reset-button filter"
          :class="{ active: state.platformCode === item.value }"
          @tap="onPlatformChange(item.value)"
          >{{ item.name }}</button
        >
      </view>
      <view class="filter-scroll">
        <button
          v-for="item in sortTabs"
          :key="item.name"
          class="ss-reset-button filter"
          :class="{ active: state.sortType === item.value }"
          @tap="onSortChange(item.value)"
          >{{ item.name }}</button
        >
        <button
          class="ss-reset-button filter"
          :class="{ active: state.hasCoupon === 1 }"
          @tap="toggleCoupon"
          >只看有券</button
        >
        <button
          class="ss-reset-button compare-button"
          :disabled="state.comparing"
          @tap="compareGoods"
          >跨平台比价</button
        >
      </view>

      <view class="parse-box">
        <view class="minor-title">链接/口令解析</view>
        <uni-easyinput
          type="textarea"
          v-model="state.originalContent"
          :inputBorder="false"
          placeholder="粘贴商品链接、商品 ID 或口令"
        />
        <button
          class="ss-reset-button outline-button"
          :disabled="state.parsing"
          @tap="parseContent"
          >{{ state.parsing ? '解析中...' : '解析内容' }}</button
        >
        <view v-if="state.parseResult" class="parse-result">
          <text :class="{ success: state.parseResult.supported }">{{
            state.parseResult.supported
              ? '解析成功，可领券购买'
              : state.parseResult.failureReason || '暂不支持该内容'
          }}</text>
          <button
            v-if="state.parseResult.supported"
            class="ss-reset-button mini-button"
            :disabled="state.linkLoading"
            @tap="generateLink(state.parseResult, state.originalContent)"
            >领券购买</button
          >
        </view>
      </view>
    </view>

    <view v-if="state.compareResult" class="compare-card">
      <view class="minor-title">跨平台比价</view>
      <view class="compare-grid">
        <view
          v-if="state.compareResult.cheapestGoods"
          class="compare-item"
          @tap="goDetail(state.compareResult.cheapestGoods)"
          ><text>最低价</text
          ><strong>¥{{ formatMoney(state.compareResult.cheapestGoods.actualPrice) }}</strong></view
        >
        <view
          v-if="state.compareResult.highestRebateGoods"
          class="compare-item"
          @tap="goDetail(state.compareResult.highestRebateGoods)"
          ><text>最高返利</text
          ><strong
            >¥{{ formatMoney(state.compareResult.highestRebateGoods.estimateRebateAmount) }}</strong
          ></view
        >
        <view
          v-if="state.compareResult.bestOverallGoods"
          class="compare-item"
          @tap="goDetail(state.compareResult.bestOverallGoods)"
          ><text>综合推荐</text
          ><strong
            >¥{{ formatMoney(state.compareResult.bestOverallGoods.actualPrice) }}</strong
          ></view
        >
      </view>
    </view>

    <view class="goods-list">
      <view
        v-if="state.loadStatus === 'loading' && !state.pagination.list.length"
        class="skeleton-list"
      >
        <view v-for="item in 3" :key="item" class="skeleton-card"
          ><view class="skeleton-image" /><view class="skeleton-lines"
            ><view /><view /><view /></view
        ></view>
      </view>
      <view
        v-for="item in state.pagination.list"
        :key="goodsKey(item)"
        class="goods-card"
        @tap="goDetail(item)"
      >
        <image
          class="goods-image"
          :src="imageUrl(item)"
          mode="aspectFill"
          @error="item.imageFailed = true"
        />
        <view class="goods-info">
          <view class="goods-title ss-line-2">{{ item.title || '未知商品' }}</view>
          <view class="goods-meta"
            >{{ platformText(item.platformCode) }} · 月销 {{ item.monthSales || 0 }}</view
          >
          <view class="decision-row"
            ><text class="price">券后 ¥{{ formatMoney(item.actualPrice) }}</text
            ><text v-if="toNumber(item.couponPrice) > 0" class="coupon"
              >券 ¥{{ formatMoney(item.couponPrice) }}</text
            ></view
          >
          <view class="rebate-row"
            ><text
              >预估返利 <strong>¥{{ formatMoney(item.estimateRebateAmount) }}</strong></text
            ><button
              class="ss-reset-button mini-button"
              :disabled="state.linkLoading"
              @tap.stop="generateLink(item)"
              >领券</button
            ></view
          >
        </view>
      </view>

      <view v-if="!state.searched && state.loadStatus !== 'loading'" class="search-guide">
        <image src="/static/goods-empty.png" mode="aspectFit" />
        <text>输入商品关键词，查看券后价与预估返利</text>
      </view>
      <view v-else-if="state.errorMessage && !state.pagination.list.length" class="error-state"
        ><text>{{ state.errorMessage }}</text
        ><button class="ss-reset-button outline-button" @tap="retrySearch">重试</button></view
      >
      <s-empty
        v-else-if="state.searched && state.pagination.total === 0 && state.loadStatus !== 'loading'"
        icon="/static/goods-empty.png"
        text="未找到相关商品"
      />
      <uni-load-more v-if="state.pagination.total > 0" :status="state.loadStatus" @tap="loadMore" />
    </view>

    <su-popup
      :show="state.showActionPopup"
      type="bottom"
      round="20"
      showClose
      @close="state.showActionPopup = false"
    >
      <view class="action-panel">
        <view class="action-title">领券购买</view>
        <view class="action-tip">{{
          state.promotionAction?.type === 'tpwd'
            ? '复制口令后打开淘宝'
            : `即将前往${platformText(state.promotionAction?.platformCode)}`
        }}</view>
        <button
          class="ss-reset-button action-button ui-BG-Main-Gradient"
          :disabled="state.actionExecuting"
          @tap="runPromotionAction"
          >{{
            state.actionExecuting
              ? '处理中...'
              : state.promotionAction?.type === 'tpwd'
              ? '复制口令'
              : '打开购买链接'
          }}</button
        >
        <button
          v-if="state.promotionAction?.type === 'url'"
          class="ss-reset-button copy-backup"
          @tap="copyPromotionBackup"
          >复制链接备用</button
        >
        <button
          v-if="state.actionError"
          class="ss-reset-button retry-link"
          @tap="runPromotionAction"
          >处理失败，点击重试</button
        >
      </view>
    </su-popup>
  </s-layout>
</template>

<script setup>
  import { reactive } from 'vue';
  import { onLoad, onReachBottom } from '@dcloudio/uni-app';
  import { concat } from 'lodash-es';
  import sheep from '@/sheep';
  import { showAuthModal } from '@/sheep/hooks/useModal';
  import { resetPagination } from '@/sheep/helper/utils';
  import CpsGoodsApi from '@/sheep/api/cps/goods';
  import { trackCpsEvent } from '@/sheep/helper/cpsAnalytics';
  import {
    createPromotionAction,
    copyPromotionValue,
    executePromotionAction,
    formatMoney,
    platformText,
    toNumber,
  } from '@/sheep/helper/cps';

  const RECENT_KEY = 'cps_recent_keywords';
  const state = reactive({
    keyword: '',
    platformCode: undefined,
    sortType: 0,
    hasCoupon: undefined,
    originalContent: '',
    parseResult: null,
    compareResult: null,
    recentKeywords: [],
    searched: false,
    parsing: false,
    comparing: false,
    linkLoading: false,
    loadStatus: '',
    errorMessage: '',
    showActionPopup: false,
    promotionAction: null,
    actionExecuting: false,
    actionError: false,
    pagination: { list: [], total: 0, pageNo: 1, pageSize: 10 },
  });
  const platformTabs = [
    { name: '全部', value: undefined },
    { name: '淘宝', value: 'taobao' },
    { name: '京东', value: 'jd' },
    { name: '拼多多', value: 'pdd' },
    { name: '抖音', value: 'douyin' },
  ];
  const sortTabs = [
    { name: '综合', value: 0 },
    { name: '销量', value: 1 },
    { name: '价格升', value: 2 },
    { name: '价格降', value: 3 },
    { name: '返利', value: 4 },
  ];

  function rememberKeyword(keyword) {
    state.recentKeywords = [
      keyword,
      ...state.recentKeywords.filter((item) => item !== keyword),
    ].slice(0, 8);
    uni.setStorageSync(RECENT_KEY, state.recentKeywords);
  }
  function clearRecent() {
    state.recentKeywords = [];
    uni.removeStorageSync(RECENT_KEY);
  }
  function searchRecent(keyword) {
    state.keyword = keyword;
    onSearch();
  }

  async function parseContent() {
    const originalContent = state.originalContent.trim();
    if (!originalContent || state.parsing) {
      if (!originalContent) sheep.$helper.toast('请粘贴商品链接或口令');
      return;
    }
    state.parsing = true;
    state.parseResult = null;
    try {
      const { code, data } = await CpsGoodsApi.parseContent({
        platformCode: state.platformCode || 'taobao',
        originalContent,
      });
      if (code === 0) state.parseResult = data;
    } finally {
      state.parsing = false;
    }
  }

  async function searchGoods() {
    if (!state.keyword.trim() || state.loadStatus === 'loading') return;
    state.loadStatus = 'loading';
    state.errorMessage = '';
    try {
      const { code, data } = await CpsGoodsApi.searchGoods({
        keyword: state.keyword.trim(),
        platformCode: state.platformCode,
        pageNo: state.pagination.pageNo,
        pageSize: state.pagination.pageSize,
        sortType: state.sortType,
        hasCoupon: state.hasCoupon,
      });
      if (code !== 0) {
        trackCpsEvent('cps_goods_search', {
          platformCode: state.platformCode || 'all',
          result: 'failed',
        });
        state.errorMessage = '加载失败，请稍后重试';
        state.loadStatus = 'more';
        return;
      }
      const list = data?.list || [];
      state.pagination.list = concat(state.pagination.list, list);
      state.pagination.total = data?.total ?? state.pagination.list.length;
      state.loadStatus = state.pagination.list.length >= state.pagination.total ? 'noMore' : 'more';
      trackCpsEvent('cps_goods_search', {
        platformCode: state.platformCode || 'all',
        result: 'success',
        resultCount: list.length,
        hasCoupon: state.hasCoupon === 1,
        sortType: state.sortType,
      });
    } catch (error) {
      trackCpsEvent('cps_goods_search', {
        platformCode: state.platformCode || 'all',
        result: 'network_error',
      });
      state.errorMessage = '网络异常，请重试';
      state.loadStatus = 'more';
    }
  }

  function onSearch() {
    const keyword = state.keyword.trim();
    if (!keyword) {
      sheep.$helper.toast('请输入搜索关键词');
      return;
    }
    state.keyword = keyword;
    state.searched = true;
    state.compareResult = null;
    rememberKeyword(keyword);
    resetPagination(state.pagination);
    searchGoods();
  }
  function retrySearch() {
    resetPagination(state.pagination);
    searchGoods();
  }
  function onPlatformChange(value) {
    state.platformCode = value;
    if (state.searched) onSearch();
  }
  function onSortChange(value) {
    state.sortType = value;
    if (state.searched) onSearch();
  }
  function toggleCoupon() {
    state.hasCoupon = state.hasCoupon === 1 ? undefined : 1;
    if (state.searched) onSearch();
  }

  async function compareGoods() {
    if (!state.keyword.trim()) {
      sheep.$helper.toast('请先输入搜索关键词');
      return;
    }
    if (state.comparing) return;
    state.comparing = true;
    try {
      const { code, data } = await CpsGoodsApi.compareGoods({
        keyword: state.keyword.trim(),
        pageSize: 12,
        sortType: state.sortType,
        hasCoupon: state.hasCoupon,
      });
      if (code === 0) state.compareResult = data;
    } finally {
      state.comparing = false;
    }
  }

  async function generateLink(item, originalContent) {
    if (!sheep.$store('user').isLogin) {
      showAuthModal();
      return;
    }
    if (state.linkLoading) return;
    state.linkLoading = true;
    try {
      const { code, data } = await CpsGoodsApi.generateLink({
        platformCode: item.platformCode || state.platformCode,
        goodsId: item.goodsId,
        goodsSign: item.goodsSign,
        originalContent,
      });
      if (code !== 0 || !data) {
        trackCpsEvent('cps_promotion_link', {
          platformCode: item.platformCode || state.platformCode,
          result: 'failed',
        });
        sheep.$helper.toast('生成购买链接失败，请重试');
        return;
      }
      const action = createPromotionAction(data, item.platformCode || state.platformCode);
      if (!action.value) {
        trackCpsEvent('cps_promotion_link', {
          platformCode: item.platformCode || state.platformCode,
          result: 'missing_action',
        });
        sheep.$helper.toast('暂未获取到可用购买链接');
        return;
      }
      state.promotionAction = action;
      state.actionError = false;
      state.showActionPopup = true;
      trackCpsEvent('cps_promotion_link', {
        platformCode: action.platformCode,
        result: 'success',
        actionType: action.type,
      });
    } catch (error) {
      trackCpsEvent('cps_promotion_link', {
        platformCode: item.platformCode || state.platformCode,
        result: 'network_error',
      });
      sheep.$helper.toast('生成购买链接失败，请重试');
    } finally {
      state.linkLoading = false;
    }
  }
  async function runPromotionAction() {
    if (state.actionExecuting) return;
    state.actionExecuting = true;
    state.actionError = false;
    try {
      await executePromotionAction(state.promotionAction);
      sheep.$helper.toast(state.promotionAction.displayText);
    } catch (error) {
      state.actionError = true;
      sheep.$helper.toast('打开或复制失败，请重试');
    } finally {
      state.actionExecuting = false;
    }
  }
  async function copyPromotionBackup() {
    try {
      await copyPromotionValue(
        state.promotionAction?.fallbackValue || state.promotionAction?.value,
      );
      sheep.$helper.toast('购买链接已复制');
    } catch (error) {
      sheep.$helper.toast('复制失败，请重试');
    }
  }
  function goDetail(item) {
    sheep.$router.go('/pages/cps/goods-detail', {
      platformCode: encodeURIComponent(item.platformCode || ''),
      goodsId: encodeURIComponent(item.goodsId || ''),
      goodsSign: encodeURIComponent(item.goodsSign || ''),
    });
  }
  function loadMore() {
    if (state.loadStatus === 'loading' || state.loadStatus === 'noMore') return;
    state.pagination.pageNo++;
    searchGoods();
  }
  function goodsKey(item) {
    return `${item.platformCode || 'all'}:${item.goodsId || item.goodsSign}`;
  }
  function imageUrl(item) {
    return item.imageFailed ? '/static/goods-empty.png' : item.mainPic || '/static/goods-empty.png';
  }

  onLoad((options = {}) => {
    state.recentKeywords = uni.getStorageSync(RECENT_KEY) || [];
    if (options.keyword) {
      state.keyword = decodeURIComponent(options.keyword);
      onSearch();
    }
    if (options.content) {
      state.originalContent = decodeURIComponent(options.content);
      parseContent();
    }
  });
  onReachBottom(loadMore);
</script>

<style lang="scss" scoped>
  .search-panel {
    padding: 22rpx;
    background: #fff;
  }
  .search-row {
    display: flex;
    gap: 14rpx;
    align-items: center;
  }
  .primary-button {
    flex: 0 0 118rpx;
    height: 66rpx;
    line-height: 66rpx;
    border-radius: 33rpx;
    color: #fff;
    background: var(--ui-BG-Main);
    font-size: 25rpx;
  }
  .minor-title {
    margin-bottom: 14rpx;
    font-size: 26rpx;
    font-weight: 600;
    color: #222;
  }
  .recent-row {
    margin-top: 22rpx;
  }
  .tag-list,
  .filter-scroll {
    display: flex;
    gap: 12rpx;
    overflow-x: auto;
  }
  .tag,
  .clear-tag,
  .filter {
    flex: 0 0 auto;
    padding: 0 20rpx;
    height: 54rpx;
    line-height: 54rpx;
    border-radius: 27rpx;
    background: #f5f5f5;
    color: #666;
    font-size: 23rpx;
  }
  .clear-tag {
    color: #999;
    background: transparent;
  }
  .filter-scroll {
    margin-top: 18rpx;
  }
  .filter.active {
    color: #fff;
    background: var(--ui-BG-Main);
  }
  .compare-button {
    flex: 0 0 auto;
    padding: 0 20rpx;
    border-radius: 27rpx;
    background: #333;
    color: #fff;
    font-size: 23rpx;
  }
  .parse-box {
    margin-top: 22rpx;
    padding: 20rpx;
    border-radius: 12rpx;
    background: #f8f8f8;
  }
  .outline-button {
    min-width: 180rpx;
    height: 60rpx;
    line-height: 60rpx;
    margin: 16rpx auto 0;
    border: 1rpx solid var(--ui-BG-Main);
    border-radius: 30rpx;
    color: var(--ui-BG-Main);
    font-size: 24rpx;
  }
  .parse-result {
    display: flex;
    align-items: center;
    justify-content: space-between;
    gap: 16rpx;
    margin-top: 16rpx;
    color: #fa3534;
    font-size: 23rpx;
  }
  .parse-result .success {
    color: #3b8d5b;
  }
  .mini-button {
    flex: 0 0 106rpx;
    height: 50rpx;
    line-height: 50rpx;
    border-radius: 25rpx;
    color: #fff;
    background: var(--ui-BG-Main);
    font-size: 22rpx;
  }
  .compare-card {
    margin: 20rpx;
    padding: 22rpx;
    border-radius: 14rpx;
    background: #fff7f2;
  }
  .compare-grid {
    display: grid;
    grid-template-columns: repeat(3, minmax(0, 1fr));
    gap: 12rpx;
  }
  .compare-item {
    min-width: 0;
    padding: 16rpx 10rpx;
    border-radius: 10rpx;
    background: #fff;
    font-size: 21rpx;
    color: #777;
  }
  .compare-item strong {
    display: block;
    margin-top: 9rpx;
    color: #fa3534;
    font-size: 25rpx;
    overflow-wrap: anywhere;
  }
  .goods-list {
    padding: 20rpx 20rpx 50rpx;
  }
  .goods-card,
  .skeleton-card {
    display: flex;
    gap: 18rpx;
    margin-bottom: 18rpx;
    padding: 20rpx;
    border-radius: 14rpx;
    background: #fff;
  }
  .goods-image,
  .skeleton-image {
    flex: 0 0 174rpx;
    width: 174rpx;
    height: 174rpx;
    border-radius: 10rpx;
    background: #f1f1f1;
  }
  .goods-info {
    flex: 1;
    min-width: 0;
  }
  .goods-title {
    min-height: 70rpx;
    font-size: 27rpx;
    font-weight: 600;
    line-height: 35rpx;
    color: #222;
  }
  .goods-meta {
    margin-top: 8rpx;
    font-size: 21rpx;
    color: #999;
  }
  .decision-row,
  .rebate-row {
    display: flex;
    align-items: center;
    justify-content: space-between;
    gap: 12rpx;
    margin-top: 12rpx;
  }
  .price {
    color: #fa3534;
    font-size: 29rpx;
    font-weight: 600;
  }
  .coupon {
    padding: 3rpx 8rpx;
    border: 1rpx solid #fa3534;
    border-radius: 5rpx;
    color: #fa3534;
    font-size: 19rpx;
  }
  .rebate-row {
    color: #8a5a3b;
    font-size: 22rpx;
  }
  .skeleton-lines {
    flex: 1;
    padding-top: 8rpx;
  }
  .skeleton-lines view {
    height: 24rpx;
    margin-bottom: 25rpx;
    border-radius: 12rpx;
    background: #f1f1f1;
  }
  .search-guide,
  .error-state {
    display: flex;
    flex-direction: column;
    align-items: center;
    gap: 20rpx;
    padding: 80rpx 30rpx;
    text-align: center;
    color: #999;
    font-size: 24rpx;
  }
  .search-guide image {
    width: 180rpx;
    height: 180rpx;
  }
  .action-panel {
    padding: 34rpx 28rpx 54rpx;
    background: #fff;
  }
  .action-title {
    font-size: 34rpx;
    font-weight: 600;
    color: #222;
  }
  .action-tip {
    margin-top: 18rpx;
    color: #777;
    font-size: 25rpx;
  }
  .action-button {
    width: 100%;
    height: 80rpx;
    line-height: 80rpx;
    margin-top: 30rpx;
    border-radius: 40rpx;
    color: #fff;
  }
  .retry-link {
    margin: 22rpx auto 0;
    color: #fa3534;
    font-size: 23rpx;
  }
  .copy-backup {
    margin: 20rpx auto 0;
    color: var(--ui-BG-Main);
    font-size: 23rpx;
  }
  button[disabled] {
    opacity: 0.55;
  }
</style>
