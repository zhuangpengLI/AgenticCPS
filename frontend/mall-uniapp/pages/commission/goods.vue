<template>
  <s-layout title="CPS 商品搜索" navbar="inner">
    <view class="search-panel">
      <view class="search-row">
        <uni-easyinput
          :inputBorder="false"
          v-model="state.keyword"
          placeholder="搜索商品关键词"
          @confirm="onSearch"
        />
        <button class="ss-reset-button search-btn ui-BG-Main-Gradient" @tap="onSearch">
          搜索
        </button>
      </view>
      <view class="filter-row">
        <button
          class="ss-reset-button filter-btn"
          :class="{ active: state.platformCode === item.value }"
          v-for="item in platformTabs"
          :key="item.name"
          @tap="onPlatformChange(item.value)"
        >
          {{ item.name }}
        </button>
      </view>
      <view class="filter-row">
        <button
          class="ss-reset-button filter-btn"
          :class="{ active: state.sortType === item.value }"
          v-for="item in sortTabs"
          :key="item.name"
          @tap="onSortChange(item.value)"
        >
          {{ item.name }}
        </button>
        <button
          class="ss-reset-button filter-btn"
          :class="{ active: state.hasCoupon === 1 }"
          @tap="toggleCoupon"
        >
          只看有券
        </button>
        <button class="ss-reset-button compare-btn" @tap="compareGoods">跨平台比价</button>
      </view>

      <view class="compare-box" v-if="state.compareResult">
        <view class="compare-title">跨平台比价</view>
        <view class="compare-grid">
          <view
            class="compare-item"
            v-if="state.compareResult.cheapestGoods"
            @tap="goDetail(state.compareResult.cheapestGoods)"
          >
            <text class="compare-label">最低价</text>
            <text class="compare-name ss-line-1">
              {{ state.compareResult.cheapestGoods.title || '未知商品' }}
            </text>
            <text class="compare-value">
              ¥{{ formatMoney(state.compareResult.cheapestGoods.actualPrice) }}
            </text>
          </view>
          <view
            class="compare-item"
            v-if="state.compareResult.highestRebateGoods"
            @tap="goDetail(state.compareResult.highestRebateGoods)"
          >
            <text class="compare-label">最高返利</text>
            <text class="compare-name ss-line-1">
              {{ state.compareResult.highestRebateGoods.title || '未知商品' }}
            </text>
            <text class="compare-value">
              ¥{{ formatMoney(state.compareResult.highestRebateGoods.estimateRebateAmount) }}
            </text>
          </view>
          <view
            class="compare-item"
            v-if="state.compareResult.bestOverallGoods"
            @tap="goDetail(state.compareResult.bestOverallGoods)"
          >
            <text class="compare-label">综合推荐</text>
            <text class="compare-name ss-line-1">
              {{ state.compareResult.bestOverallGoods.title || '未知商品' }}
            </text>
            <text class="compare-value">
              ¥{{ formatMoney(state.compareResult.bestOverallGoods.actualPrice) }}
            </text>
          </view>
        </view>
      </view>

      <view class="parse-box">
        <view class="parse-title">链接/口令解析</view>
        <uni-easyinput
          type="textarea"
          :inputBorder="false"
          v-model="state.originalContent"
          placeholder="粘贴商品链接、商品 ID 或口令"
        />
        <button class="ss-reset-button parse-btn" @tap="parseContent">解析内容</button>
        <view class="parse-result" v-if="state.parseResult">
          <view class="parse-status" :class="{ success: state.parseResult.supported }">
            {{
              state.parseResult.supported
                ? '解析成功'
                : state.parseResult.failureReason || '暂不支持'
            }}
          </view>
          <view class="parse-line" v-if="state.parseResult.goodsId">
            商品 ID：{{ state.parseResult.goodsId }}
          </view>
          <view class="parse-line" v-if="state.parseResult.goodsSign">
            goodsSign：{{ state.parseResult.goodsSign }}
          </view>
          <view class="parse-line ss-line-1" v-if="state.parseResult.itemLink">
            商品链接：{{ state.parseResult.itemLink }}
          </view>
          <view class="parse-line ss-line-1" v-if="state.parseResult.couponLink">
            优惠券：{{ state.parseResult.couponLink }}
          </view>
          <view class="parse-line ss-line-1" v-if="state.parseResult.sourceLink">
            来源链接：{{ state.parseResult.sourceLink }}
          </view>
          <button
            v-if="state.parseResult.supported"
            class="ss-reset-button parse-link-btn"
            @tap="generateLink(state.parseResult)"
          >
            生成推广链接
          </button>
        </view>
      </view>
    </view>

    <view class="goods-list">
      <view
        class="goods-card"
        v-for="item in state.pagination.list"
        :key="goodsKey(item)"
        @tap="goDetail(item)"
      >
        <image
          class="goods-image"
          :src="item.mainPic || '/static/goods-empty.png'"
          mode="aspectFill"
        />
        <view class="goods-info">
          <view class="goods-title ss-line-2">{{ item.title || '未知商品' }}</view>
          <view class="goods-meta ss-line-1">
            {{ platformText(item.platformCode) }} · 月销 {{ item.monthSales || 0 }}
          </view>
          <view class="price-row">
            <text class="actual-price">¥{{ formatMoney(item.actualPrice) }}</text>
            <text class="origin-price">¥{{ formatMoney(item.originalPrice) }}</text>
            <text class="coupon" v-if="toNumber(item.couponPrice) > 0">
              券 ¥{{ formatMoney(item.couponPrice) }}
            </text>
          </view>
          <view class="rebate-row ss-flex ss-row-between ss-col-center">
            <text>
              佣金 {{ formatRate(item.commissionRate) }} · 预估 ¥{{
                formatMoney(item.estimateRebateAmount)
              }}
            </text>
            <button class="ss-reset-button link-btn" @tap.stop="generateLink(item)">转链</button>
          </view>
        </view>
      </view>

      <s-empty
        v-if="state.pagination.total === 0 && state.loadStatus !== 'loading'"
        icon="/static/goods-empty.png"
        text="暂无 CPS 商品"
      />

      <uni-load-more
        v-if="state.pagination.total > 0"
        :status="state.loadStatus"
        :content-text="{ contentdown: '上拉加载更多' }"
        @tap="loadMore"
      />
    </view>

    <su-popup
      :show="state.showLinkPopup"
      type="bottom"
      round="20"
      showClose
      @close="state.showLinkPopup = false"
    >
      <view class="link-panel" v-if="state.currentLink">
        <view class="link-title">推广链接</view>
        <view class="link-item" v-if="state.currentLink.shortUrl">
          <text>短链</text>
          <text class="value">{{ state.currentLink.shortUrl }}</text>
        </view>
        <view class="link-item" v-if="state.currentLink.mobileUrl">
          <text>移动链接</text>
          <text class="value">{{ state.currentLink.mobileUrl }}</text>
        </view>
        <view class="link-item" v-if="state.currentLink.longUrl">
          <text>长链</text>
          <text class="value">{{ state.currentLink.longUrl }}</text>
        </view>
        <view class="link-item" v-if="state.currentLink.tpwd">
          <text>淘口令</text>
          <text class="value">{{ state.currentLink.tpwd }}</text>
        </view>
      </view>
    </su-popup>
  </s-layout>
