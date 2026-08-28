<template>
  <s-layout title="返利 AI 助手" navbar="inner">
    <view v-if="!v2Enabled" class="legacy-page">
      <view v-if="!state.messages.length" class="legacy-welcome"
        ><view class="legacy-icon">AI</view
        ><view class="legacy-title">{{ state.roleName || '返利 AI 助手' }}</view
        ><view class="legacy-copy">可以帮你搜索商品、比较价格、解释返利规则</view
        ><button
          v-for="prompt in quickPrompts.slice(0, 3)"
          :key="prompt"
          @tap="sendPrompt(prompt)"
          >{{ prompt }}</button
        ></view
      >
      <scroll-view v-else class="legacy-messages" scroll-y :scroll-into-view="state.anchor"
        ><view
          v-for="(message, index) in state.messages"
          :id="`ai-msg-${index}`"
          :key="message.id || index"
          class="legacy-row"
          :class="String(message.type || '').toUpperCase() === 'USER' ? 'legacy-user' : ''"
          ><view>{{ message.content }}</view></view
        ></scroll-view
      >
      <view class="legacy-composer">
        <textarea v-model="state.input" auto-height placeholder="问问返利 AI" /><button
          :disabled="state.loading || !state.input.trim()"
          @tap="send"
          >发送</button
        ></view
      >
    </view>
    <view v-else class="chat-page">
      <AiChatHeader
        :name="state.roleName"
        :avatar="state.roleAvatar"
        @history="showHistory"
        @new-chat="newChat"
      />
      <view class="chat-body">
        <AiChatWelcome
          v-if="!state.messages.length"
          :name="state.roleName"
          :avatar="state.roleAvatar"
          :prompts="quickPrompts"
          @prompt="sendPrompt"
        />
        <AiChatMessageList
          v-else
          :messages="state.messages"
          :anchor="state.anchor"
          :loading="state.loading"
          :error="state.error"
          @action="handleAction"
          @retry="retryLast"
        />
      </view>
      <AiChatComposer
        v-model="state.input"
        :attachments="state.attachments"
        :disabled="state.loading"
        :recording="state.recording"
        :voice-status="state.voiceStatus"
        @send="send"
        @choose-image="chooseImages"
        @remove-attachment="removeAttachment"
        @record-start="startRecording"
        @record-stop="stopRecording"
        @voice-open="openVoiceDialog"
        @voice-close="closeVoiceDialog"
      />
      <AiChatHistoryDrawer
        :visible="state.historyOpen"
        :conversations="state.historyConversations"
        @close="state.historyOpen = false"
        @select="selectHistory"
        @delete="deleteHistory"
      />
    </view>
  </s-layout>
</template>

