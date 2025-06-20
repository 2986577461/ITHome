<template>
  <div class="app-container" ref="appContainer">
    <Header></Header>
    <!-- 粒子背景 -->
    <canvas ref="particleCanvas" class="particles"></canvas>

    <!-- 玻璃态卡片容器 -->
    <div class="glass-card">
      <!-- 头部 -->
      <header class="app-header">
        <h1 class="logo">
          <span class="logo-text">AI</span>
          <span class="logo-pulse"></span>
          <span class="logo-text">Assistant</span>
        </h1>
        <div class="tech-dots">
          <div v-for="i in 5" :key="i" :style="`--delay: ${i * 0.1}s`"></div>
        </div>
      </header>

      <!-- 对话区域 -->
      <div class="chat-wrapper">
        <div class="chat-history" ref="chatHistory">
          <transition-group name="message-fade">
            <div
              v-for="(message, index) in messages"
              :key="index"
              :class="['message', message.role]"
            >
              <div class="message-avatar">
                <div class="avatar-glow"></div>
                <img
                  :src="
                    message.role === 'user'
                      ? generateAvatar('user')
                      : generateAvatar('bot')
                  "
                />
              </div>
              <div class="message-content">
                <vue-markdown
                  :source="message.content"
                  class="markdown-content"
                ></vue-markdown>
                <div class="message-time">
                  {{ formatTime(message.timestamp) }}
                </div>
              </div>
            </div>
          </transition-group>

          <div v-if="loading" class="loading-bars">
            <div v-for="i in 3" :key="i" :style="`--index: ${i}`"></div>
          </div>
        </div>

        <!-- 输入区域 -->
        <div class="input-area">
          <div class="input-border"></div>
          <textarea
            v-model="userInput"
            @keydown.enter.except.prevent="sendMessage"
            placeholder="输入问题..."
            rows="2"
          ></textarea>
          <button @click="sendMessage" :disabled="loading">
            <svg class="send-icon" viewBox="0 0 24 24">
              <path d="M2.01 21L23 12 2.01 3 2 10l15 2-15 2z" />
              <path
                class="send-icon-pulse"
                d="M2.01 21L23 12 2.01 3 2 10l15 2-15 2z"
              />
            </svg>
            <span class="send-wave"></span>
          </button>
        </div>
      </div>
    </div>

    <!-- 悬浮科技元素 -->
    <div class="tech-orb orb-1"></div>
    <div class="tech-orb orb-2"></div>
    <div class="tech-line line-1"></div>
    <div class="tech-line line-2"></div>
  </div>
</template>

<script setup>
import VueMarkdown from "vue-markdown-render";
import { ref, onMounted, nextTick } from "vue";
import Header from "@/components/Header.vue";
import { aiDialog } from "@/axios/axios";

const userInput = ref("");
const messages = ref([]);
const loading = ref(false);
const chatHistory = ref(null);
const particleCanvas = ref(null);
const appContainer = ref(null);

// 粒子系统
const initParticles = () => {
  const canvas = particleCanvas.value;
  const ctx = canvas.getContext("2d");
  const container = appContainer.value;

  canvas.width = container.offsetWidth;
  canvas.height = container.offsetHeight;

  const particles = [];
  const particleCount = Math.floor((canvas.width * canvas.height) / 10000);

  // 创建粒子
  for (let i = 0; i < particleCount; i++) {
    particles.push({
      x: Math.random() * canvas.width,
      y: Math.random() * canvas.height,
      size: Math.random() * 2 + 0.5,
      speed: Math.random() * 0.5 + 0.1,
      color: `rgba(100, 180, 255, ${Math.random() * 0.3 + 0.1})`,
      direction: Math.random() * Math.PI * 2,
    });
  }

  // 动画循环
  const animate = () => {
    ctx.clearRect(0, 0, canvas.width, canvas.height);

    particles.forEach((p) => {
      p.x += Math.cos(p.direction) * p.speed;
      p.y += Math.sin(p.direction) * p.speed;

      // 边界检查
      if (p.x < 0 || p.x > canvas.width) p.direction = Math.PI - p.direction;
      if (p.y < 0 || p.y > canvas.height) p.direction = -p.direction;

      // 绘制粒子
      ctx.beginPath();
      ctx.arc(p.x, p.y, p.size, 0, Math.PI * 2);
      ctx.fillStyle = p.color;
      ctx.fill();
    });

    requestAnimationFrame(animate);
  };

  animate();

  // 窗口大小调整
  window.addEventListener("resize", () => {
    canvas.width = container.offsetWidth;
    canvas.height = container.offsetHeight;
  });
};

