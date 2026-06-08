<template>
  <nav class="navbar" :class="{ scrolled }">
    <div class="nav-inner">
      <router-link to="/home" class="nav-logo">
        <svg class="logo-icon" viewBox="0 0 28 28" fill="none">
          <rect width="28" height="28" rx="7" fill="currentColor" />
          <path
            d="M9 14l4 4 6-8"
            stroke="white"
            stroke-width="2.5"
            stroke-linecap="round"
            stroke-linejoin="round"
          />
        </svg>
        <span class="logo-text">IT之家</span>
      </router-link>

      <div class="nav-links">
        <router-link to="/about" class="nav-item">协会介绍</router-link>
        <router-link to="/tech-study" class="nav-item">技术教学</router-link>
        <router-link to="/learning-resource" class="nav-item"
          >学习资料</router-link
        >
        <router-link to="/harvest" class="nav-item">协会成果</router-link>
        <router-link to="/ai-dialog" class="nav-item">AI对话</router-link>
        <router-link to="/join-us" class="nav-item">加入我们</router-link>
        <router-link
          v-if="
            userStore.position === '会长' || userStore.position === '副会长'
          "
          to="/colony-govern"
          class="nav-item"
          >学员管理</router-link
        >
      </div>

      <div class="nav-auth">
        <!-- Logged in -->
        <template v-if="userStore.condition">
          <router-link to="/notifications" class="nav-bell">
            <svg width="18" height="18" viewBox="0 0 20 20" fill="none">
              <path
                d="M10 2a6 6 0 00-6 6v4l-2 3h16l-2-3V8a6 6 0 00-6-6z"
                stroke="currentColor"
                stroke-width="1.4"
                stroke-linecap="round"
              />
              <path
                d="M8 17a2 2 0 004 0"
                stroke="currentColor"
                stroke-width="1.4"
                stroke-linecap="round"
              />
            </svg>
            <span v-if="notifStore.unreadCount > 0" class="bell-dot">{{
              notifStore.unreadCount > 9 ? "9+" : notifStore.unreadCount
            }}</span>
          </router-link>

          <div
            class="user-menu"
            @click.stop="menuOpen = !menuOpen"
            @mouseenter="menuOpen = true"
            @mouseleave="menuOpen = false"
          >
            <div class="user-trigger">
              <img class="nav-avatar" :src="userStore.effectiveAvatar" alt="" />
              <span class="welcome-text">{{ userStore.name }}</span>
              <svg
                :class="{ open: menuOpen }"
                width="10"
                height="10"
                viewBox="0 0 10 10"
                fill="none"
              >
                <path
                  d="M2 4l3 3 3-3"
                  stroke="currentColor"
                  stroke-width="1.5"
                  stroke-linecap="round"
                />
              </svg>
            </div>
            <transition name="drop">
              <div v-show="menuOpen" class="dropdown">
                <router-link
                  to="/profile"
                  class="drop-item"
                  @click="menuOpen = false"
                >
                  <svg width="15" height="15" viewBox="0 0 16 16" fill="none">
                    <circle
                      cx="8"
                      cy="5"
                      r="3"
                      stroke="currentColor"
                      stroke-width="1.3"
                    />
                    <path
                      d="M2 14c0-3.5 3-5 6-5s6 1.5 6 5"
                      stroke="currentColor"
                      stroke-width="1.3"
                      stroke-linecap="round"
                    />
                  </svg>
                  个人中心
                </router-link>
                <router-link
                  to="/notifications"
                  class="drop-item"
                  @click="menuOpen = false"
                >
                  <svg width="15" height="15" viewBox="0 0 16 16" fill="none">
                    <path
                      d="M8 2A4.5 4.5 0 003.5 6.5v3l-1.5 2.5h12l-1.5-2.5v-3A4.5 4.5 0 008 2z"
                      stroke="currentColor"
                      stroke-width="1.3"
                      stroke-linecap="round"
                    />
                    <path
                      d="M6.5 12a1.5 1.5 0 003 0"
                      stroke="currentColor"
                      stroke-width="1.3"
                      stroke-linecap="round"
                    />
                  </svg>
                  消息通知
                  <span v-if="notifStore.unreadCount > 0" class="drop-badge">{{
                    notifStore.unreadCount
                  }}</span>
                </router-link>
                <div class="drop-divider"></div>
                <button
                  class="drop-item"
                  @click="
                    visibleStore.resetPasswordVisible = true;
                    menuOpen = false;
                  "
                >
                  <svg width="15" height="15" viewBox="0 0 16 16" fill="none">
                    <rect
                      x="6"
                      y="8"
                      width="10"
                      height="6"
                      rx="2"
                      stroke="currentColor"
                      stroke-width="1.3"
                    />
                    <path
                      d="M6 8V5a3 3 0 016 0v3"
                      stroke="currentColor"
                      stroke-width="1.3"
                      stroke-linecap="round"
                    />
                  </svg>
                  修改密码
                </button>
                <div class="drop-divider"></div>
                <button class="drop-item drop-danger" @click="handleLogout">
                  <svg width="15" height="15" viewBox="0 0 16 16" fill="none">
                    <path
                      d="M6 14H3a1 1 0 01-1-1V3a1 1 0 011-1h3M11 11l3-3-3-3M14 8H6"
                      stroke="currentColor"
                      stroke-width="1.3"
                      stroke-linecap="round"
                      stroke-linejoin="round"
                    />
                  </svg>
                  退出登录
                </button>
              </div>
            </transition>
          </div>
        </template>

        <!-- Logged out -->
        <button
          v-else
          class="nav-btn nav-btn-primary"
          @click="visibleStore.loginOrLogoutButton()"
        >
          {{ visibleStore.getText }}
        </button>
      </div>
    </div>
    <LoginDialog />
    <ResetPasswordDialog />
  </nav>