<script setup>
  import { nextTick, onBeforeUnmount, reactive } from 'vue';
  import { onLoad } from '@dcloudio/uni-app';
  import sheep from '@/sheep';
  import { showAuthModal } from '@/sheep/hooks/useModal';
  import AiChatApi from '@/sheep/api/ai/chat';
  import { sendAiChatStream } from '@/sheep/api/ai/ai-chat-transport';
  import {
    startVoiceRecording,
    stopVoiceRecording,
    transcribeVoice,
  } from '@/sheep/api/ai/ai-chat-voice';
  import CpsGoodsApi from '@/sheep/api/cps/goods';
  import {
    copyPromotionValue,
    createPromotionAction,
    executePromotionAction,
    openPromotionUrl,
    platformText,
    promotionUrl,
  } from '@/sheep/helper/cps';
  import { chooseAndUploadFile } from '@/sheep/components/s-uploader/choose-and-upload-file';
  import AiChatHeader from './components/ai-chat/AiChatHeader.vue';
  import AiChatHistoryDrawer from './components/ai-chat/AiChatHistoryDrawer.vue';
  import AiChatWelcome from './components/ai-chat/AiChatWelcome.vue';
  import AiChatMessageList from './components/ai-chat/AiChatMessageList.vue';
  import AiChatComposer from './components/ai-chat/AiChatComposer.vue';

  const quickPrompts = [
    '帮我找一款高返蓝牙耳机',
    '比较 iPhone 在各平台的到手价',
    '查询我的返利余额和订单进度',
    '推荐一款预算 500 元的送礼好物',
    '优化商品展示，并结合优惠券、到手价和返利给出购买建议',
  ];
  // Deployment can set SHOPRO_AI_CHAT_V2_ON=0 for a reversible UI rollback.
  const v2Enabled = import.meta.env.SHOPRO_AI_CHAT_V2_ON !== '0';

  const state = reactive({
    conversationId: null,
    roleId: null,
    roleName: '',
    roleAvatar: '',
    input: '',
    attachments: [],
    messages: [],
    loading: false,
    anchor: '',
    error: '',
    lastContent: '',
    lastAttachments: [],
    lastAction: null,
    recording: false,
    voiceStatus: '',
    recordStartedAt: 0,
    abortController: null,
    historyOpen: false,
    historyConversations: [],
  });
  let conversationPromise = null;

  async function ensureConversation() {
    if (state.conversationId) return true;
    if (conversationPromise) return conversationPromise;
    conversationPromise = (async () => {
      const created = await AiChatApi.createConversation({ roleId: state.roleId || undefined });
      if (created?.code !== 0 || !created.data) {
        sheep.$helper.toast('创建 AI 会话失败');
        return false;
      }
      state.conversationId = created.data;
      const conversation = await AiChatApi.getConversation(state.conversationId);
      if (conversation?.code === 0 && conversation.data) {
        state.roleId = conversation.data.roleId;
        state.roleName = conversation.data.roleName || conversation.data.title || '返利 AI 助手';
        state.roleAvatar = conversation.data.roleAvatar || '';
      }
      return true;
    })();
    try {
      return await conversationPromise;
    } finally {
      conversationPromise = null;
    }
  }

  async function loadMessages() {
    if (!state.conversationId) return;
    const result = await AiChatApi.getMessages(state.conversationId);
    if (result?.code === 0) state.messages = (result.data || []).map(normalizeMessage);
    scrollToEnd();
  }

  function normalizeMessage(message) {
    return {
      ...message,
      toolExecutions: message.toolExecutions || [],
      reasoningOpen: false,
      blocks: message.blocks || [],
    };
  }

  function sendPrompt(text) {
    state.input = text;
    send();
  }

  async function send() {
    const content =
      state.input.trim() ||
      (state.attachments.length ? '请分析我上传的图片，并给出购买建议。' : '');
    if (!content || state.loading || !(await ensureConversation())) return;
    state.input = '';
    state.error = '';
    state.lastContent = content;
    state.lastAttachments = state.attachments.map((item) => item.url);
    state.messages.push(
      normalizeMessage({
        id: `local-${Date.now()}`,
        type: 'USER',
        content,
        attachmentUrls: state.lastAttachments,
      }),
    );
    state.attachments = [];
    state.messages.push(
      normalizeMessage({
        id: `assistant-${Date.now()}`,
        type: 'ASSISTANT',
        content: '',
        blocks: [],
        toolExecutions: [],
      }),
    );
    state.loading = true;
    state.abortController = typeof AbortController !== 'undefined' ? new AbortController() : null;
    scrollToEnd();
    await sendAiChatStream(
      {
        conversationId: state.conversationId,
        content,
        useContext: true,
        useSearch: false,
        attachmentUrls: state.lastAttachments,
        toolIntent: state.lastAction?.intent,
        intentRequestId: state.lastAction?.intentRequestId,
      },
      {
        onEvent: handleStreamEvent,
        onError: (error) => {
          state.error = error?.message || '网络异常，请稍后重试';
        },
        signal: state.abortController?.signal,
      },
    );
    state.loading = false;
    state.abortController = null;
    state.lastAction = null;
    scrollToEnd();
  }

  function handleStreamEvent(event) {
    if (!event) return;
    const type = event.eventType || 'MESSAGE_COMPLETE';
    const assistant = state.messages[state.messages.length - 1];
    if (!assistant || String(assistant.type || '').toUpperCase() !== 'ASSISTANT') return;
    if (type === 'TOOL_STARTED' || type === 'TOOL_SUCCEEDED' || type === 'TOOL_FAILED') {
      const execution = event.toolExecution || {};
      const list = assistant.toolExecutions || [];
      const index = list.findIndex((item) => item.executionId === execution.executionId);
      if (index >= 0) list[index] = { ...list[index], ...execution };
      else list.push(execution);
      assistant.toolExecutions = list;
      return;
    }
    const receive = event.receive || event.data?.receive;
    if (receive) {
      if (type === 'MESSAGE_DELTA') assistant.content += receive.content || '';
      else assistant.content = receive.content || assistant.content;
      assistant.reasoningContent =
        type === 'MESSAGE_DELTA'
          ? `${assistant.reasoningContent || ''}${receive.reasoningContent || ''}`
          : receive.reasoningContent || assistant.reasoningContent;
      assistant.blocks = receive.blocks || assistant.blocks || [];
      assistant.webSearchPages = receive.webSearchPages || assistant.webSearchPages;
      scrollToEnd();
    }
  }

  async function chooseImages() {
    if (state.attachments.length >= 3) {
      sheep.$helper.toast('最多上传 3 张图片');
      return;
    }
    try {
      const files = await chooseAndUploadFile({
        type: 'image',
        count: 3 - state.attachments.length,
        directory: 'ai/chat',
      });
      (files || []).slice(0, 3 - state.attachments.length).forEach((file) => {
        if (file?.url) state.attachments.push({ url: file.url });
      });
    } catch (error) {
      sheep.$helper.toast('图片上传失败，请重试');
    }
  }

  function removeAttachment(item) {
    state.attachments = state.attachments.filter((file) => file.url !== item.url);
  }

  function startRecording() {
    if (state.loading || state.recording) return;
    state.recording = true;
    state.voiceStatus = '';
    state.recordStartedAt = Date.now();
    startVoiceRecording({
      onStart: () => {},
      onStop: handleRecording,
      onError: (error) => {
        state.recording = false;
        state.voiceStatus = '';
        sheep.$helper.toast(error?.message || '无法使用麦克风');
      },
    });
  }

  function stopRecording() {
    if (!state.recording) return;
    state.recording = false;
    state.voiceStatus = '正在转成中文…';
    stopVoiceRecording();
  }

  async function handleRecording(file) {
    // A recorder can stop automatically at the platform's 60s limit. Keep the
    // composer in sync even when no touchend event was delivered.
    state.recording = false;
    state.voiceStatus = '正在转成中文…';
    try {
      const text = await transcribeVoice(file);
      if (text) {
        state.input = `${state.input}${state.input ? ' ' : ''}${text}`;
        state.voiceStatus = '识别完成，正在发送…';
        await send();
      } else sheep.$helper.toast('没有识别到有效内容');
    } catch (error) {
      sheep.$helper.toast(error?.message || '语音转写失败');
    } finally {
      state.voiceStatus = '';
    }
  }

  function openVoiceDialog() {
    state.voiceStatus = '';
  }
  function closeVoiceDialog() {
    if (!state.recording && !state.voiceStatus) state.voiceStatus = '';
  }

  async function handleAction({ action, item }) {
    if (action?.type === 'OPEN_DETAIL') {
      const product = item || action.payload || {};
      sheep.$router.go('/pages/cps/goods-detail', {
        platformCode: product.platformCode || '',
        goodsId: product.goodsId || '',
        goodsSign: product.goodsSign || '',
      });
      return;
    }
    if (action?.type === 'GENERATE_LINK') {
      const payload = action.payload || {};
      if (!sheep.$store('user').isLogin || !uni.getStorageSync('token')) {
        showAuthModal();
        sheep.$helper.toast('请先登录后再生成购买链接');
        return;
      }
      let result;
      try {
        result = await CpsGoodsApi.generateLink({
          platformCode: payload.platformCode,
          goodsId: payload.goodsId,
          goodsSign: payload.goodsSign,
          vendorCode: payload.vendorCode,
        });
      } catch (error) {
        const message = error?.msg || error?.message || '';
        if (/会员不能为空|请先登录|未登录|未授权|登录已过期|登陆已过期/i.test(message)) {
          showAuthModal();
          sheep.$helper.toast('请先登录后再生成购买链接');
        } else {
          sheep.$helper.toast(message || '生成链接失败');
        }
        return;
      }
      if (result?.code === 0 && result.data) {
        if (item) Object.assign(item, result.data, { promotionUrl: promotionUrl(result.data) });
        if (payload.delivery === 'copy') {
          const value =
            result.data.tpwd ||
            promotionUrl(result.data) ||
            result.data.promotionContent ||
            result.data.command;
          if (!value) throw new Error('EMPTY_PROMOTION_VALUE');
          await copyPromotionValue(value);
          sheep.$helper.toast(
            result.data.tpwd ? '口令已复制，请打开对应APP购买' : '购买链接已复制',
          );
          return;
        }
        try {
          const promotion = createPromotionAction(result.data, payload.platformCode);
          if (promotion.type === 'tpwd') {
            sheep.$helper.toast(
              `已生成${platformText(payload.platformCode)}购买信息，请点击“复制口令”后打开对应APP`,
            );
          } else {
            await executePromotionAction(promotion);
            sheep.$helper.toast(`已打开${platformText(payload.platformCode)}购买页`);
          }
        } catch (error) {
          sheep.$helper.toast('推广链接已生成，请点击购买按钮重试');
        }
      } else sheep.$helper.toast(result?.msg || '生成链接失败');
      return;
    }
    if (action?.type === 'OPEN_PROMOTION') {
      const url = action.payload?.url || promotionUrl(item || {});
      try {
        const result = await openPromotionUrl(url);
        if (result.opened)
          sheep.$helper.toast(`已打开${platformText(action.payload?.platformCode)}购买页`);
        else {
          await copyPromotionValue(url);
          sheep.$helper.toast(
            `无法直接打开，请复制链接后打开${platformText(action.payload?.platformCode)}APP`,
          );
        }
      } catch (error) {
        sheep.$helper.toast('购买链接暂不可用，请稍后重试');
      }
      return;
    }
    if (action?.type === 'COPY_COMMAND') {
      try {
        await copyPromotionValue(action.payload?.value || item?.tpwd || item?.command);
        sheep.$helper.toast(
          `口令已复制，请打开${platformText(action.payload?.platformCode)}APP购买`,
        );
      } catch (error) {
        sheep.$helper.toast('口令复制失败，请稍后重试');
      }
      return;
    }
    if (action?.type === 'SEND_PROMPT') {
      state.input = action.payload?.prompt || '';
      send();
    }
  }

  function retryLast() {
    state.input = state.lastContent;
    state.attachments = state.lastAttachments.map((url) => ({ url }));
    send();
  }

  async function showHistory() {
    const result = await AiChatApi.getConversations();
    const list = result?.data || [];
    state.historyConversations = list;
    state.historyOpen = true;
  }

  async function deleteHistory(conversation) {
    if (state.conversationId === conversation.id && state.loading) {
      sheep.$helper.toast('回复生成中，暂时无法删除当前会话');
      return;
    }
    const title = conversation.title || conversation.roleName || 'AI 会话';
    const confirmed = await new Promise((resolve) => {
      uni.showModal({
        title: '删除历史会话',
        content: `确定删除“${title}”吗？`,
        confirmColor: '#e34d59',
        success: (result) => resolve(result.confirm),
        fail: () => resolve(false),
      });
    });
    if (!confirmed) return;
    const result = await AiChatApi.deleteConversation(conversation.id);
    if (result?.code !== 0) {
      sheep.$helper.toast(result?.msg || '删除会话失败');
      return;
    }
    state.historyConversations = state.historyConversations.filter(
      (item) => item.id !== conversation.id,
    );
    if (state.conversationId === conversation.id) {
      state.conversationId = null;
      state.messages = [];
      state.input = '';
      state.attachments = [];
      state.error = '';
      state.anchor = '';
      state.lastContent = '';
      state.lastAttachments = [];
      state.lastAction = null;
    }
    sheep.$helper.toast('历史会话已删除');
  }

  async function selectHistory(picked) {
    state.historyOpen = false;
    state.conversationId = picked.id;
    state.roleId = picked.roleId;
    state.roleName = picked.roleName || picked.title || '返利 AI 助手';
    state.roleAvatar = picked.roleAvatar || '';
    state.messages = [];
    await loadMessages();
  }

  async function newChat() {
    state.conversationId = null;
    state.messages = [];
    state.input = '';
    state.attachments = [];
    await ensureConversation();
  }

  function scrollToEnd() {
    nextTick(() => {
      state.anchor = `ai-msg-${Math.max(0, state.messages.length - 1)}`;
    });
  }

  onLoad(async (options = {}) => {
    if (options.roleId) state.roleId = Number(options.roleId);
    const conversations = await AiChatApi.getConversations();
    const latest = (conversations?.data || [])[0];
    if (latest && !state.roleId) {
      state.conversationId = latest.id;
      state.roleId = latest.roleId;
      state.roleName = latest.roleName || latest.title || '返利 AI 助手';
      state.roleAvatar = latest.roleAvatar || '';
      await loadMessages();
      return;
    }
    await ensureConversation();
  });
  onBeforeUnmount(() => state.abortController?.abort?.());
