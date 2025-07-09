<template>
  <Header></Header>
  <div class="app-container" ref="appContainer">
    <canvas ref="particleCanvas" class="particles"></canvas>

    <div class="border-glow-top"></div>
    <div class="border-glow-right"></div>
    <div class="border-glow-bottom"></div>
    <div class="border-glow-left"></div>

    <div class="glass-card">
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
                    alt="Avatar"
                />
              </div>
              <div class="message-content">
                <template v-if="message.content">
                  <vue-markdown
                      :key="message.content"
                      :source="message.content"
                      class="markdown-content"
                  ></vue-markdown>
                  <div class="message-time">
                    {{ formatTime(message.timestamp) }}
                  </div>
                </template>
              </div>
            </div>
          </transition-group>
        </div>

        <div class="input-area">
          <textarea
              v-model="userInput"
              @keydown.enter.prevent="sendMessage"
              placeholder="输入你的问题..."
              rows="1"
              :disabled="loading"
          ></textarea>
          <button @click="sendMessage" :disabled="loading">
            <svg
                width="24"
                height="24"
                viewBox="0 0 24 24"
                fill="none"
                xmlns="http://www.w3.org/2000/svg"
            >
              <path
                  d="M21.3328 10.8711C21.7371 11.0506 21.9056 11.5303 21.7261 11.9347L17.7261 20.9347C17.5466 21.3391 17.0669 21.5076 16.6626 21.3281C16.2582 21.1486 16.0897 20.6689 16.2692 20.2645L20.2692 11.2645C20.4487 10.8601 20.9284 10.6916 21.3328 10.8711Z"
                  fill="currentColor"
              />
              <path
                  d="M17.0759 2.11584C17.3917 1.84917 17.8687 1.90696 18.1354 2.22271L21.8646 6.51866C22.1313 6.83441 22.0735 7.31144 21.7578 7.57811C21.442 7.84477 20.965 7.78698 20.6983 7.47124L16.9692 3.17529C16.7025 2.85954 16.7599 2.38259 17.0759 2.11584Z"
                  fill="currentColor"
              />
              <path
                  d="M1.33989 12.0621C0.935541 12.2416 0.767041 12.7213 0.946566 13.1257L4.94657 22.1257C5.12609 22.5301 5.60579 22.6986 6.01014 22.5191C6.41449 22.3396 6.58299 21.8599 6.40346 21.4555L2.40346 12.4555C2.22394 12.0512 1.74424 11.8827 1.33989 12.0621Z"
                  fill="currentColor"
              />
              <path
                  d="M17.0759 2.11584C17.3917 1.84917 17.8687 1.90696 18.1354 2.22271L21.8646 6.51866C22.1313 6.83441 22.0735 7.31144 21.7578 7.57811C21.442 7.84477 20.965 7.78698 20.6983 7.47124L16.9692 3.17529C16.7025 2.85954 16.7599 2.38259 17.0759 2.11584Z"
                  fill="currentColor"
              />
              <path
                  d="M6.92408 2.11584C6.60833 1.84917 6.13138 1.90696 5.86472 2.22271L2.13547 6.51866C1.86881 6.83441 1.9262 7.31144 2.24195 7.57811C2.5577 7.84477 3.03465 7.78698 3.30131 7.47124L7.03046 3.17529C7.29712 2.85954 7.23973 2.38259 6.92408 2.11584Z"
                  fill="currentColor"
              />
              <path
                  fill-rule="evenodd"
                  clip-rule="evenodd"
                  d="M2.99965 11.9999L20.9996 2.9999L11.9996 20.9999L9.99965 14.9999L2.99965 11.9999ZM11.0001 13.2504L13.7501 10.5004L11.0001 7.75043L8.25015 10.5004L11.0001 13.2504Z"
                  fill="currentColor"
              />
            </svg>
          </button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted, nextTick } from 'vue';
import axios from 'axios';
import VueMarkdown from 'vue-markdown-render';
import Header from "@/components/Header.vue"; // 确保此路径正确
import axiosInstance from "@/request/axiosInit.js"; // 确保此路径正确

