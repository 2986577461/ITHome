<template>
  <div class="about-page">
    <div class="page-hero hero-accent-blue">
      <div class="hero-bg-mesh"></div>
      <div class="section-inner">
        <div class="section-label reveal">About</div>
        <h1 class="reveal reveal-delay-1">协会简介</h1>
        <p class="reveal reveal-delay-2" style="color:var(--color-text-secondary);font-size:15px;margin-top:8px">
          IT之家协会 — 连接技术与未来
        </p>
      </div>
    </div>

    <section class="section">
      <div class="section-inner">
        <div class="about-content reveal">
          <p>
            IT之家创立于2013年，隶属于绵阳职业技术学院电子与信息学院下的一个学术科技类社团，具有很强的专业色彩和协会特点。IT之家协会主要从事物联网硬件设计和软件开发调试、嵌入式开发等。社团下开设了Web网页设计、JAVA移动终端、C语言程序编写、设计组、开发组、物联网开发等兴趣班，每周进行统一的专业技能素质培训学习，并且为开发组提供能力较强的同学进行专项培养，参加比赛。推崇多元化、自由节奏的学习模式。
          </p>
        </div>
      </div>
    </section>

    <section class="section section-alt">
      <div class="section-inner">
        <div class="section-label reveal">Values</div>
        <h2 class="section-title reveal reveal-delay-1">协会价值</h2>
        <div class="values-grid">
          <div class="value-card gradient-border reveal" v-for="v in values" :key="v.title">
            <div class="value-icon" v-html="v.icon"></div>
            <h3>{{ v.title }}</h3>
            <p>{{ v.desc }}</p>
          </div>
        </div>
      </div>
    </section>
  </div>
</template>

<script setup>
const values = [
  {
    icon: '<svg viewBox="0 0 36 36" fill="none"><path d="M18 4l4 12h12l-10 7 4 12-10-7-10 7 4-12-10-7h12l4-12z" stroke="currentColor" stroke-width="1.5" stroke-linejoin="round"/></svg>',
    title: "提升技能",
    desc: "通过一系列的技术培训和实践活动，帮助学生提升计算机技能，增强就业竞争力。",
  },
  {
    icon: '<svg viewBox="0 0 36 36" fill="none"><circle cx="18" cy="18" r="12" stroke="currentColor" stroke-width="1.5"/><circle cx="18" cy="18" r="4" stroke="currentColor" stroke-width="1.5"/><path d="M18 6v4m0 16v4M6 18h4m16 0h4" stroke="currentColor" stroke-width="1.5" stroke-linecap="round"/></svg>',
    title: "拓宽视野",
    desc: "分享IT业界动态和潮流资讯，帮助学生了解行业前沿和发展趋势。",
  },
  {
    icon: '<svg viewBox="0 0 36 36" fill="none"><path d="M18 6v24m0 0l-6-6m6 6l6-6" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"/><path d="M10 14l-4 4 4 4m16-8l4 4-4 4" stroke="currentColor" stroke-width="1.5" stroke-linecap="round" stroke-linejoin="round"/></svg>',
    title: "锻炼能力",
    desc: "组织各类活动和比赛，锻炼学生的团队协作、沟通协调、创新创造等能力。",
  },
  {
    icon: '<svg viewBox="0 0 36 36" fill="none"><path d="M18 6c5 0 9 3 9 8 0 6-9 14-9 14S9 20 9 14c0-5 4-8 9-8z" stroke="currentColor" stroke-width="1.5"/><circle cx="15" cy="13" r="2" fill="currentColor"/><circle cx="21" cy="13" r="2" fill="currentColor"/><path d="M14 18c1 2 3 3 4 3s3-1 4-3" stroke="currentColor" stroke-width="1.5" stroke-linecap="round"/></svg>',
    title: "服务同学",
    desc: "为广大学生提供电脑技术支持和咨询服务，解决他们在学习和生活中遇到的电脑问题。",
  },
];
</script>

<style scoped>
.about-page {
  padding-top: var(--nav-height);
}
.page-hero {
  background: linear-gradient(180deg, #f0f4ff 0%, #f5f5f7 100%);
}
.page-hero h1 {
  font-family: var(--font-heading);
  font-size: clamp(40px, 6vw, 72px);
  font-weight: 700;
  letter-spacing: -2px;
  line-height: 1.05;
  margin-bottom: 12px;
}
.about-content {
  max-width: 800px;
  margin: 0 auto;
  font-size: 17px;
  line-height: 1.9;
  color: var(--color-text);
}
.about-content p {
  margin-bottom: 1.2em;
  text-indent: 2em;
}

.values-grid {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 24px;
  margin-top: 40px;
}
.value-card {
  padding: 32px;
  border: 1px solid var(--color-border);
  transition: all 0.3s var(--easing-spring);
}
.value-card:hover {
  transform: translateY(-4px);
  box-shadow: var(--shadow-lg);
}
.value-icon {
  width: 36px;
  height: 36px;
  color: var(--color-accent);
  margin-bottom: 16px;
}
.value-card h3 {
  font-family: var(--font-heading);
  font-size: 20px;
  font-weight: 600;
  margin-bottom: 8px;
}
.value-card p {
  color: var(--color-text-secondary);
  font-size: 14px;
  line-height: 1.7;
}

@media (max-width: 640px) {
  .values-grid {
    grid-template-columns: 1fr;
  }
}
</style>
