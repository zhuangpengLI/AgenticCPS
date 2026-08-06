<template>
  <view class="list">
    <view
      v-for="item in items"
      :key="item.id || `${item.platformCode}-${item.goodsId}`"
      class="card"
      @tap="$emit('select', item)"
    >
      <image
        class="image"
        :src="item.mainPic || item.goodsImage || emptyImage"
        mode="aspectFill"
        @error="setImageFallback(item)"
      />
      <view class="content">
        <text class="title">{{ item.title || item.goodsTitle || '商品信息待更新' }}</text>
        <text v-if="item.snapshotUnavailable" class="tip">商品信息已失效，请重新搜索</text>
        <view class="bottom">
          <view>
            <text class="yen">¥</text
            ><text class="price">{{ money(item.actualPrice ?? item.price) }}</text>
          </view>
          <button v-if="removable" class="remove" @tap.stop="$emit('remove', item)">移除</button>
          <text v-else class="detail">查看实时详情 ></text>
        </view>
      </view>
    </view>
  </view>
</template>

<script setup>
  defineProps({
    items: { type: Array, default: () => [] },
    removable: { type: Boolean, default: false },
  });
  defineEmits(['select', 'remove']);
  const emptyImage = '/static/goods-empty.png';
  const money = (value) => Number(value || 0).toFixed(2);
  function setImageFallback(item) {
    if ('mainPic' in item) item.mainPic = emptyImage;
    else item.goodsImage = emptyImage;
  }
</script>

<style lang="scss" scoped>
  .list {
    padding: 0 24rpx 30rpx;
  }
  .card {
    display: flex;
    gap: 22rpx;
    margin-top: 20rpx;
    padding: 20rpx;
    border-radius: 18rpx;
    background: #fff;
  }
  .image {
    flex: none;
    width: 190rpx;
    height: 190rpx;
    border-radius: 14rpx;
    background: #eee;
  }
  .content {
    display: flex;
    min-width: 0;
    flex: 1;
    flex-direction: column;
  }
  .title {
    display: -webkit-box;
    overflow: hidden;
    color: #272932;
    font-size: 27rpx;
    line-height: 39rpx;
    -webkit-line-clamp: 2;
    -webkit-box-orient: vertical;
  }
  .tip {
    margin-top: 10rpx;
    color: #999ca5;
    font-size: 22rpx;
  }
  .bottom {
    display: flex;
    align-items: flex-end;
    justify-content: space-between;
    margin-top: auto;
  }
  .yen,
  .price {
    color: #ff3d52;
  }
  .yen {
    font-size: 20rpx;
  }
  .price {
    font-size: 34rpx;
    font-weight: 700;
  }
  .detail {
    color: #8b8e98;
    font-size: 21rpx;
  }
  .remove {
    margin: 0;
    padding: 0 22rpx;
    height: 58rpx;
    line-height: 58rpx;
    border: 1rpx solid #dddfe5;
    border-radius: 29rpx;
    color: #666974;
    background: #fff;
    font-size: 22rpx;
  }
</style>
