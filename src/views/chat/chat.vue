<template>
  <Header/>
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
            :key="user.studentId"
            :class="['user-item', { active: currentChatUser?.studentId === user.studentId }]"
            @click="selectUser(user)"
        >
          <!-- 用户列表的头像，显示用户首字母 -->
          <div class="user-avatar">{{ getUserInitial(user.name) }}</div>
          <div class="user-info">
            <div class="user-name">{{ user.name }}</div>
            <div class="user-last-msg">
              {{ getLastMessagePreview(user.studentId) }}
            </div>
          </div>
          <div
              class="unread-count"
              v-if="chatStore.unreadCounts[user.studentId] > 0"
          >
            {{ chatStore.unreadCounts[user.studentId] }}
          </div>
        </div>
      </div>
    </div>

    <div class="chat-area">
      <div class="message-container" ref="messageContainer">
        <div
            v-for="(message, index) in currentMessages"
            :key="index"
            :class="[
            'message',
            message.senderId === currentUser.studentId ? 'sent' : 'received'
          ]"
        >
          <!-- 消息中的头像 -->
          <div class="message-avatar">
            <!-- 根据消息发送者显示头像 -->
            {{ getUserInitial(getMessageSenderName(message)) }}
          </div>
          <div class="message-bubble">
            <div class="message-content">{{ message.content }}</div>
            <div class="message-time">
              {{ formatMessageTime(message.timestamp) }}
            </div>
          </div>
        </div>
      </div>
      <div class="message-input">
        <input
            v-model="newMessage"
            @keyup.enter="sendMessage"
            placeholder="输入消息..."
            :disabled="!currentChatUser"
            @click="scrollToBottom"
        />
        <button
            @click="sendMessage"
            :disabled="!currentChatUser || !newMessage.trim()"
        >
          <svg viewBox="0 0 24 24" class="send-icon">
            <path d="M2.01 21L23 12 2.01 3 2 10l15 2-15 2z"/>
          </svg>
        </button>
      </div>
    </div>
  </div>
</template>

<script setup>
import {ref, computed, nextTick, watch, onMounted} from 'vue'
import {user_store} from '@/store/user'
import {chat_store} from '@/store/chatStore'
import Header from '@/components/Header.vue'
import {ElMessage} from "element-plus";

// Store
const userStore = user_store()
const chatStore = chat_store()

// Refs
const searchQuery = ref('')
const currentChatUser = ref(null)
const newMessage = ref('')
const messageContainer = ref(null)

// Current User Info
const currentUser = computed(() => ({
  studentId: userStore.studentId,
  name: userStore.name
}))

// Computed Properties
const filteredUsers = computed(() => {
  return chatStore.onlineUsers.filter(user =>
      user.studentId !== currentUser.value.studentId &&
      user.name.toLowerCase().includes(searchQuery.value.toLowerCase())
  )
})

const currentMessages = computed(() => {
  // 移除这里的 scrollToBottom()，因为它会导致无限循环或不必要的滚动
  // 滚动应该由 watch currentMessages 或 currentChatUser 触发
  if (!currentChatUser.value) return []
  return chatStore.messages[currentChatUser.value.studentId] || []
})

// Methods
const getUserInitial = (name) => {
  return name ? String(name).charAt(0).toUpperCase() : '' // 确保 name 是字符串
}

// 新增方法：获取消息发送者的名字，用于显示头像首字母
const getMessageSenderName = (message) => {
  if (message.senderId === currentUser.value.studentId) {
    return currentUser.value.name; // 当前用户发送的消息
  } else {
    // 查找聊天对象列表，找到发送者名字
    const sender = chatStore.onlineUsers.find(user => user.studentId === message.senderId);
    return sender ? sender.name : '未知'; // 如果找不到，显示“未知”
  }
}

const getLastMessagePreview = (userId) => {
  const messages = chatStore.messages[userId] || []
  if (messages.length === 0) return '暂无消息'
  const lastMsg = messages[messages.length - 1]
  return lastMsg.content.length > 15
      ? lastMsg.content.substring(0, 15) + '...'
      : lastMsg.content
}

const formatMessageTime = (timestamp) => {
  if (!timestamp) return ''
  return new Date(timestamp).
  toLocaleTimeString([], {
    month:'long',
    day:  '2-digit',
    hour: '2-digit',
    minute: '2-digit'
  })
}

const selectUser = (user) => {
  currentChatUser.value = user
  chatStore.markMessagesAsRead(user.studentId)
  nextTick(scrollToBottom)
}