</template>

<script setup>
import { ref, onMounted, onUnmounted } from "vue";
import { useRouter } from "vue-router";
import { useUserStore } from "@/stores/user";
import { useVisibleStore } from "@/stores/visible";
import { useNotificationStore } from "@/stores/notification";
import LoginDialog from "./LoginDialog.vue";
import ResetPasswordDialog from "./ResetPasswordDialog.vue";

const router = useRouter();
const userStore = useUserStore();
const visibleStore = useVisibleStore();
const notifStore = useNotificationStore();

const scrolled = ref(false);
const menuOpen = ref(false);

let ticking = false;
function onScroll() {
  if (!ticking) {
    requestAnimationFrame(() => {
      scrolled.value = window.scrollY > 30;
      ticking = false;
    });
    ticking = true;
  }
}

function handleLogout() {
  menuOpen.value = false;
  localStorage.removeItem("authorization");
  userStore.clear();
  notifStore.setNotifications([]);
  router.push("/home");
}

onMounted(() => window.addEventListener("scroll", onScroll, { passive: true }));
onUnmounted(() => window.removeEventListener("scroll", onScroll));
</script>

<style scoped>
.navbar {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  z-index: var(--z-sticky);
  height: var(--nav-height);
  transition:
    background 0.4s var(--easing-smooth),
    backdrop-filter 0.4s var(--easing-smooth),
    border-color 0.4s var(--easing-smooth);
  background: transparent;
  border-bottom: 1px solid transparent;
}
.navbar.scrolled {
  background: rgba(245, 245, 247, 0.82);
  backdrop-filter: blur(24px) saturate(1.4);
  -webkit-backdrop-filter: blur(24px) saturate(1.4);
  border-bottom: 1px solid rgba(0, 0, 0, 0.06);
  box-shadow: 0 1px 0 rgba(0, 0, 0, 0.02), 0 4px 16px rgba(0, 0, 0, 0.03);
}
.nav-inner {
  max-width: var(--max-width);
  margin: 0 auto;
  padding: 0 32px;
  height: 100%;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 24px;
}
.nav-logo {
  display: flex;
  align-items: center;
  gap: 10px;
  text-decoration: none;
  color: var(--color-text);
  flex-shrink: 0;
}
.logo-icon {
  width: 28px;
  height: 28px;
  color: var(--color-accent);
}
.logo-text {
  font-family: var(--font-heading);
  font-weight: 600;
  font-size: 17px;
  letter-spacing: -0.3px;
}
.nav-links {
  display: flex;
  align-items: center;
  gap: 20px;
}
.nav-item {
  text-decoration: none;
  color: var(--color-text-secondary);
  font-size: 13px;
  font-weight: 500;
  transition: color 0.2s;
  font-family: var(--font-body);
  letter-spacing: 0.2px;
  white-space: nowrap;
  position: relative;
}
.nav-item::after {
  content: '';
  position: absolute;
  bottom: -4px;
  left: 0;
  width: 100%;
  height: 2px;
  background: var(--color-accent);
  border-radius: 2px;
  transform: scaleX(0);
  transition: transform 0.2s var(--easing-emphasized);
}
.nav-item:hover,
.nav-item.router-link-active {
  color: var(--color-text);
}
.nav-item:hover::after,
.nav-item.router-link-active::after {
  transform: scaleX(1);
}
.nav-auth {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-left: auto;
  flex-shrink: 0;
}

