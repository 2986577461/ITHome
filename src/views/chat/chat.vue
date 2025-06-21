<template>
  <Header></Header>
  <div class="chat-app">
    <!-- 左侧学员列表 -->
    <div class="user-list">
      <div class="search-box">
        <input
          v-model="searchQuery"
          placeholder="搜索学员..."
          class="search-input"
        />
        <svg class="search-icon" viewBox="0 0 24 24">
          <path
            d="M15.5 14h-.79l-.28-.27a6.5 6.5 0 0 0 1.48-5.34c-.47-2.78-2.79-5-5.59-5.34a6.505 6.505 0 0 0-7.27 7.27c.34 2.8 2.56 5.12 5.34 5.59a6.5 6.5 0 0 0 5.34-1.48l.27.28v.79l4.25 4.25c.41.41 1.08.41 1.49 0 .41-.41.41-1.08 0-1.49L15.5 14zm-6 0C7.01 14 5 11.99 5 9.5S7.01 5 9.5 5 14 7.01 14 9.5 11.99 14 9.5 14z"
          />
        </svg>
      </div>
      <div class="user-list-content">
        <div
          v-for="user in filteredUsers"
          :key="user.id"
          :class="['user-item', { active: currentChatUser?.id === user.id }]"
          @click="selectUser(user)"
        >
          <div class="user-avatar">{{ getUserInitial(user.name) }}</div>
          <div class="user-info">
            <div class="user-name">{{ user.name }}</div>
            <div class="user-last-msg">
              {{ getLastMessagePreview(user.id) }}
            </div>
          </div>
          <div class="unread-count" v-if="getUnreadCount(user.id) > 0">
            {{ getUnreadCount(user.id) }}
          </div>
        </div>
      </div>
    </div>

    <!-- 右侧聊天区域 -->
    <div class="chat-area">
      <div class="chat-header">
        <div v-if="currentChatUser" class="current-chat-info">
          <div class="chat-user-avatar">
            {{ getUserInitial(currentChatUser.name) }}
          </div>
          <div class="chat-user-name">{{ currentChatUser.name }}</div>
        </div>
        <div v-else class="select-user-prompt">请选择聊天对象</div>
      </div>

      <div class="message-container" ref="messageContainer">
        <!-- 加载更多按钮 -->
        <div v-if="hasMoreMessages" class="load-more" @click="loadMoreMessages">
          <button class="load-more-btn">加载更多消息</button>
        </div>

        <!-- 消息列表 -->
        <div
          v-for="(message, index) in currentMessages"
          :key="message.id || index"
          :class="[
            'message',
            message.sender === currentUser.id ? 'sent' : 'received',
          ]"
        >
          <div class="message-content">{{ message.content }}</div>
          <div class="message-time">
            {{ formatMessageTime(message.timestamp) }}
          </div>
        </div>
      </div>

      <div class="message-input">
        <input
          v-model="newMessage"
          @keyup.enter="sendMessage"
          placeholder="输入消息..."
          :disabled="!currentChatUser"
        />
        <button
          @click="sendMessage"
          :disabled="!currentChatUser || !newMessage.trim()"
        >
          <svg viewBox="0 0 24 24" class="send-icon">
            <path d="M2.01 21L23 12 2.01 3 2 10l15 2-15 2z" />
          </svg>
        </button>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, onUnmounted, nextTick, watch } from "vue";
import Header from "@/components/Header.vue";
import axios from "axios";
import { getUserList } from "@/axios/axios";
import { user_store } from "@/store/user";

const userStore = user_store();
// 当前用户信息
const currentUser = ref({ id: userStore.studentID });

// 学员列表
const users = ref([]);

// 搜索查询
const searchQuery = ref("");

// 当前选中的聊天用户
const currentChatUser = ref(null);

// 所有聊天记录 { userId: [messages] }
const allMessages = ref({});

// 当前显示的聊天记录
const currentMessages = ref([]);

// 分页控制
const pagination = ref({
  page: 1,
  pageSize: 20,
  total: 0,
});

const hasMoreMessages = computed(() => {
  if (!currentChatUser.value) return false;
  return currentMessages.value.length < pagination.value.total;
});

// 新消息内容
const newMessage = ref("");

// WebSocket连接
const socket = ref(null);
const connected = ref(false);

// DOM引用
const messageContainer = ref(null);

// 过滤后的学员列表
const filteredUsers = computed(() => {
  return users.value.filter((user) =>
    user.name.toLowerCase().includes(searchQuery.value.toLowerCase())
  );
});

// 获取用户首字母
const getUserInitial = (name) => {
  return name ? name.charAt(0).toUpperCase() : "";
};

