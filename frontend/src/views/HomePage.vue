<template>
  <div class="home-page">
    <!-- Hero -->
    <section class="hero grain-overlay">
      <div class="hero-bg">
        <div class="orb o1"></div>
        <div class="orb o2"></div>
        <div class="orb o3"></div>
        <div class="grid-line"></div>
      </div>
      <div class="hero-content">
        <h1 class="hero-title reveal reveal-delay-1">
          <span style="color: red" class="fw-light">IT之家</span>
        </h1>
        <p class="hero-desc reveal reveal-delay-2">
          成立于 2013 年，专注物联网硬件设计与软件开发。<br />探索前沿技术，分享实践经验。
        </p>
        <div class="hero-actions reveal reveal-delay-3">
          <router-link to="/about" class="btn-primary"
            >了解我们
            <svg width="16" height="16" viewBox="0 0 16 16" fill="none">
              <path
                d="M4 8h8M10 5l3 3-3 3"
                stroke="currentColor"
                stroke-width="1.8"
                stroke-linecap="round"
                stroke-linejoin="round"
              />
            </svg>
          </router-link>
          <router-link to="/join-us" class="btn-secondary"
            >加入我们</router-link
          >
        </div>
      </div>
      <div class="scroll-indicator">
        <div class="scroll-track"><div class="scroll-dot"></div></div>
        <span>向下滚动</span>
      </div>
    </section>

    <!-- Dashboard: Carousel placeholder + User card -->
    <section class="section section-alt">
      <div class="section-inner">
        <div class="dashboard">
          <div class="carousel-box reveal">
            <div class="carousel-gallery">
              <div class="gallery-grid">
                <div class="gallery-item" v-for="i in 4" :key="i">
                  <img
                    class="gallery-img"
                    src="https://xiaoyan42.oss-cn-chengdu.aliyuncs.com/old-images/560B2F230AA2507CAA636962D9A_79305276_E18C0.png"
                    alt="活动风采"
                  />
                </div>
              </div>
            </div>
            <div class="carousel-caption">活动风采</div>
          </div>
          <div class="user-card reveal reveal-delay-1">
            <div class="uc-header">
              <div class="uc-avatar">{{ (userStore.name || "?")[0] }}</div>
              <div class="uc-meta">
                <span class="uc-name">{{ userStore.name || "游客" }}</span>
                <span class="uc-role">{{
                  userStore.position || "未登录"
                }}</span>
              </div>
            </div>
            <div class="uc-body">
              <div class="uc-row">
                <span>学号</span><span>{{ userStore.studentId || "—" }}</span>
              </div>
              <div class="uc-row">
                <span>学院</span><span>{{ userStore.academy || "—" }}</span>
              </div>
              <div class="uc-row">
                <span>身份</span><span>{{ userStore.position || "—" }}</span>
              </div>
            </div>
            <div class="uc-actions">
              <button
                v-if="userStore.condition"
                class="btn-ghost"
                @click="visibleStore.resetPasswordVisible = true"
              >
                修改密码
              </button>
              <button
                class="btn-primary btn-sm"
                @click="visibleStore.loginOrLogoutButton()"
              >
                {{ visibleStore.getText }}
              </button>
            </div>
          </div>
        </div>
      </div>
    </section>

    <!-- Quick links -->
    <section class="section">
      <div class="section-inner">
        <div class="section-label reveal">Features</div>
        <h2 class="section-title reveal reveal-delay-1">协会服务</h2>
        <div class="features-grid">
          <div
            v-for="(f, i) in features"
            :key="f.title"
            :class="[
              'feature-card',
              'gradient-border',
              'reveal',
              `reveal-delay-${i + 1}`,
            ]"
            @click="$router.push(f.to)"
          >
            <div class="fc-icon" v-html="f.icon"></div>
            <h3>{{ f.title }}</h3>
            <p>{{ f.desc }}</p>
          </div>
        </div>
      </div>
    </section>

    <ResetPasswordDialog />
  </div>
</template>

<script setup>
import { useUserStore } from "@/stores/user";
import { useVisibleStore } from "@/stores/visible";
import ResetPasswordDialog from "@/components/ResetPasswordDialog.vue";

const userStore = useUserStore();
const visibleStore = useVisibleStore();