const sendMessage = () => {
  if (!newMessage.value.trim() || !currentChatUser.value) return;

  const messageToSend = {
    senderId: currentUser.value.studentId,
    receiverId: currentChatUser.value.studentId,
    content: newMessage.value,
  };

  const messageForLocalStore = {
    id: Date.now(), // 临时ID，如果后端返回真实ID可以更新
    senderId: currentUser.value.studentId,
    receiverId: currentChatUser.value.studentId,
    content: newMessage.value,
    timestamp: new Date().toISOString() // 本地时间戳
  };

  chatStore.addSentMessage(currentChatUser.value.studentId, messageForLocalStore);

  if (chatStore.socket && chatStore.connected) {
    try {
      chatStore.socket.send(JSON.stringify(messageToSend));
    } catch (error) {
      console.error("发送消息失败:", error);
      ElMessage.error("消息发送失败，请检查连接");
    }
  } else {
    ElMessage.error("WebSocket连接未建立，无法发送消息");
  }

  newMessage.value = '';
  nextTick(scrollToBottom);
};

const scrollToBottom = () => {
  if (messageContainer.value) {
    messageContainer.value.scrollTop = messageContainer.value.scrollHeight
  }
}

onMounted(() => {
  nextTick(scrollToBottom); // 初始挂载时滚动到底部
});

// 监听在线用户列表变化，实现自动选择第一个聊天对象
watch(() => chatStore.onlineUsers, (newUsers) => {
  // 延迟一小段时间确保DOM更新和消息加载
  setTimeout(() => {
    if (!currentChatUser.value && newUsers.length > 0) {
      selectUser(newUsers[0]);
    }
  }, 100); // 稍微缩短延迟，如果仍然有问题可以再调整
}, {deep: true, immediate: true});

// 监听当前聊天用户变化，用于标记已读和滚动到底部
watch(currentChatUser, (newUser) => {
  if (newUser) {
    chatStore.markMessagesAsRead(newUser.studentId)
    nextTick(scrollToBottom)
  }
});

// 监听 currentMessages 变化，确保新消息到来时滚动到底部
watch(currentMessages, () => {
  nextTick(scrollToBottom);
}, { deep: true }); // deep: true 确保能检测到数组内部元素的变化
</script>

<style scoped>
/* 整体布局 */
.chat-app {
  display: flex;
  height: calc(99vh - 60px); /* 减去 Header 的高度 */
  background-color: #f0f2f5; /* 整体背景色 */
  border-radius: 12px;
  overflow: hidden; /* 确保圆角和背景图片在边界内 */
  box-shadow: 0 4px 20px rgba(0, 0, 0, 0.1); /* 添加阴影效果 */
}

/* 用户列表区域 */
.user-list {
  width: 300px;
  border-right: 1px solid #e0e0e0;
  display: flex;
  flex-direction: column;
  background-color: #ffffff; /* 用户列表背景色 */
  padding: 10px 0;
}

.search-box {
  padding: 15px;
  position: relative;
  border-bottom: 1px solid #f0f0f0;
}

.search-input {
  width: 80%;
  padding: 10px 15px 10px 40px; /* 增加左侧 padding 为搜索图标留出空间 */
  border-radius: 25px; /* 更圆润的边角 */
  border: 1px solid #d0d0d0;
  outline: none;
  font-size: 15px;
  transition: border-color 0.3s ease;
}

.search-input:focus {
  border-color: #4a89dc;
}

.search-icon {
  position: absolute;
  left: 25px;
  top: 25px; /* 调整图标位置 */
  width: 18px; /* 稍微增大图标 */
  height: 18px;
  fill: #999;
}

.user-list-content {
  flex: 1;
  overflow-y: auto;
  padding-top: 10px; /* 顶部留白 */
}

.user-item {
  display: flex;
  padding: 12px 15px;
  cursor: pointer;
  align-items: center;
  border-bottom: 1px solid #f5f5f5;
  transition: background-color 0.2s ease;
}

.user-item:hover {
  background-color: #eef2f7; /* 悬停背景色 */
}

.user-item.active {
  background-color: #e0eaf6; /* 选中背景色 */
  border-left: 4px solid #4a89dc; /* 左侧强调边框 */
  padding-left: 11px; /* 调整 padding 以适应边框 */
}

.user-avatar {
  width: 48px; /* 增大头像尺寸 */
  height: 48px;
  border-radius: 50%;
  background-color: #4a89dc; /* 蓝色背景 */
  color: white;
  display: flex;
  align-items: center;
  justify-content: center;
  margin-right: 15px; /* 增大右侧间距 */
  font-weight: bold;
  font-size: 20px; /* 增大字体 */
  flex-shrink: 0; /* 防止头像被压缩 */
  box-shadow: 0 2px 5px rgba(0, 0, 0, 0.1); /* 添加头像阴影 */
}

.user-info {
  flex: 1;
  min-width: 0; /* 允许内容在溢出时截断 */
}

