<template>
  <div class="app-container">
    <!-- 头部导航 -->
    <Header></Header>

    <!-- 主内容区 -->
    <main class="chat-container">
      <!-- 对话历史 -->
      <div class="chat-history" ref="chatHistory">
        <div
          v-for="(message, index) in messages"
          :key="index"
          :class="['message', message.role]"
        >
          <div class="message-avatar">
            <img
              :src="message.role === 'user' ? userAvatar : botAvatar"
              :alt="message.role"
            />
          </div>
          <div class="message-content">
            <vue-markdown
              :source="message.content"
              class="markdown-content"
            ></vue-markdown>
            <div class="message-time">{{ formatTime(message.timestamp) }}</div>
          </div>
        </div>
        <div v-if="loading" class="loading-indicator">
          <div class="loading-dots">
            <div></div>
            <div></div>
            <div></div>
          </div>
        </div>
      </div>

      <!-- 输入区 -->
      <div class="input-area">
        <textarea
          v-model="userInput"
          @keydown.enter.exact.prevent="sendMessage"
          placeholder="输入您的问题..."
          rows="2"
        ></textarea>
        <button @click="sendMessage" :disabled="loading"></button>
      </div>
    </main>

    <!-- 科技感装饰元素 -->
    <div class="tech-circle circle-1"></div>
    <div class="tech-circle circle-2"></div>
    <div class="tech-circle circle-3"></div>
  </div>
</template>

<script setup>
import axios from "axios";
import VueMarkdown from "vue-markdown-render";
import { ref, onMounted, nextTick } from "vue";
import Header from "@/components/Header.vue";

const userInput = ref("");
const messages = ref([]);
const loading = ref(false);
const chatHistory = ref(null);

// 头像URL - 可以使用实际URL或base64编码图片
const userAvatar = ref(
  "data:image/svg+xml;base64,PHN2ZyB4bWxucz0iaHR0cDovL3d3dy53My5vcmcvMjAwMC9zdmciIHZpZXdCb3g9IjAgMCAyNCAyNCI+PHBhdGggZD0iTTEyIDJDNi40NzcgMiAyIDYuNDc3IDIgMTJzNC40NzcgMTAgMTAgMTAgMTAtNC40NzcgMTAtMTBTMTcuNTIzIDIgMTIgMnptMCAyYzQuNDE4IDAgOCAzLjU4MiA4IDhzLTMuNTgyIDgtOCA4LTgtMy41ODItOC04IDMuNTgyLTggOC04eiIvPjxwYXRoIGQ9Ik0xMiAxMWMxLjY1NyAwIDMtMS4zNDMgMy0zcy0xLjM0My0zLTMtMy0zIDEuMzQzLTMgMyAxLjM0MyAzIDMgM3ptMCAxYy0yLjIwOSAwLTYgMS4wMzktNiAzdjFoLDEydi0xYzAtMS45NjEtMy43OTEtMy02LTN6Ii8+PC9zdmc+"
);
const botAvatar = ref(
  "data:image/svg+xml;base64,PHN2ZyB4bWxucz0iaHR0cDovL3d3dy53My5vcmcvMjAwMC9zdmciIHZpZXdCb3g9IjAgMCAyNCAyNCI+PHBhdGggZD0iTTIwIDloLTJWN2MwLTEuMS0uOS0yLTItMmgtMlYzYzAtMS4xLS45LTItMi0ySDhjLTEuMSAwLTIgLjktMiAydjJINGMtMS4xIDAtMiAuOS0yIDJ2MkgxdjZoMnYyaC0ydjZoMnYyaDExdi0yaDJ2LTZoLTJ2LTJoMnYtNkg0VjdoMTJ2Mmg0Vjl6TTggNEgxNnYySDhWNHptMCAxNnYtMkgxNnYySDh6bTAtNkgxNnYySDh2LTJ6Ii8+PC9zdmc+"
);

