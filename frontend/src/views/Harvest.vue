<template>
  <div class="harvest-page">
    <div class="page-hero hero-accent-orange" style="background: linear-gradient(180deg, #fef5f0 0%, #f5f5f7 100%);">
      <div class="hero-bg-mesh"></div>
      <div class="section-inner">
        <div class="pill">荣誉</div>
        <h1>协会成果</h1>
        <p>历年来获得的荣誉与成就</p>
      </div>
    </div>

    <section class="section">
      <div class="section-inner">
        <div class="cert-grid">
          <div
            v-for="(c, i) in certs"
            :key="i"
            class="cert-card"
            :style="{ transitionDelay: i * 0.08 + 's' }"
          >
            <div class="cert-ribbon">
              <svg viewBox="0 0 24 24" fill="none">
                <path
                  d="M12 2l2.5 5.5L20 8.5l-4 4.5 1 6L12 16l-5 3 1-6-4-4.5 5.5-1L12 2z"
                  stroke="currentColor"
                  stroke-width="1.2"
                />
              </svg>
            </div>
            <div class="cert-badge">荣誉证书</div>
            <div class="cert-visual">
              <img class="cert-img" :src="c.img" :alt="c.name" />
            </div>
            <div class="cert-body">
              <h3>{{ c.name }}</h3>
              <p>{{ c.desc }}</p>
            </div>
          </div>
        </div>
      </div>
    </section>
  </div>
</template>

<script setup>
const certs = [
  {
    name: "孙宇",
    desc: "在2022-2023学年中表现优异，成绩优秀。",
    img: "https://xiaoyan42.oss-cn-chengdu.aliyuncs.com/old-images/medal1.png",
  },
  {
    name: "任周燃",
    desc: "在2020-2021学年中表现优异，成绩优秀。",
    img: "https://xiaoyan42.oss-cn-chengdu.aliyuncs.com/old-images/medal3.png",
  },
  {
    name: "李仁杰",
    desc: "在四川技能大赛物联网安装调试中，成绩优秀。",
    img: "https://xiaoyan42.oss-cn-chengdu.aliyuncs.com/old-images/medal6.png",
  },
  {
    name: "李毅 曹峥",
    desc: "在四川技能大赛计算机程序设计中，成绩优秀。",
    img: "https://xiaoyan42.oss-cn-chengdu.aliyuncs.com/old-images/medal7.png",
  },
  {
    name: "靳玉超",
    desc: "第十五届蓝桥杯C/C++本科组省赛二等奖。",
    img: "https://xiaoyan42.oss-cn-chengdu.aliyuncs.com/old-images/medal2.png",
  },
  {
    name: "曹峥",
    desc: "鲲鹏应用创新大赛省赛二等奖。",
    img: "https://xiaoyan42.oss-cn-chengdu.aliyuncs.com/old-images/medal9.png",
  },
];
</script>

<style scoped>
.harvest-page {
  padding-top: var(--nav-height);
}
.pill {
  display: inline-flex;
  font-size: 12px;
  font-weight: 600;
  color: #ff6b00;
  background: rgba(255, 107, 0, 0.08);
  padding: 6px 16px;
  border-radius: 980px;
  margin-bottom: 16px;
}

.cert-grid {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 24px;
  padding: 40px 0;
}
.cert-card {
  background: var(--color-surface);
  border-radius: var(--radius-md);
  border: 1px solid var(--color-border);
  overflow: hidden;
  transition: all 0.35s var(--easing-spring);
  position: relative;
  perspective: 600px;
}
.cert-card:hover {
  transform: translateY(-6px) rotateX(2deg);
  box-shadow: 0 24px 48px rgba(0, 0, 0, 0.1);
}
.cert-ribbon {
  position: absolute;
  top: 12px;
  right: 12px;
  width: 20px;
  height: 20px;
  color: #ffd700;
  opacity: 0.6;
}
.cert-badge {
  display: inline-block;
  font-size: 12px;
  font-weight: 600;
  color: #b80b22;
  background: rgba(184, 11, 34, 0.06);
  padding: 4px 14px;
  border-radius: 980px;
  margin: 20px 20px 0;
}
.cert-visual {
  height: 200px;
  overflow: hidden;
  background: linear-gradient(135deg, #fef9f0, #fff4e6);
  margin: 16px;
  border-radius: var(--radius-sm);
}
.cert-img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}
.cert-body {
  padding: 0 20px 24px;
}
.cert-body h3 {
  font-family: var(--font-heading);
  font-size: 17px;
  font-weight: 600;
  margin-bottom: 6px;
}
.cert-body p {
  color: var(--color-text-secondary);
  font-size: 13px;
  line-height: 1.6;
}

@media (max-width: 860px) {
  .cert-grid {
    grid-template-columns: repeat(2, 1fr);
  }
}
@media (max-width: 540px) {
  .cert-grid {
    grid-template-columns: 1fr;
  }
}
</style>
