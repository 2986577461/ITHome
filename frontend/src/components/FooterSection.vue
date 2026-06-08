<template>
  <footer class="footer">
    <div class="footer-inner">
      <div class="footer-top">
        <div class="footer-brand reveal">
          <a href="#" class="footer-logo">
            <svg class="logo-icon" viewBox="0 0 28 28" fill="none">
              <rect width="28" height="28" rx="7" fill="currentColor"/>
              <path d="M9 14l4 4 6-8" stroke="white" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round"/>
            </svg>
            <span class="logo-text">IT之家协会</span>
          </a>
          <p class="footer-desc">连接技术与未来<br/>让每一行代码都有温度</p>
          <div class="footer-social">
            <a href="#" class="social-link" v-for="s in socials" :key="s.label" :aria-label="s.label">
              <svg v-html="s.icon" viewBox="0 0 18 18" fill="none" width="16" height="16"></svg>
            </a>
          </div>
        </div>
        <div class="footer-links" v-for="group in linkGroups" :key="group.title">
          <h4 class="link-group-title">{{ group.title }}</h4>
          <router-link v-for="link in group.links" :key="link.to" :to="link.to" class="footer-link">{{ link.label }}</router-link>
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
    label: 'GitHub',
    icon: '<path d="M9 1a8 8 0 00-2.53 15.59c.4.07.55-.17.55-.38l-.01-1.49c-2.01.37-2.53-.49-2.69-.94-.09-.23-.48-.94-.82-1.13-.28-.15-.68-.52-.01-.53.63-.01 1.08.58 1.23.82.72 1.21 1.87.87 2.33.66.07-.52.28-.87.51-1.07-1.78-.2-3.64-.89-3.64-3.95 0-.87.31-1.59.82-2.15-.08-.2-.36-1.02.08-2.12 0 0 .67-.21 2.2.82a7.66 7.66 0 014 0c1.53-1.04 2.2-.82 2.2-.82.44 1.1.16 1.92.08 2.12.51.56.82 1.27.82 2.15 0 3.07-1.87 3.75-3.65 3.95.29.25.54.73.54 1.48l-.01 2.2c0 .21.15.46.55.38A8.01 8.01 0 009 1z" stroke="currentColor" stroke-width="1.2"/>',
  },
  {
    label: '微信',
    icon: '<path d="M6.5 4c-2.2 0-4 1.3-4 3 0 1 .6 2 1.5 2.6l-.5 1.4 1.8-1c.4.1.8.1 1.2.1.2 0 .4 0 .6-.1C6.2 9.1 5.5 7.9 5.5 6.5c0-.9.3-1.7.7-2.4C6 4 6.2 4 6.5 4zM13 8c-1.7 0-3 1-3 2.5S11.3 13 13 13c.4 0 .8 0 1.1-.1l1.4.8-.4-1.2c.7-.5 1.1-1.2 1.1-2 0-1.5-1.3-2.5-3-2.5z" stroke="currentColor" stroke-width="1.3" stroke-linejoin="round"/>',
  },
  {
    label: 'Bilibili',
    icon: '<path d="M3.5 6.5h11v6a2 2 0 01-2 2h-7a2 2 0 01-2-2v-6z" stroke="currentColor" stroke-width="1.2"/><path d="M2 4.5h14v2H2z" stroke="currentColor" stroke-width="1.2"/><circle cx="6.5" cy="10" r=".8" fill="currentColor"/><circle cx="11.5" cy="10" r=".8" fill="currentColor"/><path d="M7.5 12.5c.5.5 1 .8 1.5.8s1-.3 1.5-.8" stroke="currentColor" stroke-width="1" stroke-linecap="round"/>',
  },
  {
    label: '知乎',
    icon: '<path d="M7 4l-1 6h3l-1 5 4-7H9l2-4H7z" stroke="currentColor" stroke-width="1.3" stroke-linejoin="round"/>',
  },
]
const linkGroups = [
  {
    title: '学习',
    links: [
      { to: '/tech-study', label: '技术教学' },
      { to: '/learning-resource', label: '学习资料' },
      { to: '/harvest', label: '协会成果' },
    ],
  },
  {
    title: '互动',
    links: [
      { to: '/ai-dialog', label: 'AI对话' },
      { to: '/join-us', label: '加入我们' },
    ],
  },
  {
    title: '关于',
    links: [
      { to: '/about', label: '协会介绍' },
      { to: '/home', label: '首页' },
    ],
  },
]
</script>

<style scoped>
.footer {
  background: var(--color-text);
  color: rgba(255, 255, 255, 0.6);
  padding: 80px 32px 28px;
  position: relative;
}
.footer::before {
  content: '';
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  height: 1px;
  background: linear-gradient(90deg, transparent, rgba(255,255,255,0.1), transparent);
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
  .footer-top { grid-template-columns: 1fr 1fr; gap: 32px; }
  .footer-brand { grid-column: 1 / -1; }
  .footer-bottom { flex-direction: column; gap: 8px; text-align: center; }
}
</style>