// 发送消息到后端
const sendMessage = async () => {
  if (!userInput.value.trim() || loading.value) return;

  const userMessage = {
    role: "user",
    content: userInput.value,
    timestamp: new Date(),
  };

  messages.value.push(userMessage);
  userInput.value = "";

  try {
    loading.value = true;
    scrollToBottom();

    const response = await axios.post("http://your-springboot-api.com/chat", {
      message: userMessage.content,
      history: messages.value.slice(-5), // 发送最近5条历史
    });

    const botMessage = {
      role: "assistant",
      content: response.data.reply,
      timestamp: new Date(),
    };

    messages.value.push(botMessage);
  } catch (error) {
    console.error("API请求失败:", error);
    messages.value.push({
      role: "assistant",
      content: "抱歉，请求服务时出现错误，请稍后再试。",
      timestamp: new Date(),
    });
  } finally {
    loading.value = false;
    scrollToBottom();
  }
};

// 格式化时间
const formatTime = (date) => {
  return new Date(date).toLocaleTimeString([], {
    hour: "2-digit",
    minute: "2-digit",
  });
};

// 滚动到底部
const scrollToBottom = () => {
  nextTick(() => {
    if (chatHistory.value) {
      chatHistory.value.scrollTop = chatHistory.value.scrollHeight;
    }
  });
};

// 初始化欢迎消息
onMounted(() => {
  messages.value.push({
    role: "assistant",
    content:
      "您好！我是AI助手，请问有什么可以帮您？\n\n**支持Markdown格式**：\n- 代码块\n- 列表\n- 表格等",
    timestamp: new Date(),
  });
});
</script>

<style scoped>
/* 基础样式 */
:root {
  --primary-color: #1e88e5;
  --primary-light: #64b5f6;
  --primary-dark: #0d47a1;
  --bg-color: #f5f9ff;
  --text-color: #333;
  --card-bg: #ffffff;
  --tech-line: rgba(30, 136, 229, 0.2);
}

* {
  margin: 0;
  padding: 0;
  box-sizing: border-box;
  font-family: "Segoe UI", "PingFang SC", "Microsoft YaHei", sans-serif;
}

.app-container {
  min-height: 100vh;
  background-color: var(--bg-color);
  position: relative;
  overflow: hidden;
}

@keyframes pulse {
  0%,
  100% {
    opacity: 0.3;
  }
  50% {
    opacity: 1;
  }
}

/* 聊天容器 */
.chat-container {
  max-width: 900px;
  margin: 2rem auto;
  padding: 0 1rem;
  position: relative;
  z-index: 5;
}

.chat-history {
  height: 65vh;
  overflow-y: auto;
  padding: 1rem;
  background-color: var(--card-bg);
  border-radius: 12px;
  box-shadow: 0 8px 32px rgba(30, 136, 229, 0.1);
  margin-bottom: 1.5rem;
  scroll-behavior: smooth;
}

/* 消息样式 */
.message {
  display: flex;
  margin-bottom: 1.5rem;
  animation: fadeIn 0.3s ease-out;
}

