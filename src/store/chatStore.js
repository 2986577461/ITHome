import { defineStore } from "pinia";

export const chat_store = defineStore("chat", {
    state: () => ({
        socket: null,
        connected: false,
        onlineUsers: [],
        messages: {},
        unreadCounts: {}
    }),
    actions: {

        addMessage(conversationPartnerId, message) { 
            if (!this.messages[conversationPartnerId]) { 
                this.messages[conversationPartnerId] = []; 
            }
            // 确保 read 属性设置正确
            const messageToAdd = { ...message, read: false }; 
            this.messages[conversationPartnerId] = [...this.messages[conversationPartnerId], messageToAdd]; 

            // 更新未读计数
            if (!this.unreadCounts[conversationPartnerId]) {
                this.unreadCounts[conversationPartnerId] = 0;
            }
            this.unreadCounts[conversationPartnerId] += 1;
        },

        // 标记消息为已读
        markMessagesAsRead(userId) {
            if (!this.messages[userId]) return;

            const updatedMessages = this.messages[userId].map(msg => {
                if (!msg.read) {
                    return { ...msg, read: true };
                }
                return msg;
            });

            // 使用数组替换确保响应式
            this.messages[userId] = updatedMessages;

            // 重置未读计数
            this.unreadCounts[userId] = 0;
        },

        addSentMessage(conversationPartnerId, message) { 
            if (!this.messages[conversationPartnerId]) { 
                this.messages[conversationPartnerId] = []; 
            }
            // 确保 read 属性设置正确
            const messageToAdd = { ...message, read: true }; 
            this.messages[conversationPartnerId] = [...this.messages[conversationPartnerId], messageToAdd]; 
        }
    }
});