const features = [
  {
    icon: '<svg viewBox="0 0 36 36" fill="none"><rect x="4" y="6" width="28" height="24" rx="4" stroke="currentColor" stroke-width="1.5"/><path d="M14 18l3 3 5-6" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"/></svg>',
    title: "技术教学",
    desc: "C/C++、前端、数据结构与算法、MySQL数据库、Java、Python/AI 系统教学",
    to: "/tech-study",
  },
  {
    icon: '<svg viewBox="0 0 36 36" fill="none"><path d="M6 8h24v20H6z" stroke="currentColor" stroke-width="1.5" rx="2"/><path d="M6 14h24" stroke="currentColor" stroke-width="1.5"/><circle cx="12" cy="20" r="2" stroke="currentColor" stroke-width="1.5"/></svg>',
    title: "学习资料",
    desc: "开放共享的资源库，支持学员自由上传与下载",
    to: "/learning-resource",
  },
  {
    icon: '<svg viewBox="0 0 36 36" fill="none"><circle cx="18" cy="18" r="12" stroke="currentColor" stroke-width="1.5"/><path d="M18 10v8l6 4" stroke="currentColor" stroke-width="1.5" stroke-linecap="round"/></svg>',
    title: "成果展示",
    desc: "历年来获得的优秀成果与荣誉证书",
    to: "/harvest",
  },
  {
    icon: '<svg viewBox="0 0 36 36" fill="none"><rect x="6" y="12" width="24" height="16" rx="3" stroke="currentColor" stroke-width="1.5"/><circle cx="18" cy="20" r="3" stroke="currentColor" stroke-width="1.5"/><path d="M14 6l4-3 4 3" stroke="currentColor" stroke-width="1.5" stroke-linecap="round" stroke-linejoin="round"/></svg>',
    title: "AI 对话",
    desc: "基于 DeepSeek 的智能助手，随时解答你的问题",
    to: "/ai-dialog",
  },
  {
    icon: '<svg viewBox="0 0 36 36" fill="none"><path d="M18 6c-4 0-7 3-7 7 0 5 7 11 7 11s7-6 7-11c0-4-3-7-7-7z" stroke="currentColor" stroke-width="1.5"/><circle cx="18" cy="13" r="2" stroke="currentColor" stroke-width="1.5"/></svg>',
    title: "加入我们",
    desc: "成为 IT 之家的一员，共同成长进步",
    to: "/join-us",
  },
  {
    icon: '<svg viewBox="0 0 36 36" fill="none"><path d="M6 26l6-8 5 6 4-5 9 11H6z" stroke="currentColor" stroke-width="1.5" stroke-linecap="round" stroke-linejoin="round"/><circle cx="24" cy="12" r="3" stroke="currentColor" stroke-width="1.5"/></svg>',
    title: "协会介绍",
    desc: "了解 IT 之家协会的宗旨、价值与活动",
    to: "/about",
  },
];
</script>