@keyframes fadeIn {
  from {
    opacity: 0;
    transform: translateY(10px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

.message-avatar {
  width: 40px;
  height: 40px;
  border-radius: 50%;
  overflow: hidden;
  margin-right: 1rem;
  flex-shrink: 0;
  background-color: #e3f2fd;
  display: flex;
  align-items: center;
  justify-content: center;
}

.message-avatar img {
  width: 24px;
  height: 24px;
}

.message-content {
  flex: 1;
  max-width: calc(100% - 60px);
}

.user {
  flex-direction: row-reverse;
}

.user .message-avatar {
  margin-right: 0;
  margin-left: 1rem;
  background-color: #bbdefb;
}

.user .message-content {
  text-align: right;
}

.markdown-content {
  padding: 0.8rem 1rem;
  border-radius: 12px;
  background-color: #e3f2fd;
  display: inline-block;
  max-width: 100%;
  text-align: left;
}

.user .markdown-content {
  background-color: var(--primary-light);
  color: white;
}

.message-time {
  font-size: 0.75rem;
  color: #757575;
  margin-top: 0.3rem;
}

.user .message-time {
  text-align: right;
}

/* 加载指示器 */
.loading-indicator {
  display: flex;
  justify-content: center;
  padding: 1rem;
}

.loading-dots {
  display: flex;
  gap: 6px;
}

.loading-dots div {
  width: 10px;
  height: 10px;
  border-radius: 50%;
  background-color: var(--primary-light);
  animation: bounce 1.4s infinite ease-in-out;
}

.loading-dots div:nth-child(2) {
  animation-delay: 0.2s;
}

.loading-dots div:nth-child(3) {
  animation-delay: 0.4s;
}

@keyframes bounce {
  0%,
  80%,
  100% {
    transform: scale(0);
  }
  40% {
    transform: scale(1);
  }
}

/* 输入区域 */
.input-area {
  display: flex;
  gap: 1rem;
  background-color: var(--card-bg);
  border-radius: 12px;
  padding: 1rem;
  box-shadow: 0 8px 32px rgba(30, 136, 229, 0.1);
}

.input-area textarea {
  flex: 1;
  border: none;
  border-radius: 8px;
  padding: 0.8rem 1rem;
  font-size: 1rem;
  resize: none;
  background-color: #f5f9ff;
  border: 1px solid #bbdefb;
  transition: all 0.3s;
  outline: none;
}

.input-area textarea:focus {
  border-color: var(--primary-color);
  box-shadow: 0 0 0 2px rgba(30, 136, 229, 0.2);
}

.input-area button {
  width: 48px;
  height: 48px;
  border: none;
  border-radius: 50%;
  background: linear-gradient(
    135deg,
    var(--primary-color),
    var(--primary-dark)
  );
  color: white;
  cursor: pointer;
  transition: all 0.3s;
  display: flex;
  align-items: center;
  justify-content: center;
}

.input-area button:hover {
  transform: translateY(-2px);
  box-shadow: 0 4px 12px rgba(30, 136, 229, 0.3);
}

.input-area button:disabled {
  background: #b0bec5;
  cursor: not-allowed;
  transform: none;
  box-shadow: none;
}

.input-area button svg {
  width: 24px;
  height: 24px;
  fill: white;
}

/* 科技感装饰元素 */
.tech-circle {
  position: absolute;
  border-radius: 50%;
  background: radial-gradient(
    circle,
    rgba(30, 136, 229, 0.1) 0%,
    rgba(30, 136, 229, 0) 70%
  );
  z-index: 1;
}

.circle-1 {
  width: 300px;
  height: 300px;
  top: -100px;
  left: -100px;
}

.circle-2 {
  width: 500px;
  height: 500px;
  bottom: -200px;
  right: -200px;
}

.circle-3 {
  width: 200px;
  height: 200px;
  top: 50%;
  right: 10%;
}

/* 响应式设计 */
@media (max-width: 768px) {
  .chat-history {
    height: 60vh;
  }

  .input-area {
    flex-direction: column;
  }

  .input-area button {
    width: 100%;
    border-radius: 8px;
    height: 42px;
  }
}

/* Markdown内容样式增强 */
:deep(.markdown-content) {
  line-height: 1.6;
}

:deep(.markdown-content h1),
:deep(.markdown-content h2),
:deep(.markdown-content h3) {
  margin: 0.5em 0;
  color: inherit;
}

:deep(.markdown-content p) {
  margin: 0.5em 0;
}

:deep(.markdown-content ul),
:deep(.markdown-content ol) {
  padding-left: 1.5em;
  margin: 0.5em 0;
}

:deep(.markdown-content code) {
  background-color: rgba(0, 0, 0, 0.05);
  padding: 0.2em 0.4em;
  border-radius: 3px;
  font-family: "Courier New", monospace;
}

:deep(.markdown-content pre) {
  background-color: rgba(0, 0, 0, 0.05);
  padding: 1em;
  border-radius: 6px;
  overflow-x: auto;
}

:deep(.markdown-content pre code) {
  background-color: transparent;
  padding: 0;
}

:deep(.markdown-content blockquote) {
  border-left: 3px solid var(--primary-light);
  padding-left: 1em;
  margin: 1em 0;
  color: #666;
}

.user :deep(.markdown-content code) {
  background-color: rgba(255, 255, 255, 0.2);
}

.user :deep(.markdown-content pre) {
  background-color: rgba(255, 255, 255, 0.15);
}
</style>