const userInput = ref('');
const messages = ref([]);
const loading = ref(false);
const chatHistory = ref(null);
const currentAIMessageIndex = ref(-1);
const particleCanvas = ref(null);
const appContainer = ref(null);

let particlesArray = [];
let ctx;
let mouse = {
  x: null,
  y: null,
  radius: 100,
};

const colors = {
  '--bg-dark': '#0A1325',
  '--bg-medium': '#141D32',
  '--bg-light': '#1E2741',
  '--primary-color': '#00BFFF',
  '--primary-light': '#87CEFA',
  '--primary-rgb': '0, 191, 255',
  '--accent-color': '#FF00FF',
  '--accent-light': '#FF99FF',
  '--accent-rgb': '255, 0, 255',
  '--text-light': '#E0E6EF',
  '--text-dark': '#A0AABB',
  '--glass-bg': 'rgba(10, 20, 40, 0.6)',
  '--glass-border': 'rgba(100, 180, 255, 0.3)',
  '--glass-shadow': 'rgba(0, 0, 0, 0.4)',
  '--user-bubble-bg': 'linear-gradient(135deg, #00BFFF, #007ACC)',
  '--bot-bubble-bg': 'linear-gradient(135deg, #1E2741, #2A3650)',
};

onMounted(() => {
  for (const key in colors) {
    document.documentElement.style.setProperty(key, colors[key]);
  }
});

const animateParticles = () => {
  requestAnimationFrame(animateParticles);
  if (ctx && particleCanvas.value) { // 检查 ctx 和 canvas 元素是否存在
    ctx.clearRect(0, 0, particleCanvas.value.width, particleCanvas.value.height);
  }

  for (let i = 0; i < particlesArray.length; i++) {
    particlesArray[i].update();
  }
};

const formatTime = (date) => {
  if (!date) return '';
  const d = new Date(date);
  const hours = d.getHours().toString().padStart(2, '0');
  const minutes = d.getMinutes().toString().padStart(2, '0');
  return `${hours}:${minutes}`;
};

const scrollToBottom = async () => {
  await nextTick();
  if (chatHistory.value) {
    chatHistory.value.scrollTop = chatHistory.value.scrollHeight;
  }
};

const generateAvatar = (role) => {
  // 确保这里的 URL 稳定且有效
  if (role === 'user') {
    return `https://pic.rmb.bdstatic.com/bjh/bc11504608c0/250317/d63cda97f56b0a6ca4ae8d02dfc25e4f.png`;
  } else {
    return `https://th.bing.com/th/id/OIP.iTH2miJeDuDveYxznohe8AHaHa?r=0&rs=1&pid=ImgDetMain`;
  }
};

// 粒子类 (保持不变)
class Particle {
  constructor(x, y, directionX, directionY, size, color) {
    this.x = x;
    this.y = y;
    this.directionX = directionX;
    this.directionY = directionY;
    this.size = size;
    this.color = color;
  }

  draw() {
    if (!ctx) return; // 确保 ctx 存在
    ctx.beginPath();
    ctx.arc(this.x, this.y, this.size, 0, Math.PI * 2, false);
    ctx.fillStyle = this.color;
    ctx.shadowBlur = this.size * 0.5;
    ctx.shadowColor = this.color;
    ctx.fill();
  }

  update() {
    if (!particleCanvas.value) return; // 确保 canvas 元素存在

    if (this.x + this.size > particleCanvas.value.width || this.x - this.size < 0) {
      this.directionX = -this.directionX;
    }
    if (this.y + this.size > particleCanvas.value.height || this.y - this.size < 0) {
      this.directionY = -this.directionY;
    }
    this.x += this.directionX;
    this.y += this.directionY;

    let dx = mouse.x - this.x;
    let dy = mouse.y - this.y;
    let distance = Math.sqrt(dx * dx + dy * dy);
    if (distance < mouse.radius + this.size) {
      if (mouse.x !== null && this.x < particleCanvas.value.width - this.size * 10) {
        this.x += 10;
      }
      if (mouse.x !== null && this.x > this.size * 10) {
        this.x -= 10;
      }
      if (mouse.y !== null && this.y < particleCanvas.value.height - this.size * 10) {
        this.y += 10;
      }
      if (mouse.y !== null && this.y > this.size * 10) {
        this.y -= 10;
      }
    }
    this.draw();
  }
}