// 获取最后一条消息预览
const getLastMessagePreview = (userId) => {
  const messages = allMessages.value[userId] || [];
  if (messages.length === 0) return "暂无消息";
  const lastMsg = messages[messages.length - 1];
  return lastMsg.content.length > 15
    ? lastMsg.content.substring(0, 15) + "..."
    : lastMsg.content;
};

// 获取未读消息数
const getUnreadCount = (userId) => {
  const messages = allMessages.value[userId] || [];
  return messages.filter((msg) => msg.sender === userId && !msg.read).length;
};

// 格式化消息时间
const formatMessageTime = (timestamp) => {
  const date = new Date(timestamp);
  return date.toLocaleTimeString([], { hour: "2-digit", minute: "2-digit" });
};

// 选择聊天用户
const selectUser = async (user) => {
  currentChatUser.value = user;
  // 标记消息为已读
  markMessagesAsRead(user.id);
  // 加载历史消息
  // await loadMessages();
  // 滚动到底部
  scrollToBottom();
};

// 加载历史消息
const loadMessages = async () => {
  if (!currentChatUser.value) return;

  try {
    const response = await axios.get("/api/messages", {
      params: {
        userId: currentChatUser.value.id,
        page: pagination.value.page,
        pageSize: pagination.value.pageSize,
      },
    });

    const messages = response.data.messages || [];
    pagination.value.total = response.data.total || 0;

    // 如果是第一页，直接替换
    if (pagination.value.page === 1) {
      currentMessages.value = messages;
      allMessages.value[currentChatUser.value.id] = messages;
    } else {
      // 否则添加到前面
      currentMessages.value = [...messages, ...currentMessages.value];
      allMessages.value[currentChatUser.value.id] = [
        ...messages,
        ...allMessages.value[currentChatUser.value.id],
      ];
    }
  } catch (error) {
    console.error("加载消息失败:", error);
  }
};

// 加载更多消息
const loadMoreMessages = async () => {
  pagination.value.page += 1;
  await loadMessages();

  // 保持滚动位置
  nextTick(() => {
    if (messageContainer.value) {
      const oldHeight = messageContainer.value.scrollHeight;
      const scrollPosition = messageContainer.value.scrollHeight - oldHeight;
      messageContainer.value.scrollTop = scrollPosition;
    }
  });
};

// 标记消息为已读
const markMessagesAsRead = (userId) => {
  if (!allMessages.value[userId]) return;

  allMessages.value[userId] = allMessages.value[userId].map((msg) => {
    if (msg.sender === userId && !msg.read) {
      return { ...msg, read: true };
    }
    return msg;
  });
};

// 发送消息
const sendMessage = async () => {
  if (!newMessage.value.trim() || !currentChatUser.value) return;
  const message = {
    id: Date.now().toString(),
    sender: currentUser.value.id,
    receiver: currentChatUser.value.id,
    content: newMessage.value,
    timestamp: Date.now(),
  };

  // 添加到本地消息列表
  if (!allMessages.value[currentChatUser.value.id]) {
    allMessages.value[currentChatUser.value.id] = [];
  }
  allMessages.value[currentChatUser.value.id].push(message);

  // 通过WebSocket发送消息
  if (socket.value && connected.value) {
    socket.value.send(JSON.stringify(message));
  }
  currentMessages.value = allMessages.value[message.receiver] || [];
  newMessage.value = "";
  scrollToBottom();
};

// 滚动到底部
const scrollToBottom = () => {
  nextTick(() => {
    if (messageContainer.value) {
      messageContainer.value.scrollTop = messageContainer.value.scrollHeight;
    }
  });
};

// 初始化WebSocket连接
const initWebSocket = () => {
  // 替换为你的WebSocket服务器地址
  socket.value = new WebSocket("ws://localhost:8080/ws/" + userStore.studentID);

  socket.value.onopen = () => {
    connected.value = true;
  };

  socket.value.onmessage = (event) => {
    try {
      const message = JSON.parse(event.data);
      // 如果消息是发给当前用户的
      if (message.receiver === currentUser.value.id) {
        const senderId = message.sender;
        // 添加到消息列表
        if (!allMessages.value[senderId]) {
          allMessages.value[senderId] = [];
        }
        allMessages.value[senderId].push(message);
      }
    } catch (e) {
      console.error("解析消息错误:", e);
    }
  };

  socket.value.onclose = () => {
    connected.value = false;
    setTimeout(initWebSocket, 3000);
  };

  socket.value.onerror = (error) => {
    console.error("WebSocket错误:", error);
  };
};

// 监听所有消息变化;
watch(currentChatUser, (newVal) => {
  if (newVal) {
    pagination.value.page = 1;
    currentMessages.value = allMessages.value[newVal.id] || [];
    scrollToBottom();
  }
});

// 生命周期钩子
onMounted(async () => {
  initWebSocket();
  users.value = await getUserList();
});
</script>

