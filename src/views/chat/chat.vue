<template>
  <Header></Header>
  <div class="chat-app">
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
import {
  ref,
  computed,
  onMounted,
  nextTick,
  watch,
  onBeforeUnmount,
} from "vue";
import { user_store } from "@/store/user";
import { baseURL } from "@/axios/axiosInit";
import { ElMessage } from "element-plus";
import Header from "@/components/Header.vue";

const userStore = user_store();
// 当前用户信息
const currentUser = computed(() => ({
  id: userStore.studentID,
  name: userStore.name,
}));
// 学员列表
const users = ref([]);

// 搜索查询
const searchQuery = ref("");

// 当前选中的聊天用户
const currentChatUser = ref(null);

// 所有聊天记录 { userId: [messages] }
// 消息结构：{ sender: Long, receiver: Long, content: String, timestamp: Long }
const allMessages = ref({});

// 当前显示的聊天记录
const currentMessages = ref([]);

// 新消息内容
const newMessage = ref("");

// WebSocket连接
const socket = ref(null);
const connected = ref(false);

// DOM引用
const messageContainer = ref(null);

// WebSocket 重试相关
const MAX_RETRIES = 10; // 最大重试次数
const RETRY_INTERVAL_MS = 1000; // 初始重试间隔，毫秒
let retryCount = 0; // 当前重试次数
let retryTimeoutId = null; // 用于存储 setTimeout 的 ID

