<template>
  <footer class="footer">
    <div class="footer-inner">
      <div class="footer-top">
        <div class="footer-brand reveal">
          <a href="#" class="footer-logo">
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
            <span class="logo-text">IT之家协会</span>
          </a>
          <p class="footer-desc">连接技术与未来<br />让每一行代码都有温度</p>
          <div class="footer-social">
            <a
              :href="s.url"
              target="_blank"
              class="social-link"
              v-for="s in socials"
              :key="s.label"
              :aria-label="s.label"
            >
              <img :src="s.icon" :alt="s.label" width="18" height="18" />
            </a>
          </div>
        </div>
        <div
          class="footer-links"
          v-for="group in linkGroups"
          :key="group.title"
        >
          <h4 class="link-group-title">{{ group.title }}</h4>
          <router-link
            v-for="link in group.links"
            :key="link.to"
            :to="link.to"
            class="footer-link"
            >{{ link.label }}</router-link
          >
        </div>
      </div>
      <div class="footer-bottom">
        <span>&copy; 2026 IT 之家协会. All rights reserved.</span>
        <div class="footer-bottom-links">
          <a href="#">隐私政策</a>
          <a href="#">服务条款</a>
        </div>
      </div>
    </div>
  </footer>
</template>

<script setup>
const socials = [
  {
    label: "GitHub",
    icon: "/github.png",
    url: "https://github.com/2986577461/ITHome#",
  },
  { label: "微信", icon: "/wechat.png", url: "/wechat-qr.png" },
  { label: "QQ", icon: "/qq-penguin-shape.png", url: "/qq-qr.png" },
  {
    label: "抖音",
    icon: "/tiktok.png",
    url: "https://v.douyin.com/O6Ila2rK5dQ",
  },
];
const linkGroups = [
  {
    title: "学习",
    links: [
      { to: "/tech-study", label: "技术教学" },
      { to: "/learning-resource", label: "学习资料" },
      { to: "/harvest", label: "协会成果" },
    ],
  },
  {
    title: "互动",
    links: [
      { to: "/ai-dialog", label: "AI对话" },
      { to: "/join-us", label: "加入我们" },
    ],
  },
  {
    title: "关于",
    links: [
      { to: "/about", label: "协会介绍" },
      { to: "/home", label: "首页" },
    ],
  },
];
</script>

<style scoped>
.footer {
  background: var(--color-text);
  color: rgba(255, 255, 255, 0.6);
  padding: 80px 32px 28px;
  position: relative;
}
.footer::before {
  content: "";
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  height: 1px;
  background: linear-gradient(
    90deg,
    transparent,
    rgba(255, 255, 255, 0.1),
    transparent
  );
}

.footer-inner {
  max-width: var(--max-width);
  margin: 0 auto;
}

.footer-top {
  display: grid;
  grid-template-columns: 2fr 1fr 1fr 1fr;
  gap: 48px;
  padding-bottom: 48px;
  border-bottom: 1px solid rgba(255, 255, 255, 0.08);
}

.footer-logo {
  display: inline-flex;
  align-items: center;
  gap: 10px;
  margin-bottom: 16px;
  text-decoration: none;
}

.logo-icon {
  width: 26px;
  height: 26px;
  color: var(--color-accent);
}

.logo-text {
  font-family: var(--font-heading);
  font-weight: 600;
  color: #fff;
  font-size: 16px;
  letter-spacing: -0.3px;
}

.footer-desc {
  font-size: 13px;
  line-height: 1.7;
  margin-bottom: 24px;
}

.footer-social {
  display: flex;
  gap: 10px;
}

.social-link {
  width: 34px;
  height: 34px;
  border-radius: 10px;
  background: rgba(255, 255, 255, 0.06);
  display: flex;
  align-items: center;
  justify-content: center;
  text-decoration: none;
  color: rgba(255, 255, 255, 0.4);
  transition: all 0.2s;
}

.social-link:hover {
  background: var(--color-accent);
  color: #fff;
  transform: translateY(-2px);
  box-shadow: 0 0 16px rgba(0, 113, 227, 0.4);
}

.link-group-title {
  font-family: var(--font-heading);
  font-size: 12px;
  font-weight: 600;
  color: #fff;
  margin-bottom: 20px;
  letter-spacing: 0.8px;
  text-transform: uppercase;
}

.footer-link {
  display: block;
  color: rgba(255, 255, 255, 0.45);
  text-decoration: none;
  font-size: 13px;
  margin-bottom: 12px;
  transition: color 0.2s;
}

.footer-link:hover {
  color: #fff;
}

.footer-bottom {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding-top: 24px;
  font-size: 11px;
  color: rgba(255, 255, 255, 0.35);
}

.footer-bottom-links {
  display: flex;
  gap: 24px;
}

.footer-bottom-links a {
  color: rgba(255, 255, 255, 0.35);
  text-decoration: none;
  transition: color 0.2s;
}

.footer-bottom-links a:hover {
  color: rgba(255, 255, 255, 0.6);
}

@media (max-width: 768px) {
  .footer-top {
    grid-template-columns: 1fr 1fr;
    gap: 32px;
  }
  .footer-brand {
    grid-column: 1 / -1;
  }
  .footer-bottom {
    flex-direction: column;
    gap: 8px;
    text-align: center;
  }
}
</style>