<style scoped>
.hero {
  position: relative;
  min-height: 92vh;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 0 32px;
  padding-top: var(--nav-height);
  background: linear-gradient(180deg, #f0f4ff 0%, #fafafa 40%, #f5f5f7 100%);
  overflow: hidden;
  text-align: center;
}
.hero-bg {
  position: absolute;
  inset: 0;
  z-index: 0;
}
.orb {
  position: absolute;
  border-radius: 50%;
  filter: blur(100px);
  opacity: 0.3;
}
.o1 {
  width: 600px;
  height: 600px;
  background: radial-gradient(circle, #0071e3, #5856d6, transparent);
  top: -200px;
  right: -100px;
  animation: float 20s ease-in-out infinite;
}
.o2 {
  width: 450px;
  height: 450px;
  background: radial-gradient(circle, #5856d6, #af52de, transparent);
  bottom: -150px;
  left: -80px;
  animation: float 25s ease-in-out infinite reverse;
}
.o3 {
  width: 300px;
  height: 300px;
  background: radial-gradient(circle, #40a9ff, #0071e3, transparent);
  top: 40%;
  left: 55%;
  animation: float 18s ease-in-out infinite 5s;
}
@keyframes float {
  0%,
  100% {
    transform: translate(0, 0) scale(1);
  }
  25% {
    transform: translate(40px, -40px) scale(1.06);
  }
  50% {
    transform: translate(-20px, 20px) scale(0.94);
  }
  75% {
    transform: translate(20px, 30px) scale(1.03);
  }
}
.grid-line {
  position: absolute;
  inset: 0;
  background-image:
    linear-gradient(rgba(0, 0, 0, 0.03) 1px, transparent 1px),
    linear-gradient(90deg, rgba(0, 0, 0, 0.03) 1px, transparent 1px);
  background-size: 48px 48px;
  mask-image: radial-gradient(ellipse at center, black 25%, transparent 75%);
}
.hero-content {
  position: relative;
  z-index: 1;
  max-width: 720px;
}
@keyframes pulse-dot {
  0%,
  100% {
    opacity: 1;
    transform: scale(1);
  }
  50% {
    opacity: 0.5;
    transform: scale(0.8);
  }
}
.hero-title {
  font-family: var(--font-heading);
  font-size: clamp(48px, 10vw, 100px);
  font-weight: 700;
  line-height: 1.02;
  letter-spacing: -3px;
  margin-bottom: 20px;
}
.fw-light {
  font-weight: 300;
  color: var(--color-text-secondary);
}
.hero-desc {
  font-size: 18px;
  color: var(--color-text-secondary);
  max-width: 520px;
  margin: 0 auto 36px;
  line-height: 1.7;
}
.hero-actions {
  display: flex;
  gap: 14px;
  justify-content: center;
}
.scroll-indicator {
  position: absolute;
  bottom: 32px;
  left: 50%;
  transform: translateX(-50%);
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 6px;
  opacity: 0.4;
}
.scroll-track {
  width: 20px;
  height: 30px;
  border: 1.5px solid var(--color-text-secondary);
  border-radius: 10px;
  display: flex;
  justify-content: center;
  padding-top: 6px;
  animation: scroll-pulse-ring 2.5s infinite;
}
@keyframes scroll-pulse-ring {
  0%,
  100% {
    box-shadow: 0 0 0 0 rgba(0, 0, 0, 0.1);
  }
  50% {
    box-shadow: 0 0 0 6px rgba(0, 0, 0, 0);
  }
}
.scroll-dot {
  width: 3px;
  height: 8px;
  background: var(--color-text-secondary);
  border-radius: 2px;
  animation: scroll-bounce 2s infinite;
}
@keyframes scroll-bounce {
  0%,
  100% {
    transform: translateY(0);
    opacity: 1;
  }
  50% {
    transform: translateY(8px);
    opacity: 0.2;
  }
}
.scroll-indicator span {
  font-size: 10px;
  color: var(--color-text-tertiary);
  letter-spacing: 1px;
}

/* Dashboard */
.dashboard {
  display: flex;
  gap: 24px;
  align-items: stretch;
}
.carousel-box {
  flex: 1;
  min-width: 0;
}
.carousel-gallery {
  border-radius: var(--radius-md);
  overflow: hidden;
}
.gallery-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 4px;
}
.gallery-item {
  aspect-ratio: 16/10;
  overflow: hidden;
  border-radius: 6px;
}
.gallery-img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}
.carousel-caption {
  font-size: 13px;
  color: var(--color-text-tertiary);
  text-align: center;
  margin-top: 10px;
  letter-spacing: 0.5px;
}

.user-card {
  width: 300px;
  flex-shrink: 0;
  background: linear-gradient(145deg, #0071e3, #5856d6);
  border-radius: var(--radius-md);
  padding: 28px;
  color: #fff;
  display: flex;
  flex-direction: column;
  gap: 20px;
}
.uc-header {
  display: flex;
  align-items: center;
  gap: 14px;
}
.uc-avatar {
  width: 44px;
  height: 44px;
  border-radius: 50%;
  background: rgba(255, 255, 255, 0.2);
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 18px;
  font-weight: 600;
}
.uc-meta {
  display: flex;
  flex-direction: column;
  gap: 2px;
}
.uc-name {
  font-size: 16px;
  font-weight: 600;
}
.uc-role {
  font-size: 12px;
  opacity: 0.7;
}
.uc-body {
  display: flex;
  flex-direction: column;
  gap: 12px;
}
.uc-row {
  display: flex;
  justify-content: space-between;
  font-size: 14px;
  opacity: 0.9;
}
.uc-actions {
  display: flex;
  gap: 10px;
  margin-top: auto;
}
.btn-ghost {
  flex: 1;
  padding: 10px;
  border-radius: 980px;
  border: 1px solid rgba(255, 255, 255, 0.3);
  background: transparent;
  color: #fff;
  font-size: 13px;
  cursor: pointer;
  transition: all 0.2s;
}
.btn-ghost:hover {
  background: rgba(255, 255, 255, 0.1);
}
.btn-sm {
  flex: 1;
  padding: 10px;
  font-size: 13px;
  justify-content: center;
}

/* Features */
.features-grid {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 20px;
}
.feature-card {
  background: var(--color-surface);
  border-radius: var(--radius-md);
  padding: 28px;
  border: 1px solid var(--color-border);
  cursor: pointer;
  transition: all 0.35s var(--easing-spring);
}
.feature-card:hover {
  transform: translateY(-6px);
  box-shadow: var(--shadow-lg);
  border-color: transparent;
}
.fc-icon {
  width: 36px;
  height: 36px;
  color: var(--color-accent);
  margin-bottom: 14px;
}
.feature-card h3 {
  font-family: var(--font-heading);
  font-size: 18px;
  font-weight: 600;
  margin-bottom: 8px;
}
.feature-card p {
  color: var(--color-text-secondary);
  font-size: 14px;
  line-height: 1.6;
}

@media (max-width: 860px) {
  .dashboard {
    flex-direction: column;
  }
  .user-card {
    width: 100%;
  }
  .features-grid {
    grid-template-columns: 1fr 1fr;
  }
}
@media (max-width: 540px) {
  .features-grid {
    grid-template-columns: 1fr;
  }
  .gallery-grid {
    grid-template-columns: 1fr;
  }
}
</style>