// 过滤后的学员列表 - 确保不显示自己
const filteredUsers = computed(() => {
  return users.value.filter(
    (user) =>
      user.id !== currentUser.value.id && // 核心：过滤掉当前用户
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
  if (!timestamp) return "";
  const date = new Date(timestamp);
  // 可以根据需要调整时间格式，例如显示日期和时间
  return date.toLocaleTimeString([], { hour: "2-digit", minute: "2-digit" });
};

// 选择聊天用户
const selectUser = async (user) => {
  currentChatUser.value = user;
  markMessagesAsRead(user.id);
  currentMessages.value = allMessages.value[user.id] || [];
  scrollToBottom();
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
    sender: currentUser.value.id,
    receiver: currentChatUser.value.id,
    content: newMessage.value,
    messageType: "CHAT",
  };

  if (!allMessages.value[currentChatUser.value.id]) {
    allMessages.value[currentChatUser.value.id] = [];
  }
  allMessages.value[currentChatUser.value.id].push(message);

  currentMessages.value = allMessages.value[currentChatUser.value.id];

  if (socket.value && connected.value) {
    socket.value.send(JSON.stringify(message));
  } else {
    ElMessage.error("WebSocket连接未建立，无法发送消息。");
  }

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

// 初始化WebSocket连接（带重试机制）
const initWebSocket = async () => {
  // 如果已经连接，则不再尝试
  if (
    connected.value &&
    socket.value &&
    socket.value.readyState === WebSocket.OPEN
  ) {
    console.log("WebSocket已连接，无需重新建立。");
    return;
  }

  // 每次尝试连接前，清除之前的重试定时器
  if (retryTimeoutId) {
    clearTimeout(retryTimeoutId);
    retryTimeoutId = null;
  }

  if (!userStore.studentID || !userStore.name) {
    console.warn(
      `用户数据未准备好 (ID: ${userStore.studentID}, Name: ${userStore.name})，${RETRY_INTERVAL_MS}ms 后重试...`
    );
    // 如果用户数据未准备好，立即安排下一次重试，不计入 retryCount
    retryTimeoutId = setTimeout(initWebSocket, RETRY_INTERVAL_MS);
    return;
  }

  // 检查是否达到最大重试次数
  if (retryCount >= MAX_RETRIES) {
    console.error("达到最大WebSocket重试次数，连接失败。");
    ElMessage.error("聊天服务连接失败，请刷新页面或稍后重试。");
    return;
  }

  console.log(`尝试建立WebSocket连接 (第 ${retryCount + 1} 次)...`);

  const encodedUserName = encodeURIComponent(userStore.name);
  const wsUrl = `ws://${baseURL}/ws/${userStore.studentID}/${encodedUserName}`;
  // const wsUrl = `ws://${baseURL}/ws/${202300573}/${"靳玉超"}`;

  try {
    socket.value = new WebSocket(wsUrl);

    socket.value.onopen = () => {
      connected.value = true;
      console.log("WebSocket连接已建立！");
      retryCount = 0; // 重置重试次数
    };

    socket.value.onmessage = (event) => {
      try {
        const message = JSON.parse(event.data);

        if (message.messageType === "ONLINE_USER_LIST_UPDATE") {
          console.log("收到在线用户列表更新:", message.onlineUsers);
          users.value = message.onlineUsers || [];
        } else if (message.receiver === currentUser.value.id) {
          const senderId = message.sender;
          if (!allMessages.value[senderId]) {
            allMessages.value[senderId] = [];
          }
          allMessages.value[senderId].push({ ...message, read: false });

          if (currentChatUser.value && currentChatUser.value.id === senderId) {
            currentMessages.value = allMessages.value[senderId];
            scrollToBottom();
            markMessagesAsRead(senderId);
          }
        }
      } catch (e) {
        console.error("解析WebSocket消息错误:", e);
      }
    };

    socket.value.onclose = () => {
      connected.value = false;
      console.warn("WebSocket连接已关闭。");
      // 连接关闭后，如果不是用户主动关闭，尝试重连
      if (retryCount < MAX_RETRIES) {
        retryCount++;
        const delay = RETRY_INTERVAL_MS * Math.pow(2, retryCount - 1); // 指数退避
        console.log(`WebSocket将在 ${delay}ms 后尝试重连...`);
        retryTimeoutId = setTimeout(initWebSocket, delay);
      } else {
        console.error("WebSocket重连失败，达到最大重试次数。");
        ElMessage.error("聊天服务连接断开，请刷新页面或稍后重试。");
      }
    };

    socket.value.onerror = (error) => {
      console.error("WebSocket错误:", error);
      connected.value = false;
      // 错误发生时，也会触发 onclose，所以重连逻辑主要放在 onclose 中处理
      // 这里的 reject 可以在 onMounted 中捕获，用于首次连接失败
    };
  } catch (error) {
    // 这捕获的是 new WebSocket() 自身抛出的同步错误，不常见
    console.error("创建WebSocket对象失败:", error);
    if (retryCount < MAX_RETRIES) {
      retryCount++;
      const delay = RETRY_INTERVAL_MS * Math.pow(2, retryCount - 1);
      console.log(`创建WebSocket失败，将在 ${delay}ms 后尝试重试...`);
      retryTimeoutId = setTimeout(initWebSocket, delay);
    } else {
      ElMessage.error("无法建立WebSocket连接，请检查网络或联系管理员。");
    }
  }
};

// 生命周期钩子
onMounted(() => {
  // 首次尝试连接，会检查用户数据并启动重试机制
  initWebSocket();
});

onBeforeUnmount(() => {
  if (socket.value && socket.value.readyState === WebSocket.OPEN) {
    socket.value.close();
  }
  // 清除任何未执行的重试定时器
  if (retryTimeoutId) {
    clearTimeout(retryTimeoutId);
    retryTimeoutId = null;
  }
});

// 监听 currentChatUser 变化，以更新 currentMessages
watch(currentChatUser, (newVal) => {
  if (newVal) {
    currentMessages.value = allMessages.value[newVal.id] || [];
    scrollToBottom();
  } else {
    currentMessages.value = [];
  }
});
</script>

<style scoped>
/* 你的 CSS 样式保持不变 */
.chat-app {
  display: flex;
  height: calc(100vh - 60px); /* 假设 Header 高度 60px */
  border: 1px solid #ccc;
  overflow: hidden;
}

.user-list {
  width: 280px;
  border-right: 1px solid #eee;
  padding: 10px;
  box-sizing: border-box;
  display: flex;
  flex-direction: column;
}

.search-box {
  position: relative;
  margin-bottom: 15px;
}

.search-input {
  width: 100%;
  padding: 10px 10px 10px 40px; /* 留出左侧空间给图标 */
  border: 1px solid #ddd;
  border-radius: 20px;
  font-size: 1em;
  box-sizing: border-box;
}

.search-icon {
  position: absolute;
  left: 10px;
  top: 50%;
  transform: translateY(-50%);
  width: 20px;
  height: 20px;
  fill: #888;
}

.user-list-content {
  flex-grow: 1;
  overflow-y: auto;
  -ms-overflow-style: none; /* IE and Edge */
  scrollbar-width: none; /* Firefox */
}

.user-list-content::-webkit-scrollbar {
  display: none; /* Chrome, Safari, Opera*/
}

.user-item {
  display: flex;
  align-items: center;
  padding: 12px 10px;
  border-bottom: 1px solid #f0f0f0;
  cursor: pointer;
  transition: background-color 0.2s ease;
}

.user-item:hover {
  background-color: #f5f5f5;
}

.user-item.active {
  background-color: #e6f7ff; /* 选中状态背景色 */
  border-left: 3px solid #007bff; /* 选中状态左边框 */
  padding-left: 7px; /* 调整填充以适应边框 */
}

.user-avatar {
  width: 45px;
  height: 45px;
  border-radius: 50%;
  background-color: #007bff;
  color: white;
  display: flex;
  justify-content: center;
  align-items: center;
  font-weight: bold;
  font-size: 1.2em;
  margin-right: 12px;
  flex-shrink: 0; /* 防止头像被压缩 */
}

.user-info {
  flex-grow: 1;
  overflow: hidden; /* 隐藏溢出内容 */
}

.user-name {
  font-weight: bold;
  font-size: 1.1em;
  color: #333;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.user-last-msg {
  font-size: 0.9em;
  color: #888;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
  margin-top: 2px;
}

.unread-count {
  background-color: #ff4d4f; /* 红色 */
  color: white;
  border-radius: 12px; /* 更圆 */
  padding: 4px 8px;
  font-size: 0.8em;
  margin-left: 10px;
  min-width: 24px; /* 最小宽度，保证圆形 */
  text-align: center;
  flex-shrink: 0;
}

.chat-area {
  flex-grow: 1;
  display: flex;
  flex-direction: column;
}

.chat-header {
  padding: 15px 20px;
  border-bottom: 1px solid #eee;
  background-color: #f7f7f7;
  display: flex;
  align-items: center;
}

.current-chat-info {
  display: flex;
  align-items: center;
}

.chat-user-avatar {
  width: 40px;
  height: 40px;
  border-radius: 50%;
  background-color: #1890ff;
  color: white;
  display: flex;
  justify-content: center;
  align-items: center;
  font-weight: bold;
  margin-right: 10px;
  font-size: 1.1em;
}

.chat-user-name {
  font-weight: bold;
  font-size: 1.2em;
  color: #333;
}

.select-user-prompt {
  flex-grow: 1;
  text-align: center;
  color: #888;
  font-size: 1.1em;
}

.message-container {
  flex-grow: 1;
  padding: 15px 20px;
  overflow-y: auto;
  background-color: #fdfdfd;
  display: flex;
  flex-direction: column; /* 保持消息从上到下排列 */
  gap: 10px; /* 消息间距 */
}

/* 加载更多按钮样式移除 */
.load-more {
  text-align: center;
  margin-bottom: 10px;
}

.load-more-btn {
  background-color: #e9e9e9;
  border: none;
  padding: 8px 15px;
  border-radius: 5px;
  cursor: pointer;
  font-size: 0.9em;
  color: #555;
  transition: background-color 0.2s;
}

.load-more-btn:hover {
  background-color: #ddd;
}

.message {
  display: flex;
  flex-direction: column;
  max-width: 60%;
  padding: 10px 15px;
  border-radius: 15px;
  word-wrap: break-word;
  box-shadow: 0 1px 2px rgba(0, 0, 0, 0.1);
}

.message.sent {
  align-self: flex-end; /* 右对齐 */
  background-color: #e1ffc7; /* 发送消息的背景色 */
  color: #333;
  border-bottom-right-radius: 2px; /* 模拟聊天气泡尖角 */
}

.message.received {
  align-self: flex-start; /* 左对齐 */
  background-color: #ffffff; /* 接收消息的背景色 */
  color: #333;
  border-bottom-left-radius: 2px; /* 模拟聊天气泡尖角 */
}

.message-content {
  font-size: 1em;
  margin-bottom: 5px;
}

.message-time {
  font-size: 0.75em;
  color: #999;
  text-align: right; /* 时间右对齐 */
}

.message-input {
  display: flex;
  padding: 15px 20px;
  border-top: 1px solid #eee;
  background-color: #f7f7f7;
}

.message-input input {
  flex-grow: 1;
  padding: 10px 15px;
  border: 1px solid #ddd;
  border-radius: 20px;
  font-size: 1em;
  margin-right: 10px;
  box-shadow: inset 0 1px 2px rgba(0, 0, 0, 0.05);
}

.message-input button {
  background-color: #007bff;
  color: white;
  border: none;
  border-radius: 20px;
  padding: 10px 20px;
  cursor: pointer;
  font-size: 1em;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: background-color 0.2s ease;
}

.message-input button:hover:not(:disabled) {
  background-color: #0056b3;
}

.message-input button:disabled {
  background-color: #cccccc;
  cursor: not-allowed;
}

.send-icon {
  width: 20px;
  height: 20px;
  fill: white;
}
</style>