/* Bell icon */
.nav-bell {
  position: relative;
  color: var(--color-text-secondary);
  transition: color 0.2s;
  display: flex;
}
.nav-bell:hover {
  color: var(--color-text);
}
.bell-dot {
  position: absolute;
  top: -4px;
  right: -6px;
  min-width: 16px;
  height: 16px;
  border-radius: 980px;
  background: #ff3b30;
  color: #fff;
  font-size: 10px;
  font-weight: 700;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 0 4px;
  line-height: 1;
}

/* User menu */
.user-menu {
  position: relative;
  cursor: pointer;
  user-select: none;
}
.user-trigger {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 4px 10px 4px 4px;
  border-radius: 980px;
  transition: background 0.2s;
}
.user-trigger:hover {
  background: rgba(0, 0, 0, 0.04);
}
.nav-avatar {
  width: 28px;
  height: 28px;
  border-radius: 50%;
  object-fit: cover;
  background: var(--color-border);
}
.welcome-text {
  font-size: 13px;
  color: var(--color-text-secondary);
  white-space: nowrap;
  font-weight: 500;
}
.user-trigger svg {
  color: var(--color-text-tertiary);
  transition: transform 0.2s;
}
.user-trigger svg.open {
  transform: rotate(180deg);
}

/* Dropdown */
.dropdown {
  position: absolute;
  top: calc(100% + 6px);
  right: 0;
  min-width: 200px;
  background: var(--color-surface);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-sm);
  box-shadow: var(--shadow-lg);
  padding: 6px;
  z-index: var(--z-dropdown);
}
.drop-enter-active {
  transition: opacity 0.2s var(--easing-spring), transform 0.25s var(--easing-spring);
}
.drop-leave-active {
  transition: opacity 0.12s, transform 0.15s;
}
.drop-enter-from,
.drop-leave-to {
  opacity: 0;
  transform: translateY(-10px) scale(0.96);
}
.drop-item {
  display: flex;
  align-items: center;
  gap: 10px;
  width: 100%;
  padding: 9px 12px;
  border-radius: 8px;
  font-size: 13px;
  color: var(--color-text);
  text-decoration: none;
  background: none;
  border: none;
  cursor: pointer;
  text-align: left;
  transition: background 0.15s;
  font-family: var(--font-body);
}
.drop-item:hover {
  background: var(--color-bg);
}
.drop-item svg {
  color: var(--color-text-tertiary);
  flex-shrink: 0;
}
.drop-danger {
  color: #ff3b30;
}
.drop-danger svg {
  color: #ff3b30;
}
.drop-divider {
  height: 1px;
  background: var(--color-border);
  margin: 4px 8px;
}
.drop-badge {
  margin-left: auto;
  background: var(--color-accent);
  color: #fff;
  font-size: 10px;
  font-weight: 700;
  min-width: 18px;
  height: 18px;
  border-radius: 980px;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 0 5px;
}

.nav-btn {
  background: transparent;
  border: 1px solid var(--color-border);
  color: var(--color-text);
  padding: 6px 14px;
  border-radius: 980px;
  font-size: 12px;
  font-weight: 500;
  cursor: pointer;
  transition: all 0.2s var(--easing-spring);
  font-family: var(--font-body);
  white-space: nowrap;
}
.nav-btn:hover {
  border-color: var(--color-accent);
  color: var(--color-accent);
}
.nav-btn-primary {
  background: var(--color-accent);
  border-color: var(--color-accent);
  color: #fff;
}
.nav-btn-primary:hover {
  background: var(--color-accent-light);
  border-color: var(--color-accent-light);
  color: #fff;
}
@media (max-width: 860px) {
  .nav-links {
    display: none;
  }
  .nav-inner {
    padding: 0 16px;
  }
  .welcome-text {
    display: none;
  }
}
</style>