<style scoped>
.chat-app {
  display: flex;
  height: 92vh;
  margin: 0 auto;
  background-color: #fff;
  border-radius: 8px;
  box-shadow: 0 2px 10px rgba(0, 0, 0, 0.1);
  overflow: hidden;
}

/* 左侧学员列表 */
.user-list {
  width: 300px;
  border-right: 1px solid #eaeaea;
  display: flex;
  flex-direction: column;
}

.search-box {
  padding: 15px;
  position: relative;
  border-bottom: 1px solid #eaeaea;
}

.search-input {
  width: 80%;
  padding: 10px 15px 10px 35px;
  border: 1px solid #eaeaea;
  border-radius: 20px;
  outline: none;
  font-size: 14px;
  transition: border-color 0.3s;
}

.search-input:focus {
  border-color: #1e88e5;
}

.search-icon {
  position: absolute;
  left: 25px;
  top: 50%;
  transform: translateY(-50%);
  width: 18px;
  height: 18px;
  fill: #999;
}

.user-list-content {
  flex: 1;
  overflow-y: auto;
}

.user-item {
  display: flex;
  padding: 12px 15px;
  align-items: center;
  cursor: pointer;
  transition: background-color 0.2s;
  position: relative;
}

.user-item:hover {
  background-color: #f5f5f5;
}

.user-item.active {
  background-color: #e3f2fd;
}

.user-avatar {
  width: 40px;
  height: 40px;
  border-radius: 50%;
  background-color: #1e88e5;
  color: white;
  display: flex;
  align-items: center;
  justify-content: center;
  font-weight: bold;
  margin-right: 12px;
  flex-shrink: 0;
}

.user-info {
  flex: 1;
  min-width: 0;
}

.user-name {
  font-weight: 500;
  margin-bottom: 3px;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.user-last-msg {
  font-size: 12px;
  color: #666;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.unread-count {
  background-color: #ff3b30;
  color: white;
  border-radius: 50%;
  width: 20px;
  height: 20px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 12px;
  font-weight: bold;
}

/* 右侧聊天区域 */
.chat-area {
  flex: 1;
  display: flex;
  flex-direction: column;
}

.chat-header {
  padding: 15px 20px;
  border-bottom: 1px solid #eaeaea;
  display: flex;
  align-items: center;
}

.current-chat-info {
  display: flex;
  align-items: center;
}

.chat-user-avatar {
  width: 36px;
  height: 36px;
  border-radius: 50%;
  background-color: #1e88e5;
  color: white;
  display: flex;
  align-items: center;
  justify-content: center;
  font-weight: bold;
  margin-right: 12px;
}

.chat-user-name {
  font-weight: 500;
  font-size: 16px;
}

.select-user-prompt {
  color: #666;
  font-style: italic;
}

.message-container {
  flex: 1;
  padding: 20px;
  overflow-y: auto;
  background-color: #f9f9f9;
}

.load-more {
  text-align: center;
  margin-bottom: 15px;
}

.load-more-btn {
  background-color: #e3f2fd;
  color: #1e88e5;
  border: none;
  padding: 8px 15px;
  border-radius: 15px;
  font-size: 13px;
  cursor: pointer;
  transition: background-color 0.2s;
}

.load-more-btn:hover {
  background-color: #bbdefb;
}

.message {
  margin-bottom: 15px;
  display: flex;
  flex-direction: column;
  max-width: 70%;
}

.message.sent {
  align-self: flex-end;
  align-items: flex-end;
}

.message.received {
  align-self: flex-start;
  align-items: flex-start;
}

.message-content {
  padding: 10px 15px;
  border-radius: 18px;
  font-size: 14px;
  line-height: 1.4;
  word-break: break-word;
}

.message.sent .message-content {
  background-color: #1e88e5;
  color: white;
  border-bottom-right-radius: 5px;
}

.message.received .message-content {
  background-color: #e3f2fd;
  color: #333;
  border-bottom-left-radius: 5px;
}

.message-time {
  font-size: 11px;
  color: #999;
  margin-top: 4px;
}

.message-input {
  padding: 15px;
  border-top: 1px solid #eaeaea;
  display: flex;
  align-items: center;
}

.message-input input {
  flex: 1;
  padding: 12px 15px;
  border: 1px solid #eaeaea;
  border-radius: 20px;
  outline: none;
  font-size: 14px;
  transition: border-color 0.3s;
}

.message-input input:focus {
  border-color: #1e88e5;
}

.message-input button {
  margin-left: 10px;
  width: 40px;
  height: 40px;
  border-radius: 50%;
  background-color: #1e88e5;
  color: white;
  border: none;
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  transition: background-color 0.2s;
}

.message-input button:disabled {
  background-color: #bbdefb;
  cursor: not-allowed;
}

.send-icon {
  width: 20px;
  height: 20px;
  fill: currentColor;
}
</style>