// 发送消息
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
    const response = await aiDialog(userMessage.content);
    messages.value.push({
      role: "assistant",
      content: response,
      timestamp: new Date(),
    });
  } catch (error) {
    console.error("API Error:", error);
    messages.value.push({
      role: "assistant",
      content: "⚠️ 请求失败，请稍后重试\n```\n" + error.message + "\n```",
      timestamp: new Date(),
    });
  } finally {
    loading.value = false;
    scrollToBottom();
  }
};

// 辅助函数
const formatTime = (date) => {
  return new Date(date).toLocaleTimeString([], {
    hour: "2-digit",
    minute: "2-digit",
  });
};

const scrollToBottom = () => {
  nextTick(() => {
    chatHistory.value.scrollTo({
      top: chatHistory.value.scrollHeight,
      behavior: "smooth",
    });
  });
};

// 初始化
onMounted(() => {
  initParticles();
  messages.value.push({
    role: "assistant",
    content:
      "**欢迎使用AI助手**\n\n我是您的智能助理，可以：\n- 解答技术问题\n- 生成创意内容\n- 分析数据\n\n试试问我点什么吧！",
    timestamp: new Date(),
  });
});

// 生成简约头像
function generateAvatar(type) {
  const canvas = document.createElement("canvas");
  canvas.width = 64;
  canvas.height = 64;
  const ctx = canvas.getContext("2d");

  // 渐变背景
  const gradient = ctx.createRadialGradient(32, 32, 0, 32, 32, 32);
  gradient.addColorStop(0, type === "user" ? "#4facfe" : "#00f2fe");
  gradient.addColorStop(1, type === "user" ? "#00f2fe" : "#4facfe");

  ctx.fillStyle = gradient;
  ctx.fillRect(0, 0, 64, 64);

  // 添加字母
  ctx.fillStyle = "rgba(255,255,255,0.9)";
  ctx.font = "bold 30px Arial";
  ctx.textAlign = "center";
  ctx.textBaseline = "middle";
  ctx.fillText(type === "user" ? "U" : "AI", 32, 32);

  return canvas.toDataURL();
}
</script>

<style scoped>
/* 颜色变量 */
:root {
  --primary: #1e88e5;
  --primary-light: #64b5f6;
  --primary-dark: #0d47a1;
  --accent: #00e5ff;
  --bg: #0a192f;
  --glass: rgba(16, 42, 87, 0.6);
  --text: #e6f1ff;
}

/* 基础样式 */
* {
  margin: 0;
  padding: 0;
  box-sizing: border-box;
  font-family: "Segoe UI", "PingFang SC", -apple-system, sans-serif;
}

