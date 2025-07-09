<template>
  <Header />
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
            :key="index"
            :class="[
            'message',
            message.sender === currentUser.studentId ? 'sent' : 'received'
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
            <path d="M2.01 21L23 12 2.01 3 2 10l15 2-15 2z"/>
          </svg>
        </button>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, nextTick, watch } from 'vue'
import { user_store } from '@/store/user'
import { chat_store } from '@/store/chatStore'
import Header from '@/components/Header.vue'
import { ElMessage } from "element-plus";

// Store
const userStore = user_store()
const chatStore = chat_store()

// Refs
const searchQuery = ref('')
const currentChatUser = ref(null)
const newMessage = ref('')
const messageContainer = ref(null)

// 新增等待状态
const isWsInitializing = ref(true)

// 当前用户信息
const currentUser = computed(() => ({
  studentId: userStore.studentId,
  name: userStore.name
}))

// 计算属性
const filteredUsers = computed(() => {
  return chatStore.onlineUsers.filter(user =>
      user.studentId !== currentUser.value.studentId &&
      user.name.toLowerCase().includes(searchQuery.value.toLowerCase())
  )
})

const currentMessages = computed(() => {
  if (!currentChatUser.value) return []
  return chatStore.messages[currentChatUser.value.studentId] || []
})

// 方法
const getUserInitial = (name) => {
  return name ? name.charAt(0).toUpperCase() : ''
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
  return new Date(timestamp).toLocaleTimeString([], {
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
  if (!newMessage.value.trim() || !currentChatUser.value) return

  const message = {
    sender: currentUser.value.studentId,
    receiver: currentChatUser.value.studentId,
    content: newMessage.value,
    timestamp: Date.now(),
    messageType: 'CHAT'
  }

  // 添加到本地消息列表
  chatStore.addSentMessage(currentChatUser.value.studentId, message)

  // 发送WebSocket消息
  if (chatStore.socket && chatStore.connected) {
    try {
      chatStore.socket.send(JSON.stringify(message))
    } catch (error) {
      console.error("发送消息失败:", error)
      ElMessage.error("消息发送失败，请检查连接")
    }
  } else {
    ElMessage.error("WebSocket连接未建立，无法发送消息")
  }

  newMessage.value = ''
  nextTick(scrollToBottom)
}

const scrollToBottom = () => {
  if (messageContainer.value) {
    messageContainer.value.scrollTop = messageContainer.value.scrollHeight
  }
}

onMounted(async () => {
  // 等待WebSocket初始化
  const maxAttempts = 10
  let attempts = 0

  while (!chatStore.socket && attempts < maxAttempts) {
    await new Promise(resolve => setTimeout(resolve, 300))
    attempts++
  }

  if (chatStore.socket) {
    isWsInitializing.value = false

    const messageHandler = (event) => {
      try {
        const message = JSON.parse(event.data)

        if (message.messageType === 'ONLINE_USER_LIST_UPDATE') {
          chatStore.updateOnlineUsers(message.onlineUsers || [])
          return
        }

        if (message.receiver === currentUser.value.studentId) {
          chatStore.addMessage(message.sender, message)

          if (currentChatUser.value?.studentId === message.sender) {
            chatStore.markMessagesAsRead(message.sender)
          }

          scrollToBottom()
        }
      } catch (e) {
        console.error('消息解析错误:', e)
      }
    }

    chatStore.socket.addEventListener('message', messageHandler)

    return () => {
      chatStore.socket?.removeEventListener('message', messageHandler)
    }
  } else {
    console.warn("WebSocket初始化超时")
    isWsInitializing.value = false
  }

  scrollToBottom()
})

// 监听在线用户列表变化
watch(() => chatStore.onlineUsers, (newUsers) => {
  console.log("在线用户列表已更新:", newUsers)
}, { deep: true })

// 监听当前聊天用户变化
watch(currentChatUser, (newUser) => {
  if (newUser) {
    chatStore.markMessagesAsRead(newUser.studentId)
    nextTick(scrollToBottom)
  }
})
</script>

<style scoped>
/* 原有的样式保持不变 */
.chat-app {
  display: flex;
  height: calc(100vh - 60px);
}

.user-list {
  width: 300px;
  border-right: 1px solid #eee;
  display: flex;
  flex-direction: column;
}

.search-box {
  padding: 15px;
  position: relative;
}

.search-input {
  width: 100%;
  padding: 8px 15px 8px 35px;
  border-radius: 20px;
  border: 1px solid #ddd;
  outline: none;
}

.search-icon {
  position: absolute;
  left: 25px;
  top: 23px;
  width: 16px;
  height: 16px;
  fill: #999;
}

.user-list-content {
  flex: 1;
  overflow-y: auto;
}

.user-item {
  display: flex;
  padding: 12px 15px;
  cursor: pointer;
  align-items: center;
  border-bottom: 1px solid #f5f5f5;
}

.user-item:hover {
  background-color: #f9f9f9;
}

.user-item.active {
  background-color: #eef6ff;
}

.user-avatar {
  width: 40px;
  height: 40px;
  border-radius: 50%;
  background-color: #4a89dc;
  color: white;
  display: flex;
  align-items: center;
  justify-content: center;
  margin-right: 12px;
  font-weight: bold;
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
  color: #999;
  font-size: 13px;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.unread-count {
  background-color: #ff4d4f;
  color: white;
  border-radius: 10px;
  min-width: 20px;
  height: 20px;
  font-size: 12px;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 0 5px;
}

.chat-area {
  flex: 1;
  display: flex;
  flex-direction: column;
}

.chat-header {
  padding: 15px;
  border-bottom: 1px solid #eee;
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
  background-color: #4a89dc;
  color: white;
  display: flex;
  align-items: center;
  justify-content: center;
  margin-right: 12px;
  font-weight: bold;
}

.chat-user-name {
  font-weight: 500;
}

.select-user-prompt {
  color: #999;
}

.message-container {
  flex: 1;
  padding: 15px;
  overflow-y: auto;
  background-color: #f5f5f5;
}

.message {
  margin-bottom: 15px;
  max-width: 70%;
}

.message.sent {
  margin-left: auto;
  text-align: right;
}

.message.received {
  margin-right: auto;
}

.message-content {
  display: inline-block;
  padding: 10px 15px;
  border-radius: 18px;
  background-color: white;
  word-break: break-word;
}

.message.sent .message-content {
  background-color: #4a89dc;
  color: white;
}

.message-time {
  font-size: 12px;
  color: #999;
  margin-top: 5px;
}

.message-input {
  padding: 15px;
  border-top: 1px solid #eee;
  display: flex;
}

.message-input input {
  flex: 1;
  padding: 10px 15px;
  border-radius: 20px;
  border: 1px solid #ddd;
  outline: none;
  margin-right: 10px;
}

.message-input button {
  width: 40px;
  height: 40px;
  border-radius: 50%;
  background-color: #4a89dc;
  color: white;
  border: none;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
}

.message-input button:disabled {
  background-color: #ccc;
  cursor: not-allowed;
}

.send-icon {
  width: 20px;
  height: 20px;
  fill: white;
}
</style>