// 初始化粒子 (保持不变)
const initParticles = () => {
  if (!particleCanvas.value) return;

  ctx = particleCanvas.value.getContext('2d');
  particleCanvas.value.width = window.innerWidth;
  particleCanvas.value.height = window.innerHeight;
  particlesArray = [];
  let numberOfParticles = (particleCanvas.value.height * particleCanvas.value.width) / 9000;

  for (let i = 0; i < numberOfParticles; i++) {
    let size = (Math.random() * 5) + 1;
    let x = (Math.random() * ((particleCanvas.value.width - size * 2) - (size * 2)) + size * 2);
    let y = (Math.random() * ((particleCanvas.value.height - size * 2) - (size * 2)) + size * 2);
    let directionX = (Math.random() * 0.5) - 0.25;
    let directionY = (Math.random() * 0.5) - 0.25;
    let color = `rgba(${Math.floor(Math.random() * 50) + 180}, ${Math.floor(Math.random() * 50) + 200}, ${Math.floor(Math.random() * 50) + 250}, ${Math.random() * 0.5 + 0.1})`;
    particlesArray.push(new Particle(x, y, directionX, directionY, size, color));
  }
};


const sendMessage = async () => {
  if (!userInput.value.trim() || loading.value) return;

  const userMessage = {
    role: "user",
    content: userInput.value,
    timestamp: new Date(),
  };
  messages.value.push(userMessage);
  userInput.value = "";
  loading.value = true;
  await scrollToBottom();

  const aiMessagePlaceholder = {
    role: "assistant",
    content: "",
    timestamp: new Date(),
  };
  messages.value.push(aiMessagePlaceholder);
  currentAIMessageIndex.value = messages.value.length - 1;
  await scrollToBottom();

  try {
    const eventSource = new EventSource(
        `http://localhost:8080/user/ai-dialog?message=${encodeURIComponent(userMessage.content)}&token=${localStorage.getItem("token")}`
    );

    eventSource.onmessage = (event) => {
      let rawData = event.data;

      // 修复：处理可能的双重 data: 前缀
      if (rawData.startsWith('data:')) {
        rawData = rawData.substring(5).trim();
      }

      // **宝宝，看这里，我帮你加了对 '[DONE]' 的判断！**
      if (rawData === '[DONE]') {
        eventSource.close(); // 关闭 EventSource
        loading.value = false; // 停止加载状态
        return; // 不再尝试解析 JSON
      }

      try {
        const eventData = JSON.parse(rawData);
        const contentChunk = eventData.choices?.[0]?.delta?.content || '';
        if (contentChunk) {
          messages.value[currentAIMessageIndex.value].content += contentChunk;
          scrollToBottom();
        }
      } catch (e) {
        console.error('解析 JSON 失败:', e, event.data);
      }
    };

    eventSource.onerror = () => {
      eventSource.close();
      loading.value = false;
    };
  } catch (error) {
    console.error("Error:", error);
    loading.value = false;
  }
};

onMounted(() => {
  initParticles();
  animateParticles();
  window.addEventListener('resize', initParticles);

  messages.value.push({
    role: "assistant",
    content: "哈喽！我是你的专属AI助手，有什么问题尽管问我吧！",
    timestamp: new Date(),
  });

  window.addEventListener('mousemove', (event) => {
    mouse.x = event.x;
    mouse.y = event.y;
  });
  window.addEventListener('mouseout', () => {
    mouse.x = undefined;
    mouse.y = undefined;
  });

  scrollToBottom();
});
</script>

