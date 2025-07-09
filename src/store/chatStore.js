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
        // 更新在线用户列表（确保响应式）
        updateOnlineUsers(users) {
            // 使用数组替换确保响应式更新
            this.onlineUsers = [...users];
        },

        // 添加新消息
        addMessage(senderId, message) {
            if (!this.messages[senderId]) {
                this.messages[senderId] = [];
            }

            const newMessage = {
                ...message,
                read: false
            };

            // 使用数组操作确保响应式
            this.messages[senderId] = [...this.messages[senderId], newMessage];

            // 更新未读计数
            if (!this.unreadCounts[senderId]) {
                this.unreadCounts[senderId] = 0;
            }
            this.unreadCounts[senderId] += 1;
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

        // 添加发送的消息
        addSentMessage(receiverId, message) {
            if (!this.messages[receiverId]) {
                this.messages[receiverId] = [];
            }

            const newMessage = {
                ...message,
                read: true
            };

            // 使用数组操作确保响应式
            this.messages[receiverId] = [...this.messages[receiverId], newMessage];
        }
    }
});