.app-container {
  min-height: 100vh;
  background: linear-gradient(135deg, var(--bg), #0a0f2b);
  position: relative;
  overflow: hidden;
}

/* 粒子背景 */
.particles {
  position: fixed;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  z-index: 1;
  pointer-events: none;
}

/* 玻璃态卡片 */
.glass-card {
  position: relative;
  max-width: 900px;
  margin: 3% auto;
  background: var(--glass);
  backdrop-filter: blur(12px);
  -webkit-backdrop-filter: blur(12px);
  border-radius: 20px;
  border: 1px solid rgba(100, 200, 255, 0.1);
  box-shadow: 0 8px 32px rgba(0, 0, 0, 0.3),
    inset 0 0 0 1px rgba(255, 255, 255, 0.05);
  overflow: hidden;
  z-index: 2;
}

/* 头部样式 */
.app-header {
  padding: 1.5rem 2rem;
  border-bottom: 1px solid rgba(255, 255, 255, 0.1);
  position: relative;
}

.logo {
  display: flex;
  align-items: center;
  gap: 12px;
  font-size: 1.8rem;
  font-weight: 600;
  background: linear-gradient(90deg, var(--primary-light), var(--accent));
  -webkit-background-clip: text;
  background-clip: text;
  color: transparent;
  position: relative;
}

.logo-pulse {
  width: 12px;
  height: 12px;
  background: var(--accent);
  border-radius: 50%;
  box-shadow: 0 0 0 0 rgba(0, 229, 255, 0.7);
  animation: pulse 1.5s infinite;
}

@keyframes pulse {
  0% {
    box-shadow: 0 0 0 0 rgba(0, 229, 255, 0.7);
  }
  70% {
    box-shadow: 0 0 0 12px rgba(0, 229, 255, 0);
  }
  100% {
    box-shadow: 0 0 0 0 rgba(0, 229, 255, 0);
  }
}

.tech-dots {
  display: flex;
  gap: 8px;
  position: absolute;
  right: 2rem;
  top: 50%;
  transform: translateY(-50%);
}

.tech-dots div {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  background: var(--primary-light);
  opacity: 0.6;
  animation: blink 2s infinite;
}

.tech-dots div:nth-child(1) {
  animation-delay: 0.1s;
}
.tech-dots div:nth-child(2) {
  animation-delay: 0.3s;
}
.tech-dots div:nth-child(3) {
  animation-delay: 0.5s;
}
.tech-dots div:nth-child(4) {
  animation-delay: 0.7s;
}
.tech-dots div:nth-child(5) {
  animation-delay: 0.9s;
}

@keyframes blink {
  0%,
  100% {
    opacity: 0.2;
    transform: scale(0.8);
  }
  50% {
    opacity: 1;
    transform: scale(1.1);
  }
}

/* 对话区域 */
.chat-wrapper {
  display: flex;
  flex-direction: column;
  height: 70vh;
}

.chat-history {
  flex: 1;
  padding: 1.5rem;
  overflow-y: auto;
  scroll-behavior: smooth;
}

/* 消息样式 */
.message {
  display: flex;
  margin-bottom: 1.5rem;
  position: relative;
}

.message-avatar {
  width: 40px;
  height: 40px;
  border-radius: 50%;
  margin-right: 1rem;
  flex-shrink: 0;
  position: relative;
}

.avatar-glow {
  position: absolute;
  width: 100%;
  height: 100%;
  border-radius: 50%;
  background: radial-gradient(circle, var(--primary-light) 0%, transparent 70%);
  opacity: 0.4;
  animation: glow-pulse 3s infinite;
}

@keyframes glow-pulse {
  0%,
  100% {
    transform: scale(1);
    opacity: 0.3;
  }
  50% {
    transform: scale(1.1);
    opacity: 0.6;
  }
}

.message-avatar img {
  width: 100%;
  height: 100%;
  border-radius: 50%;
  object-fit: cover;
  position: relative;
  z-index: 1;
}

.message-content {
  max-width: calc(100% - 60px);
}

.user {
  flex-direction: row-reverse;
}

.user .message-avatar {
  margin-right: 0;
  margin-left: 1rem;
}

.user .message-content {
  align-items: flex-end;
}

.markdown-content {
  padding: 12px 16px;
  border-radius: 12px;
  background: rgba(30, 136, 229, 0.15);
  border: 1px solid rgba(100, 200, 255, 0.1);
  display: inline-block;
  max-width: 100%;
  backdrop-filter: blur(5px);
  animation: message-appear 0.3s ease-out;
}

.user .markdown-content {
  background: rgba(0, 229, 255, 0.15);
  border: 1px solid rgba(0, 229, 255, 0.2);
}

@keyframes message-appear {
  from {
    opacity: 0;
    transform: translateY(10px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

.message-time {
  font-size: 0.75rem;
  color: rgba(255, 255, 255, 0.5);
  margin-top: 4px;
}

.user .message-time {
  text-align: right;
}

/* 加载动画 */
.loading-bars {
  display: flex;
  gap: 4px;
  height: 20px;
  align-items: center;
  padding: 0 60px;
}

.loading-bars div {
  height: 100%;
  width: 6px;
  background: var(--primary-light);
  animation: loading-bar 1.2s infinite ease-in-out;
  border-radius: 2px;
}

.loading-bars div:nth-child(1) {
  animation-delay: 0s;
  height: 30%;
}
.loading-bars div:nth-child(2) {
  animation-delay: 0.2s;
  height: 60%;
}
.loading-bars div:nth-child(3) {
  animation-delay: 0.4s;
  height: 100%;
}

@keyframes loading-bar {
  0%,
  100% {
    transform: scaleY(0.3);
    opacity: 0.6;
  }
  50% {
    transform: scaleY(1);
    opacity: 1;
  }
}

/* 输入区域 */
.input-area {
  position: relative;
  padding: 1.5rem;
  border-top: 1px solid rgba(255, 255, 255, 0.1);
}

.input-border {
  position: absolute;
  top: 0;
  left: 0;
  width: 100%;
  height: 2px;
  background: linear-gradient(90deg, var(--primary), var(--accent));
  transform: scaleX(0);
  transform-origin: left;
  transition: transform 0.3s;
}

.input-area:focus-within .input-border {
  transform: scaleX(1);
}

textarea {
  width: 100%;
  background: rgba(10, 25, 47, 0.5);
  border: 1px solid rgba(100, 200, 255, 0.2);
  border-radius: 12px;
  padding: 12px 16px;
  color: var(--text);
  font-size: 1rem;
  resize: none;
  outline: none;
  transition: all 0.3s;
  backdrop-filter: blur(5px);
}

textarea:focus {
  border-color: var(--accent);
  box-shadow: 0 0 0 2px rgba(0, 229, 255, 0.2);
}

button {
  position: absolute;
  right: 2rem;
  bottom: 2rem;
  width: 48px;
  height: 48px;
  border: none;
  border-radius: 50%;
  background: linear-gradient(135deg, var(--primary), var(--primary-dark));
  color: white;
  cursor: pointer;
  transition: all 0.3s;
  overflow: hidden;
}

button:hover {
  transform: translateY(-2px);
  box-shadow: 0 4px 20px rgba(0, 229, 255, 0.3);
}

button:disabled {
  opacity: 0.5;
  transform: none !important;
  box-shadow: none !important;
}

.send-icon {
  width: 24px;
  height: 24px;
  position: relative;
  z-index: 1;
}

.send-icon path {
  fill: white;
}

.send-icon-pulse {
  opacity: 0;
  animation: send-pulse 1.5s infinite;
}

@keyframes send-pulse {
  0% {
    opacity: 0;
    transform: scale(1);
  }
  50% {
    opacity: 1;
    transform: scale(1.2);
  }
  100% {
    opacity: 0;
    transform: scale(1.4);
  }
}

.send-wave {
  position: absolute;
  width: 100%;
  height: 100%;
  top: 0;
  left: 0;
  background: radial-gradient(
    circle,
    rgba(0, 229, 255, 0.4) 0%,
    transparent 70%
  );
  transform: scale(0);
  opacity: 0;
  border-radius: 50%;
}

button:not(:disabled):hover .send-wave {
  animation: wave 0.6s ease-out;
}

@keyframes wave {
  0% {
    transform: scale(0);
    opacity: 1;
  }
  100% {
    transform: scale(2);
    opacity: 0;
  }
}

/* 悬浮科技元素 */
.tech-orb {
  position: fixed;
  border-radius: 50%;
  filter: blur(40px);
  opacity: 0.15;
  z-index: 0;
}

.orb-1 {
  width: 300px;
  height: 300px;
  background: var(--primary-light);
  top: -100px;
  left: -100px;
  animation: float 12s infinite ease-in-out;
}

.orb-2 {
  width: 400px;
  height: 400px;
  background: var(--accent);
  bottom: -150px;
  right: -100px;
  animation: float 15s infinite ease-in-out reverse;
}

.tech-line {
  position: fixed;
  background: linear-gradient(
    90deg,
    transparent,
    var(--primary-light),
    transparent
  );
  height: 1px;
  transform-origin: left;
  z-index: 0;
}

.line-1 {
  width: 100%;
  top: 30%;
  transform: rotate(-5deg);
  opacity: 0.1;
  animation: line-move 20s linear infinite;
}

.line-2 {
  width: 80%;
  bottom: 20%;
  left: 10%;
  transform: rotate(2deg);
  opacity: 0.05;
  animation: line-move 25s linear infinite reverse;
}

@keyframes float {
  0%,
  100% {
    transform: translate(0, 0);
  }
  50% {
    transform: translate(20px, 20px);
  }
}

@keyframes line-move {
  0% {
    transform: translateX(-100%) rotate(-5deg);
  }
  100% {
    transform: translateX(100%) rotate(-5deg);
  }
}

/* 过渡动画 */
.message-fade-enter-active,
.message-fade-leave-active {
  transition: all 0.3s ease;
}

.message-fade-enter-from,
.message-fade-leave-to {
  opacity: 0;
  transform: translateY(10px);
}

/* 响应式设计 */
@media (max-width: 768px) {
  .glass-card {
    border-radius: 0;
    height: 100vh;
  }

  .chat-wrapper {
    height: calc(100vh - 120px);
  }

  .tech-orb {
    display: none;
  }
}

/* Markdown增强样式 */
:deep(.markdown-content) {
  line-height: 1.7;
  color: var(--text);
}

:deep(.markdown-content h1),
:deep(.markdown-content h2) {
  margin: 1em 0 0.5em;
  color: var(--primary-light);
}

:deep(.markdown-content h3) {
  margin: 0.8em 0 0.4em;
  color: var(--accent);
}

:deep(.markdown-content p) {
  margin: 0.5em 0;
}

:deep(.markdown-content ul),
:deep(.markdown-content ol) {
  padding-left: 1.5em;
  margin: 0.5em 0;
}

:deep(.markdown-content li) {
  margin: 0.3em 0;
}

:deep(.markdown-content code) {
  background: rgba(0, 0, 0, 0.3);
  padding: 0.2em 0.4em;
  border-radius: 4px;
  font-family: "Fira Code", monospace;
  font-size: 0.9em;
}

:deep(.markdown-content pre) {
  background: rgba(10, 25, 47, 0.8) !important;
  padding: 1em;
  border-radius: 8px;
  overflow-x: auto;
  margin: 0.8em 0;
  border: 1px solid rgba(100, 200, 255, 0.1);
}

:deep(.markdown-content pre code) {
  background: transparent;
  padding: 0;
}

:deep(.markdown-content blockquote) {
  border-left: 3px solid var(--primary);
  padding-left: 1em;
  margin: 1em 0;
  color: rgba(255, 255, 255, 0.7);
  font-style: italic;
}

.user :deep(.markdown-content) {
  color: rgb(0, 0, 0);
}

.user :deep(.markdown-content code) {
  background: rgba(255, 255, 255, 0.15);
}

.user :deep(.markdown-content pre) {
  background: rgba(0, 0, 0, 0.4) !important;
  border-color: rgba(255, 255, 255, 0.1);
}
</style>
