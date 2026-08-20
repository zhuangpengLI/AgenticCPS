<template>
  <s-layout title="返利 AI 助手" navbar="inner">
    <view class="chat-page">
      <view v-if="!state.messages.length" class="welcome">
        <view class="welcome-icon _icon-magic" />
        <view class="welcome-title">{{ state.roleName || '返利 AI 助手' }}</view>
        <view class="welcome-text">可以帮你搜索商品、比较价格、解释返利规则</view>
        <view class="quick-list"><button v-for="item in quickPrompts" :key="item" class="ss-reset-button quick-item" @tap="sendPrompt(item)">{{ item }}</button></view>
      </view>
      <scroll-view v-else class="message-scroll" scroll-y :scroll-into-view="state.anchor">
        <view v-for="(item, index) in state.messages" :id="`msg-${index}`" :key="item.id || index" class="message-row" :class="item.type === 'USER' ? 'is-user' : 'is-ai'"><view class="message-bubble">{{ item.content }}</view></view>
        <view v-if="state.loading" class="message-row is-ai"><view class="message-bubble pending">正在思考...</view></view>
      </scroll-view>
      <view class="composer"><textarea v-model="state.input" class="composer-input" placeholder="问问返利 AI" :maxlength="500" @confirm="send" /><button class="ss-reset-button send-button" :disabled="state.loading" @tap="send">发送</button></view>
    </view>
  </s-layout>
</template>

<script setup>
  import { onLoad } from '@dcloudio/uni-app';
  import { reactive } from 'vue';
  import AiChatApi from '@/sheep/api/ai/chat';
  import sheep from '@/sheep';
  const quickPrompts = ['帮我找高返蓝牙耳机', '怎么计算返利？', '推荐一款适合送人的礼物'];
  const state = reactive({ conversationId: null, roleId: null, roleName: '', input: '', messages: [], loading: false, anchor: '' });
  let conversationPromise = null;
  async function ensureConversation() {
    if (state.conversationId) return true;
    if (conversationPromise) return conversationPromise;
    conversationPromise = (async () => {
      const created = await AiChatApi.createConversation({ roleId: state.roleId || undefined });
      if (created?.code !== 0 || !created.data) { sheep.$helper.toast('创建 AI 会话失败'); return false; }
      state.conversationId = created.data;
      const conversation = await AiChatApi.getConversation(state.conversationId);
      if (conversation?.code === 0 && conversation.data) {
        state.roleId = conversation.data.roleId;
        state.roleName = conversation.data.roleName || conversation.data.title || '返利 AI 助手';
      }
      return true;
    })();
    try { return await conversationPromise; } finally { conversationPromise = null; }
  }
  async function loadMessages() { if (!state.conversationId) return; const result = await AiChatApi.getMessages(state.conversationId); if (result?.code === 0) state.messages = result.data || []; }
  async function sendPrompt(text) { state.input = text; await send(); }
  async function send() {
    const content = state.input.trim(); if (!content || state.loading || !(await ensureConversation())) return;
    state.input = ''; state.loading = true;
    try {
      const result = await AiChatApi.send({ conversationId: state.conversationId, content });
      if (result === false) return;
      if (result?.code !== 0) { sheep.$helper.toast(result?.msg || 'AI 暂时无法响应'); return; }
      const response = result.data || {}; if (response.send) state.messages.push(response.send); if (response.receive) state.messages.push(response.receive); state.anchor = `msg-${state.messages.length - 1}`;
    } catch (error) { sheep.$helper.toast('网络异常，请稍后重试'); } finally { state.loading = false; }
  }
  onLoad(async (options = {}) => {
    if (options.roleId) state.roleId = Number(options.roleId);
    const conversations = await AiChatApi.getConversations(); const latest = (conversations?.data || [])[0];
    if (latest && !state.roleId) { state.conversationId = latest.id; state.roleId = latest.roleId; state.roleName = latest.roleName || latest.title || '返利 AI 助手'; await loadMessages(); return; }
    await ensureConversation();
  });
</script>

<style lang="scss" scoped>
  .chat-page { display: flex; min-height: calc(100vh - 88rpx); flex-direction: column; background: #f7f8fa; }
  .welcome { padding: 100rpx 38rpx 30rpx; text-align: center; }
  .welcome-icon { display: inline-flex; width: 92rpx; height: 92rpx; align-items: center; justify-content: center; border-radius: 28rpx; color: #fff; background: #f4513b; font-size: 48rpx; }
  .welcome-title { margin-top: 24rpx; color: #222; font-size: 36rpx; font-weight: 600; }
  .welcome-text { margin-top: 12rpx; color: #888; font-size: 24rpx; }
  .quick-list { display: flex; margin-top: 46rpx; flex-direction: column; gap: 18rpx; }
  .quick-item { padding: 20rpx 24rpx; border: 1rpx solid #f0d7d0; border-radius: 14rpx; color: #a25b49; background: #fff; font-size: 24rpx; text-align: left; }
  .message-scroll { flex: 1; padding: 24rpx 24rpx 180rpx; box-sizing: border-box; }
  .message-row { display: flex; margin-bottom: 20rpx; }
  .message-row.is-user { justify-content: flex-end; }
  .message-bubble { max-width: 78%; padding: 18rpx 22rpx; border-radius: 16rpx; color: #333; background: #fff; font-size: 26rpx; line-height: 1.55; overflow-wrap: anywhere; }
  .is-user .message-bubble { color: #fff; background: #f4513b; }
  .pending { color: #888; }
  .composer { position: fixed; right: 0; bottom: 0; left: 0; display: flex; align-items: flex-end; gap: 14rpx; padding: 18rpx 20rpx calc(18rpx + env(safe-area-inset-bottom)); background: #fff; box-shadow: 0 -4rpx 18rpx rgba(0,0,0,.05); }
  .composer-input { min-height: 70rpx; max-height: 160rpx; padding: 18rpx; box-sizing: border-box; border: 1rpx solid #e5e6eb; border-radius: 14rpx; flex: 1; font-size: 25rpx; }
  .send-button { width: 118rpx; height: 70rpx; border-radius: 14rpx; color: #fff; background: #f4513b; font-size: 25rpx; line-height: 70rpx; }
</style>
