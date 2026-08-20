import request from '@/sheep/request';

const AiChatApi = {
  getConversations: () => request({ url: '/ai/chat/conversation/list', method: 'GET', custom: { showLoading: false, auth: true } }),
  getConversation: (id) => request({ url: '/ai/chat/conversation/get', method: 'GET', params: { id }, custom: { showLoading: false, auth: true } }),
  createConversation: ({ roleId } = {}) => request({ url: '/ai/chat/conversation/create', method: 'POST', data: { roleId }, custom: { auth: true } }),
  getMessages: (conversationId) => request({ url: '/ai/chat/message/list', method: 'GET', params: { conversationId }, custom: { showLoading: false, auth: true } }),
  send: ({ conversationId, content }) => request({ url: '/ai/chat/message/send', method: 'POST', timeout: 60000, data: { conversationId, content, useContext: true, useSearch: false, attachmentUrls: [] }, custom: { auth: true } }),
};

export default AiChatApi;