</script>

<style scoped lang="scss">
  .chat-page {
    display: flex;
    min-height: calc(100vh - 88rpx);
    flex-direction: column;
    background: #f6f8fb;
  }
  .chat-body {
    min-height: 0;
    flex: 1;
  }
  .legacy-page {
    min-height: calc(100vh - 88rpx);
    padding: 28rpx;
    box-sizing: border-box;
    background: #f7f8fa;
  }
  .legacy-welcome {
    padding-top: 90rpx;
    text-align: center;
  }
  .legacy-icon {
    display: inline-flex;
    width: 88rpx;
    height: 88rpx;
    align-items: center;
    justify-content: center;
    border-radius: 24rpx;
    color: #fff;
    background: #103ea6;
    font-weight: 800;
  }
  .legacy-title {
    margin-top: 22rpx;
    color: #22324d;
    font-size: 34rpx;
    font-weight: 700;
  }
  .legacy-copy {
    margin: 12rpx 0 30rpx;
    color: #68778e;
    font-size: 24rpx;
  }
  .legacy-welcome button {
    margin: 14rpx 0;
    border-radius: 14rpx;
    color: #103ea6;
    background: #fff;
    font-size: 24rpx;
  }
  .legacy-messages {
    height: calc(100vh - 270rpx);
    padding-bottom: 120rpx;
    box-sizing: border-box;
  }
  .legacy-row {
    display: flex;
    margin: 18rpx 0;
    justify-content: flex-start;
  }
  .legacy-row view {
    max-width: 78%;
    padding: 18rpx 22rpx;
    border-radius: 16rpx;
    color: #273854;
    background: #fff;
    white-space: pre-wrap;
  }
  .legacy-row.legacy-user {
    justify-content: flex-end;
  }
  .legacy-row.legacy-user view {
    color: #fff;
    background: #103ea6;
  }
  .legacy-composer {
    position: fixed;
    right: 0;
    bottom: 0;
    left: 0;
    display: flex;
    padding: 16rpx 22rpx calc(16rpx + env(safe-area-inset-bottom));
    gap: 12rpx;
    background: #fff;
  }
  .legacy-composer textarea {
    min-height: 68rpx;
    padding: 15rpx;
    border-radius: 14rpx;
    background: #f3f5f8;
    flex: 1;
  }
  .legacy-composer button {
    width: 112rpx;
    height: 68rpx;
    margin: 0;
    border-radius: 14rpx;
    color: #fff;
    background: #103ea6;
    font-size: 24rpx;
    line-height: 68rpx;
  }
</style>