.user-name {
  font-weight: 600; /* 更粗的字体 */
  margin-bottom: 4px;
  color: #333;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.user-last-msg {
  color: #777;
  font-size: 14px; /* 调整字体大小 */
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.unread-count {
  background-color: #ff4d4f; /* 红色背景 */
  color: white;
  border-radius: 12px; /* 更圆润的边角 */
  min-width: 24px; /* 最小宽度 */
  height: 24px; /* 高度 */
  font-size: 13px; /* 字体大小 */
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 0 8px; /* 左右内边距 */
  margin-left: 10px; /* 左侧间距 */
}

/* 聊天区域 */
.chat-area {
  flex: 1;
  display: flex;
  flex-direction: column;
  background-color: #f5f5f5; /* 默认背景色，将被图片覆盖 */
  background-image:
      linear-gradient(rgba(255, 255, 255, 0.4), rgba(255, 255, 255, 0.3)), /* 调整为白色半透明覆盖 */
      url('https://liuxingjielihaichuanlai.oss-cn-chengdu.aliyuncs.com/%E3%80%90%E5%93%B2%E9%A3%8E%E5%A3%81%E7%BA%B8%E3%80%91%E6%99%B4%E7%A9%BA-%E6%B2%BB%E6%84%88-%E7%8C%AB.png');
  background-size: cover;
  background-position: center;
  background-repeat: no-repeat;
  background-attachment: fixed;
}






.message-container {
  flex: 1;
  padding: 20px; /* 增加内边距 */
  overflow-y: auto;
  /* 隐藏滚动条 */
  scrollbar-width: none; /* Firefox */
  -ms-overflow-style: none; /* IE/Edge */
}

.message-container::-webkit-scrollbar {
  width: 0;
  height: 0;
  display: none; /* Webkit */
}

/* 消息样式 */
.message {
  display: flex; /* 使用 flex 布局 */
  align-items: flex-start; /* 顶部对齐 */
  margin-bottom: 20px; /* 增加消息间距 */
  max-width: 80%; /* 增加最大宽度 */
}

.message.sent {
  margin-left: auto; /* 推到右边 */
  flex-direction: row-reverse; /* 头像和内容反向排列 */
}

.message.received {
  margin-right: auto; /* 推到左边 */
  flex-direction: row; /* 头像和内容正常排列 */
}

.message-avatar {
  width: 40px;
  height: 40px;
  border-radius: 50%;
  background-color: #4a89dc; /* 默认蓝色 */
  color: white;
  display: flex;
  align-items: center;
  justify-content: center;
  font-weight: bold;
  font-size: 18px; /* 字体大小 */
  flex-shrink: 0; /* 防止头像被压缩 */
  box-shadow: 0 2px 5px rgba(0, 0, 0, 0.1); /* 添加阴影 */
}

.message.sent .message-avatar {
  margin-left: 10px; /* 发送消息时，头像在右，左侧有间距 */
  margin-right: 0;
  background-color: #8bc34a; /* 发送者头像使用绿色 */
}

.message.received .message-avatar {
  margin-right: 10px; /* 接收消息时，头像在左，右侧有间距 */
  margin-left: 0;
  background-color: #466bff; /* 接收者头像使用红色 */
}

.message-bubble {
  display: flex;
  flex-direction: column;
  max-width: calc(100% - 60px); /* 减去头像和间距的宽度 */
}

.message-content {
  padding: 12px 18px; /* 增大内边距 */
  border-radius: 18px; /* 更圆润的边角 */
  word-break: break-word;
  line-height: 1.5; /* 行高 */
  font-size: 15px;
  box-shadow: 0 2px 5px rgba(0, 0, 0, 0.1); /* 消息气泡阴影 */
}

.message.sent .message-content {
  background-color: #4a89dc; /* 发送消息气泡蓝色 */
  color: white;
}

.message.received .message-content {
  background-color: #ffffff; /* 接收消息气泡白色 */
  color: #333;
  border: 1px solid #e0e0e0; /* 接收消息气泡边框 */
}

.message-time {
  font-size: 12px; /* 字体更小 */
  color: #050505;
  margin-top: 5px;
  white-space: nowrap; /* 防止时间换行 */
}

.message.sent .message-time {
  text-align: right; /* 发送消息时时间右对齐 */
}

.message.received .message-time {
  text-align: left; /* 接收消息时时间左对齐 */
}

/* 消息输入区域 */
.message-input {
  padding: 15px 20px; /* 增加内边距 */
  display: flex;
  align-items: center;
}

.message-input input {
  flex: 1;
  padding: 12px 20px; /* 增大内边距 */
  border-radius: 25px; /* 更圆润的边角 */
  border: 1px solid #d0d0d0;
  outline: none;
  margin-right: 15px; /* 增大右侧间距 */
  font-size: 16px;
  transition: border-color 0.3s ease;
}

.message-input input:focus {
  border-color: #4a89dc;
}

.message-input button {
  width: 48px; /* 增大按钮尺寸 */
  height: 48px;
  border-radius: 50%;
  background-color: #4a89dc;
  color: white;
  border: none;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0; /* 防止按钮被压缩 */
  box-shadow: 0 2px 5px rgba(0, 0, 0, 0.1); /* 按钮阴影 */
  transition: background-color 0.3s ease;
}

.message-input button:hover:not(:disabled) {
  background-color: #3a7bdc; /* 悬停效果 */
}

.message-input button:disabled {
  background-color: #ccc;
  cursor: not-allowed;
  box-shadow: none;
}

.send-icon {
  width: 24px; /* 增大图标尺寸 */
  height: 24px;
  fill: white;
}
</style>
