import request from '@/sheep/request';

const AiChatApi = {
  getRoles: () => request({ url: '/ai/chat/role/simple-list', method: 'GET', custom: { showLoading: false } }),
  getConversations: () => request({ url: '/ai/chat/conversation/list', method: 'GET', custom: { showLoading: false, auth: true } }),
  createConversation: ({ roleId, knowledgeId } = {}) => request({ url: '/ai/chat/conversation/create', method: 'POST', data: { roleId, knowledgeId }, custom: { auth: true } }),
  getMessages: (conversationId) => request({ url: '/ai/chat/message/list', method: 'GET', params: { conversationId }, custom: { showLoading: false, auth: true } }),
  send: ({ conversationId, content }) => request({ url: '/ai/chat/message/send', method: 'POST', data: { conversationId, content, useContext: true, useSearch: false, attachmentUrls: [] }, custom: { auth: true } }),
};

export default AiChatApi;