</template>

<script setup>
  import { reactive } from 'vue';
  import { onLoad, onReachBottom } from '@dcloudio/uni-app';
  import { concat } from 'lodash-es';
  import sheep from '@/sheep';
  import { resetPagination } from '@/sheep/helper/utils';
  import CpsGoodsApi from '@/sheep/api/cps/goods';

  const state = reactive({
    keyword: '咖啡',
    platformCode: undefined,
    sortType: 0,
    hasCoupon: undefined,
    originalContent: '',
    parseResult: null,
    compareResult: null,
    loadStatus: '',
    showLinkPopup: false,
    currentLink: null,
    pagination: {
      list: [],
      total: 0,
      pageNo: 1,
      pageSize: 10,
    },
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
    { name: '佣金', value: 4 },
  ];

  async function parseContent() {
    if (!state.originalContent) {
      sheep.$helper.toast('请粘贴商品链接、商品 ID 或口令');
      return;
    }
    const { code, data } = await CpsGoodsApi.parseContent({
      platformCode: state.platformCode || 'taobao',
      originalContent: state.originalContent,
    });
    if (code !== 0) {
      return;
    }
    state.parseResult = data;
  }

  async function searchGoods() {
    if (!state.keyword) {
      sheep.$helper.toast('请输入搜索关键词');
      return;
    }
    if (state.loadStatus === 'loading') {
      return;
    }
    state.loadStatus = 'loading';
    const { code, data } = await CpsGoodsApi.searchGoods({
      keyword: state.keyword,
      platformCode: state.platformCode,
      pageNo: state.pagination.pageNo,
      pageSize: state.pagination.pageSize,
      sortType: state.sortType,
      hasCoupon: state.hasCoupon,
    });
    if (code !== 0) {
      state.loadStatus = 'more';
      return;
    }
    const list = data?.list || [];
    state.pagination.list = concat(state.pagination.list, list);
    state.pagination.total = data?.total ?? state.pagination.list.length;
    state.loadStatus =
      state.pagination.total >= 0 && state.pagination.list.length >= state.pagination.total
        ? 'noMore'
        : 'more';
  }

  async function compareGoods() {
    if (!state.keyword) {
      sheep.$helper.toast('请输入搜索关键词');
      return;
    }
    const { code, data } = await CpsGoodsApi.compareGoods({
      keyword: state.keyword,
      pageSize: 12,
      sortType: state.sortType,
      hasCoupon: state.hasCoupon,
    });
    if (code !== 0) {
      return;
    }
    state.compareResult = data;
  }

  async function generateLink(item) {
    const { code, data } = await CpsGoodsApi.generateLink({
      platformCode: item.platformCode,
      goodsId: item.goodsId,
      goodsSign: item.goodsSign,
    });
    if (code !== 0 || !data) {
      return;
    }
    state.currentLink = data;
    state.showLinkPopup = true;
  }

  function goDetail(item) {
    sheep.$router.go(
      `/pages/commission/goods-detail?platformCode=${encodeURIComponent(
        item.platformCode || '',
      )}&goodsId=${encodeURIComponent(item.goodsId || '')}&goodsSign=${encodeURIComponent(
        item.goodsSign || '',
      )}`,
    );
  }

  function onSearch() {
    resetPagination(state.pagination);
    state.compareResult = null;
    searchGoods();
  }

  function onPlatformChange(platformCode) {
    state.platformCode = platformCode;
    onSearch();
  }

  function onSortChange(sortType) {
    state.sortType = sortType;
    onSearch();
  }

  function toggleCoupon() {
    state.hasCoupon = state.hasCoupon === 1 ? undefined : 1;
    onSearch();
  }

  function loadMore() {
    if (state.loadStatus === 'loading' || state.loadStatus === 'noMore') {
      return;
    }
    state.pagination.pageNo++;
    searchGoods();
  }

  function goodsKey(item) {
    return `${item.platformCode || 'all'}:${item.goodsId || item.goodsSign}`;
  }

  function platformText(platformCode) {
    const map = {
      taobao: '淘宝',
      jd: '京东',
      pdd: '拼多多',
      douyin: '抖音',
    };
    return map[platformCode] || platformCode || '未知平台';
  }

  function formatMoney(value) {
    return toNumber(value).toFixed(2);
  }

  function formatRate(value) {
    return `${toNumber(value).toFixed(2)}%`;
  }

  function toNumber(value) {
    const num = Number(value || 0);
    return Number.isFinite(num) ? num : 0;
  }

  onLoad(() => {
    searchGoods();
  });

  onReachBottom(() => {
    loadMore();
  });
</script>

<style lang="scss" scoped>
  .search-panel {
    padding: 22rpx;
    background: #ffffff;
  }

  .search-row {
    display: flex;
    gap: 16rpx;
    align-items: center;
  }

  .search-btn {
    flex: 0 0 120rpx;
    height: 66rpx;
    line-height: 66rpx;
    border-radius: 33rpx;
    font-size: 26rpx;
    color: #ffffff;
  }

  .filter-row {
    display: flex;
    gap: 12rpx;
    margin-top: 18rpx;
    overflow-x: auto;
  }

  .filter-btn {
    flex: 0 0 auto;
    height: 56rpx;
    line-height: 56rpx;
    padding: 0 20rpx;
    border-radius: 28rpx;
    font-size: 24rpx;
    color: #666666;
    background: #f6f6f6;
  }

  .filter-btn.active {
    color: #ffffff;
    background: var(--ui-BG-Main);
  }

  .compare-btn {
    flex: 0 0 auto;
    height: 56rpx;
    line-height: 56rpx;
    padding: 0 22rpx;
    border-radius: 28rpx;
    font-size: 24rpx;
    color: #ffffff;
    background: #333333;
  }

  .compare-box {
    margin-top: 22rpx;
    padding: 20rpx;
    background: #fff7f2;
    border-radius: 10rpx;
  }

  .compare-title {
    margin-bottom: 16rpx;
    font-size: 26rpx;
    font-weight: 600;
    color: #222222;
  }

  .compare-grid {
    display: grid;
    grid-template-columns: repeat(3, minmax(0, 1fr));
    gap: 12rpx;
  }

  .compare-item {
    min-width: 0;
    padding: 16rpx 12rpx;
    background: #ffffff;
    border-radius: 8rpx;
  }

  .compare-label,
  .compare-name,
  .compare-value {
    display: block;
  }

  .compare-label {
    font-size: 21rpx;
    color: #888888;
  }

  .compare-name {
    margin-top: 10rpx;
    font-size: 22rpx;
    color: #333333;
  }

  .compare-value {
    margin-top: 10rpx;
    font-size: 25rpx;
    font-weight: 600;
    color: #fa3534;
    word-break: break-all;
  }

  .parse-box {
    margin-top: 22rpx;
    padding: 20rpx;
    background: #f8f8f8;
    border-radius: 10rpx;
  }

  .parse-title {
    margin-bottom: 14rpx;
    font-size: 26rpx;
    font-weight: 600;
    color: #222222;
  }

  .parse-btn,
  .parse-link-btn {
    height: 58rpx;
    line-height: 58rpx;
    margin-top: 16rpx;
    border-radius: 29rpx;
    font-size: 24rpx;
    color: #ffffff;
    background: var(--ui-BG-Main);
  }

  .parse-link-btn {
    width: 180rpx;
  }

  .parse-result {
    margin-top: 16rpx;
    padding-top: 14rpx;
    border-top: 1rpx solid #eeeeee;
  }

  .parse-status {
    font-size: 24rpx;
    color: #fa3534;
  }

  .parse-status.success {
    color: var(--ui-BG-Main);
  }

  .parse-line {
    margin-top: 10rpx;
    font-size: 22rpx;
    color: #666666;
  }

  .goods-list {
    padding: 20rpx 20rpx 40rpx;
  }

  .goods-card {
    display: flex;
    gap: 20rpx;
    margin-bottom: 20rpx;
    padding: 22rpx;
    background: #ffffff;
    border-radius: 10rpx;
  }

  .goods-image {
    flex: 0 0 170rpx;
    width: 170rpx;
    height: 170rpx;
    border-radius: 8rpx;
    background: #f5f5f5;
  }

  .goods-info {
    flex: 1;
    min-width: 0;
  }

  .goods-title {
    min-height: 72rpx;
    font-size: 28rpx;
    font-weight: 600;
    color: #222222;
    line-height: 36rpx;
  }

  .goods-meta {
    margin-top: 10rpx;
    font-size: 22rpx;
    color: #888888;
  }

  .price-row {
    display: flex;
    align-items: baseline;
    gap: 12rpx;
    margin-top: 12rpx;
  }

  .actual-price {
    font-size: 32rpx;
    font-weight: 600;
    color: #fa3534;
  }

  .origin-price {
    font-size: 22rpx;
    color: #999999;
    text-decoration: line-through;
  }

  .coupon {
    padding: 3rpx 8rpx;
    border: 1rpx solid #fa3534;
    border-radius: 6rpx;
    font-size: 20rpx;
    color: #fa3534;
  }

  .rebate-row {
    margin-top: 14rpx;
    font-size: 22rpx;
    color: #666666;
    gap: 16rpx;
  }

  .link-btn {
    flex: 0 0 96rpx;
    height: 48rpx;
    line-height: 48rpx;
    border-radius: 24rpx;
    font-size: 22rpx;
    color: #ffffff;
    background: var(--ui-BG-Main);
  }

  .link-panel {
    max-height: 78vh;
    padding: 32rpx 28rpx 48rpx;
    overflow-y: auto;
    background: #ffffff;
  }

  .link-title {
    margin-bottom: 24rpx;
    font-size: 34rpx;
    font-weight: 600;
    color: #222222;
  }

  .link-item {
    padding: 20rpx 0;
    border-bottom: 1rpx solid #eeeeee;
    font-size: 26rpx;
    color: #777777;
  }

  .link-item .value {
    display: block;
    margin-top: 10rpx;
    color: #222222;
    word-break: break-all;
  }
</style>