<style scoped>
/* 根变量定义，方便统一管理颜色和效果 */
:root {
  --bg-dark: #0A1325;
  --bg-medium: #141D32;
  --bg-light: #1E2741;

  --primary-color: #00BFFF; /* 深天蓝色 */
  --primary-light: #87CEFA; /* 浅天蓝色 */
  --primary-rgb: 0, 191, 255; /* primary-color 的 RGB 值 */

  --accent-color: #FF00FF; /* 紫红色 */
  --accent-light: #FF99FF; /* 浅紫红色 */
  --accent-rgb: 255, 0, 255; /* accent-color 的 RGB 值 */

  --text-light: #E0E6EF;
  --text-dark: #A0AABB;

  --glass-bg: rgba(10, 20, 40, 0.6); /* 玻璃背景色 */
  --glass-border: rgba(100, 180, 255, 0.3); /* 玻璃边框色 */
  --glass-shadow: rgba(0, 0, 0, 0.4); /* 玻璃阴影色 */

  --user-bubble-bg: linear-gradient(135deg, var(--primary-color), #007ACC); /* 用户消息背景渐变 */
  --bot-bubble-bg: linear-gradient(135deg, var(--bg-light), var(--bg-medium)); /* AI 消息背景渐变 */
}

/* 全局容器和背景 */
.app-container {
  display: flex;
  justify-content: center;
  align-items: center;
  min-height: 90vh;
  background-color: var(--bg-dark);
  color: var(--text-light);
  font-family: 'Segoe UI', Tahoma, Geneva, Verdana, sans-serif;
  overflow: hidden; /* 防止粒子溢出 */
  position: relative;
  width: 100%;
  height: 92vh;
}

/* 粒子背景 */
.particles {
  position: absolute;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  z-index: 1;
  pointer-events: none; /* 允许点击穿透到下面的元素 */
}

/* 科技感边框光晕效果 */
.border-glow-top, .border-glow-right, .border-glow-bottom, .border-glow-left {
  position: absolute;
  background: linear-gradient(to right, rgba(var(--primary-rgb), 0) 0%, rgba(var(--primary-rgb), 0.5) 50%, rgba(var(--primary-rgb), 0) 100%);
  filter: blur(10px);
  z-index: 2;
  opacity: 0.5;
  animation: borderScan 4s infinite linear;
}

.border-glow-top {
  top: 0;
  left: 0;
  width: 100%;
  height: 2px;
  transform-origin: left;
}

.border-glow-bottom {
  bottom: 0;
  left: 0;
  width: 100%;
  height: 2px;
  transform-origin: right;
  animation-delay: 2s;
}

.border-glow-right, .border-glow-left {
  background: linear-gradient(to bottom, rgba(var(--primary-rgb), 0) 0%, rgba(var(--primary-rgb), 0.5) 50%, rgba(var(--primary-rgb), 0) 100%);
  width: 2px;
  height: 100%;
  animation: borderScanY 4s infinite linear;
}

.border-glow-right {
  right: 0;
  top: 0;
  transform-origin: top;
}

.border-glow-left {
  left: 0;
  top: 0;
  transform-origin: bottom;
  animation-delay: 2s;
}

@keyframes borderScan {
  0% {
    transform: translateX(-100%);
  }
  100% {
    transform: translateX(100%);
  }
}

@keyframes borderScanY {
  0% {
    transform: translateY(-100%);
  }
  100% {
    transform: translateY(100%);
  }
}


/* 玻璃态卡片 */
.glass-card {
  position: relative;
  z-index: 10; /* 确保在粒子背景之上 */
  width: 90%;
  max-width: 800px;
  height: 90vh;
  background: var(--glass-bg);
  border-radius: 15px;
  border: 1px solid var(--glass-border);
  box-shadow: 0 8px 32px 0 var(--glass-shadow);
  backdrop-filter: blur(10px); /* 玻璃模糊效果 */
  -webkit-backdrop-filter: blur(10px);
  display: flex;
  flex-direction: column;
  overflow: hidden; /* 确保内容在卡片内部 */
  animation: fadeInScale 0.8s ease-out forwards;
}

@keyframes fadeInScale {
  from {
    opacity: 0;
    transform: scale(0.9);
  }
  to {
    opacity: 1;
    transform: scale(1);
  }
}

/* 头部样式 */
.app-header {
  padding: 15px 20px;
  border-bottom: 1px solid var(--glass-border);
  display: flex;
  justify-content: space-between;
  align-items: center;
  background: rgba(var(--bg-dark), 0.7); /* 略深一点的头部背景 */
  position: relative;
  z-index: 1;
}

.logo {
  display: flex;
  align-items: center;
  font-size: 1.8em;
  font-weight: bold;
  color: var(--primary-light);
  text-shadow: 0 0 8px rgba(var(--primary-rgb), 0.7);
  position: relative;
}

.logo-text {
  position: relative;
  z-index: 1;
}

.logo-pulse {
  position: absolute;
  width: 100%;
  height: 100%;
  background: radial-gradient(circle, rgba(var(--accent-rgb), 0.5) 0%, transparent 70%);
  border-radius: 50%;
  filter: blur(10px);
  animation: pulse 1.5s infinite ease-out, glow-subtle 1.5s infinite alternate;
  transform: scale(0.8);
  opacity: 0.7;
}

@keyframes pulse {
  0% {
    transform: scale(0.8);
    opacity: 0.7;
  }
  50% {
    transform: scale(1);
    opacity: 1;
  }
  100% {
    transform: scale(0.8);
    opacity: 0.7;
  }
}

@keyframes glow-subtle {
  0% {
    box-shadow: 0 0 5px var(--accent-color), 0 0 10px var(--accent-color);
  }
  100% {
    box-shadow: 0 0 8px var(--accent-color), 0 0 18px var(--accent-color);
  }
}

.tech-dots {
  display: flex;
  gap: 5px;
}

.tech-dots div {
  width: 8px;
  height: 8px;
  background-color: var(--primary-light);
  border-radius: 50%;
  box-shadow: 0 0 5px var(--primary-color), 0 0 10px var(--primary-color);
  animation: dot-pulse 1.5s infinite alternate var(--delay);
}

@keyframes dot-pulse {
  0% {
    transform: scale(0.8);
    opacity: 0.6;
  }
  50% {
    transform: scale(1.1);
    opacity: 1;
  }
  100% {
    transform: scale(0.8);
    opacity: 0.6;
  }
}

/* 对话区域 */
.chat-wrapper {
  flex-grow: 1;
  display: flex;
  flex-direction: column;
  padding: 15px;
  overflow: hidden;
}

.chat-history {
  flex-grow: 1;
  overflow-y: auto;
  padding-right: 10px; /* 为滚动条留出空间 */
  padding-bottom: 20px; /* 防止最后一条消息被输入框遮挡 */
  -webkit-overflow-scrolling: touch; /* 提升移动端滚动体验 */

  /* 自定义滚动条 */

  &::-webkit-scrollbar {
    width: 8px;
  }

  &::-webkit-scrollbar-track {
    background: rgba(255, 255, 255, 0.05);
    border-radius: 10px;
  }

  &::-webkit-scrollbar-thumb {
    background: rgba(var(--primary-rgb), 0.5);
    border-radius: 10px;
    box-shadow: 0 0 5px rgba(var(--primary-rgb), 0.7);
  }

  &::-webkit-scrollbar-thumb:hover {
    background: var(--primary-color);
  }
}

/* 消息样式 */
.message {
  display: flex;
  align-items: flex-start;
  margin-bottom: 15px;
  animation: message-fade-in 0.3s ease-out;
}

.message-fade-enter-active,
.message-fade-leave-active {
  transition: all 0.5s ease;
}

.message.user {
  justify-content: flex-end;
}

.message.assistant {
  justify-content: flex-start;
}

.message-avatar {
  width: 40px;
  height: 40px;
  border-radius: 50%;
  overflow: hidden;
  position: relative;
  flex-shrink: 0;
  margin: 0 10px;
  border: 2px solid var(--primary-light);
  box-shadow: 0 0 10px rgba(var(--primary-rgb), 0.5);
}

.message.user .message-avatar {
  order: 2;
  margin-left: 10px;
  margin-right: 0;
  border-color: var(--accent-light);
  box-shadow: 0 0 10px rgba(var(--accent-rgb), 0.5);
}

.message-avatar img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.avatar-glow {
  position: absolute;
  top: -5px;
  left: -5px;
  right: -5px;
  bottom: -5px;
  border-radius: 50%;
  background: radial-gradient(circle at center, rgba(var(--primary-rgb), 0.5) 0%, transparent 70%);
  filter: blur(8px);
  animation: avatar-pulse 2s infinite alternate;
  z-index: -1;
}

.message.user .avatar-glow {
  background: radial-gradient(circle at center, rgba(var(--accent-rgb), 0.5) 0%, transparent 70%);
}

@keyframes avatar-pulse {
  0% {
    transform: scale(0.9);
    opacity: 0.7;
  }
  100% {
    transform: scale(1.1);
    opacity: 1;
  }
}

.message-content {
  max-width: 70%;
  padding: 12px 18px;
  border-radius: 18px;
  position: relative;
  font-size: 0.95em;
  line-height: 1.6;
  word-wrap: break-word;
  overflow-wrap: break-word;
  hyphens: auto;
  box-shadow: 0 4px 15px rgba(0, 0, 0, 0.3);
  transition: all 0.3s ease;
}

.message.user .message-content {
  background: var(--user-bubble-bg);
  color: var(--text-light);
  border-bottom-right-radius: 5px; /* 右下角尖角 */
}

.message.assistant .message-content {
  background: var(--bot-bubble-bg);
  color: var(--text-light);
  border-bottom-left-radius: 5px; /* 左下角尖角 */
}

/* loading 动画 */
.loading-dots .message-content {
  display: flex;
  gap: 5px;
  background: var(--bot-bubble-bg);
  padding: 10px 15px;
}

.loading-dots .message-content span {
  width: 8px;
  height: 8px;
  background-color: var(--primary-light);
  border-radius: 50%;
  animation: dot-bounce 1.2s infinite ease-in-out;
}

.loading-dots .message-content span:nth-child(2) {
  animation-delay: 0.2s;
}

.loading-dots .message-content span:nth-child(3) {
  animation-delay: 0.4s;
}

@keyframes dot-bounce {
  0%, 80%, 100% {
    transform: translateY(0);
  }
  40% {
    transform: translateY(-8px);
  }
}

.message-time {
  font-size: 0.75em;
  color: var(--text-dark);
  margin-top: 5px;
  opacity: 0.8;
  text-align: right;
}

.message.assistant .message-time {
  text-align: left;
}

/* 输入区域 */
.input-area {
  display: flex;
  align-items: center;
  padding: 15px;
  border-top: 1px solid var(--glass-border);
  background: rgba(var(--bg-dark), 0.7); /* 略深一点的底部背景 */
  gap: 10px;
}

textarea {
  flex-grow: 1;
  background: rgba(0, 0, 0, 0.3);
  border: 1px solid rgba(var(--primary-rgb), 0.3);
  border-radius: 8px;
  padding: 12px 15px;
  color: var(--text-light);
  font-size: 1em;
  resize: none; /* 禁止手动调整大小 */
  outline: none;
  transition: all 0.3s ease;
  box-shadow: inset 0 0 8px rgba(var(--primary-rgb), 0.1);
  overflow-y: hidden; /* 防止出现滚动条 */
}

textarea:focus {
  border-color: var(--primary-light);
  box-shadow: 0 0 0 3px rgba(var(--primary-rgb), 0.4), inset 0 0 10px rgba(var(--primary-rgb), 0.2);
}

textarea::placeholder {
  color: var(--text-dark);
  opacity: 0.7;
}

button {
  background: linear-gradient(135deg, var(--accent-color), var(--accent-light));
  color: white;
  border: none;
  border-radius: 8px;
  padding: 10px 15px;
  font-size: 1em;
  font-weight: bold;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: all 0.3s ease;
  box-shadow: 0 4px 15px rgba(var(--accent-rgb), 0.5);
  border: 1px solid rgba(var(--accent-rgb), 0.6);
  position: relative;
  overflow: hidden;
}

button:hover {
  transform: translateY(-2px);
  box-shadow: 0 6px 20px rgba(var(--accent-rgb), 0.7);
}

button:active {
  transform: translateY(0);
  box-shadow: 0 2px 10px rgba(var(--accent-rgb), 0.5);
}

button:disabled {
  background: #555;
  cursor: not-allowed;
  box-shadow: none;
  border-color: #666;
}

/* 按钮的微光效果 */
button::after {
  content: '';
  position: absolute;
  top: -50%;
  left: -50%;
  width: 200%;
  height: 200%;
  background: radial-gradient(circle at center, rgba(255, 255, 255, 0.3) 0%, rgba(255, 255, 255, 0) 70%);
  transform: scale(0);
  opacity: 0;
  border-radius: 50%;
  transition: all 0.5s ease-out;
}

button:hover::after {
  transform: scale(1);
  opacity: 1;
}

/* Markdown 增强样式 */
:deep(.markdown-content) {
  line-height: 1.7;
  color: var(--text-light);
}

:deep(.markdown-content h1),
:deep(.markdown-content h2),
:deep(.markdown-content h3) {
  margin: 1em 0 0.5em;
  color: var(--primary-light);
  text-shadow: 0 0 5px rgba(var(--primary-rgb), 0.5);
}

:deep(.markdown-content p) {
  margin: 0.5em 0;
}

:deep(.markdown-content ul),
:deep(.markdown-content ol) {
  padding-left: 1.8em;
  margin: 0.5em 0;
}

:deep(.markdown-content li) {
  margin: 0.4em 0;
}

:deep(.markdown-content strong) {
  color: var(--accent-light);
}

:deep(.markdown-content em) {
  font-style: italic;
  color: var(--text-dark);
}

:deep(.markdown-content a) {
  color: var(--primary-light);
  text-decoration: none;
  border-bottom: 1px dotted var(--primary-color);
  transition: color 0.3s ease;
}

:deep(.markdown-content a:hover) {
  color: var(--accent-light);
  border-bottom-color: var(--accent-light);
}

:deep(.markdown-content code) {
  background: rgba(0, 0, 0, 0.4);
  padding: 0.2em 0.5em;
  border-radius: 5px;
  font-family: 'Fira Code', 'Cascadia Code', monospace; /* 程序员字体 */
  font-size: 0.9em;
  color: #a9b7c6; /* 更适合代码的颜色 */
}

:deep(.markdown-content pre) {
  background: rgba(10, 25, 47, 0.8) !important; /* 更深的背景 */
  padding: 1.2em;
  border-radius: 8px;
  overflow-x: auto;
  margin: 1em 0;
  border: 1px solid rgba(var(--primary-rgb), 0.2);
  box-shadow: inset 0 0 15px rgba(var(--primary-rgb), 0.1);
}

:deep(.markdown-content pre code) {
  background: none;
  padding: 0;
  color: #cddbdf; /* 代码块内的字体颜色 */
}

/* 响应式设计 */
@media (max-width: 768px) {
  .glass-card {
    width: 98%;
    height: 98vh;
    border-radius: 10px;
  }

  .app-header {
    padding: 10px 15px;
  }

  .logo {
    font-size: 1.5em;
  }

  .tech-dots {
    display: none; /* 移动端可以隐藏，保持简洁 */
  }

  .chat-wrapper {
    padding: 10px;
  }

  .message-avatar {
    width: 35px;
    height: 35px;
    margin: 0 8px;
  }

  .message-content {
    max-width: 80%;
    padding: 10px 15px;
    font-size: 0.9em;
  }

  .input-area {
    padding: 10px;
  }

  textarea {
    font-size: 0.9em;
    padding: 10px 12px;
  }

  button {
    padding: 8px 12px;
    font-size: 0.9em;
  }

  .particles {
    opacity: 0.7; /* 移动端粒子可以少一点或透明一点 */
  }

  .border-glow-top, .border-glow-right, .border-glow-bottom, .border-glow-left {
    filter: blur(5px);
    opacity: 0.3;
  }
}
</style>