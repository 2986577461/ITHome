<template>
  <div class="join-page">
    <div class="page-hero hero-accent-green" style="background: linear-gradient(180deg, #f0fff4 0%, #f5f5f7 100%);">
      <div class="hero-bg-mesh"></div>
      <div class="section-inner">
        <div class="pill">加入我们</div>
        <h1>成为 IT 之家的一员</h1>
        <p>一起探索技术的无限可能</p>
      </div>
    </div>

    <section class="section">
      <div class="section-inner">
        <div class="benefits-grid">
          <div v-for="(b, i) in benefits" :key="b.title" :class="['benefit-card', 'reveal', `reveal-delay-${i + 1}`]">
            <div class="bc-icon" v-html="b.icon"></div>
            <h3>{{ b.title }}</h3>
            <p>{{ b.desc }}</p>
          </div>
        </div>
      </div>
    </section>

    <section class="section section-alt">
      <div class="section-inner">
        <div class="apply-trigger" v-if="!formVisible">
          <h2 class="form-title">准备好了吗？</h2>
          <p>加入 IT 之家协会，开启技术学习之旅</p>
          <button class="btn-primary" style="padding:16px 48px;font-size:16px" @click="formVisible = true">
            <svg width="18" height="18" viewBox="0 0 18 18" fill="none"><path d="M9 3v12M3 9h12" stroke="currentColor" stroke-width="2" stroke-linecap="round"/></svg>
            立即申请加入
          </button>
        </div>
        <div class="form-card" v-else>
          <div class="form-card-head">
            <h2 class="form-title">申请加入</h2>
            <button class="form-back" @click="formVisible = false">
              <svg width="18" height="18" viewBox="0 0 18 18" fill="none"><path d="M4 9h10M8 5l-4 4 4 4" stroke="currentColor" stroke-width="1.5" stroke-linecap="round" stroke-linejoin="round"/></svg>
              返回
            </button>
          </div>
          <form @submit.prevent="submitForm" class="form-grid">
            <div class="form-field">
              <label>学号</label>
              <input v-model="form.studentId" type="text" required placeholder="请输入学号" />
            </div>
            <div class="form-field">
              <label>姓名</label>
              <input v-model="form.name" type="text" required placeholder="请输入姓名" />
            </div>
            <div class="form-field">
              <label>性别</label>
              <select v-model="form.sex" required>
                <option value="">请选择</option>
                <option>男</option>
                <option>女</option>
              </select>
            </div>
            <div class="form-field">
              <label>专业</label>
              <input v-model="form.major" type="text" required placeholder="例如：软件技术" />
            </div>
            <div class="form-field">
              <label>班级</label>
              <input v-model="form.className" type="text" required placeholder="例如：252" />
            </div>
            <div class="form-field">
              <label>学院</label>
              <select v-model="form.academy" required>
                <option value="">请选择</option>
                <option v-for="a in academies" :key="a" :value="a">{{ a }}学院</option>
              </select>
            </div>
            <div class="form-field full">
              <label>简介 <span class="req">(50-1000字)</span></label>
              <textarea v-model="form.introduce" placeholder="介绍一下你的信息、爱好、学习计划等" required class="ta"></textarea>
              <div class="intro-progress">
                <div class="intro-bar" :style="{ width: Math.min(form.introduce.length / 10, 100) + '%' }" :class="{ over: form.introduce.length > 1000 }"></div>
              </div>
              <span class="counter" :class="{ over: form.introduce.length > 1000 }">{{ form.introduce.length }}/1000</span>
            </div>
            <div class="form-action full">
              <button type="submit" class="btn-primary" style="padding:14px 56px" :disabled="submitting">
                <template v-if="submitting">
                  <svg class="spinner" width="16" height="16" viewBox="0 0 16 16" fill="none">
                    <circle cx="8" cy="8" r="6" stroke="currentColor" stroke-width="2" stroke-dasharray="30 10" stroke-linecap="round"/>
                  </svg>
                  提交中...
                </template>
                <template v-else>提交申请</template>
              </button>
            </div>
          </form>
        </div>
      </div>
    </section>
  </div>
</template>

<script setup>
import { reactive, ref } from "vue";
import { useRouter } from "vue-router";
import { sendApply } from "@/request/axiosForNewcomers.js";
import { ElMessage } from "element-plus";

const router = useRouter();
const submitting = ref(false);
const formVisible = ref(false);
const academies = ["马克思主义","公共教育","材料与建造","人工智能","智能制造","财经商贸","旅游与管理","艺术设计"];

const benefits = [
  { icon: '<svg viewBox="0 0 32 32" fill="none"><rect x="4" y="6" width="24" height="20" rx="3" stroke="currentColor" stroke-width="1.5"/><path d="M14 16l3 3 5-6" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round"/></svg>', title: "计算机学习教学", desc: "提供大一所应具备的计算机知识，如C语言、HTML、CSS、JavaScript、Java、MySQL" },
  { icon: '<svg viewBox="0 0 32 32" fill="none"><circle cx="16" cy="16" r="10" stroke="currentColor" stroke-width="1.5"/><path d="M16 10v6l4 2" stroke="currentColor" stroke-width="1.5" stroke-linecap="round"/></svg>', title: "编程学习路线", desc: "依据最新企业就业环境，指导当前最流行的技术栈" },
  { icon: '<svg viewBox="0 0 32 32" fill="none"><rect x="4" y="8" width="24" height="18" rx="3" stroke="currentColor" stroke-width="1.5"/><path d="M12 8V6a2 2 0 012-2h4a2 2 0 012 2v2" stroke="currentColor" stroke-width="1.5"/><path d="M8 18h16M8 22h10" stroke="currentColor" stroke-width="1.5" stroke-linecap="round"/></svg>', title: "工作室学习环境", desc: "在工作室学习，提供良好的学习氛围和硬件条件" },
  { icon: '<svg viewBox="0 0 32 32" fill="none"><path d="M6 10h20v16H6z" stroke="currentColor" stroke-width="1.5" rx="2"/><path d="M6 14h20" stroke="currentColor" stroke-width="1.5"/><circle cx="12" cy="20" r="2" stroke="currentColor" stroke-width="1.5"/></svg>', title: "学习资料共享", desc: "秉承开放和共享原则，学员可自行上传和下载学习资料" },
  { icon: '<svg viewBox="0 0 32 32" fill="none"><path d="M16 4l2.5 5.5L24 10.5l-4 4.5 1 6.5-5-3.5-5 3.5 1-6.5-4-4.5 5.5-1L16 4z" stroke="currentColor" stroke-width="1.5"/></svg>', title: "计算机竞赛", desc: "蓝桥杯、职业技能大赛，门槛不高，只需会任意编程语言" },
  { icon: '<svg viewBox="0 0 32 32" fill="none"><circle cx="16" cy="16" r="10" stroke="currentColor" stroke-width="1.5"/><path d="M12 16l3 3 5-5" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round"/></svg>', title: "协会活动", desc: "在团学会带领下开展丰富多彩的社团活动" },
];

const form = reactive({ studentId: "", name: "", sex: "", major: "", className: "", academy: "", introduce: "" });

async function submitForm() {
  const l = form.introduce.length;
  if (l > 1000) { ElMessage.error("简介太长了"); return; }
  if (l < 50) { ElMessage.error("简介至少50字"); return; }
  submitting.value = true;
  try {
    const resp = await sendApply({ ...form });
    if (resp) { ElMessage.success("申请成功！"); setTimeout(() => router.push("/home"), 1200); }
    else ElMessage.error("重复申请");
  } catch { ElMessage.error("提交失败"); }
  finally { submitting.value = false; }
}
</script>

<style scoped>
.join-page { padding-top: var(--nav-height); }
.pill { display: inline-flex; font-size: 12px; font-weight: 600; color: #34c759; background: rgba(52,199,89,.08); padding: 6px 16px; border-radius: 980px; margin-bottom: 16px; }

.benefits-grid { display: grid; grid-template-columns: repeat(3,1fr); gap: 20px; }
.benefit-card { background: var(--color-surface); border: 1px solid var(--color-border); border-radius: var(--radius-md); padding: 24px; transition: all .3s var(--easing-spring); }
.benefit-card:hover { transform: translateY(-4px); box-shadow: var(--shadow-lg); }
.bc-icon { width: 28px; height: 28px; color: var(--color-accent); margin-bottom: 12px; }
.benefit-card h3 { font-family: var(--font-heading); font-size: 16px; font-weight: 600; margin-bottom: 6px; }
.benefit-card p { color: var(--color-text-secondary); font-size: 13px; line-height: 1.6; }

.form-card { max-width: 680px; margin: 0 auto; background: var(--color-surface); border-radius: var(--radius-md); border: 1px solid var(--color-border); padding: 48px; box-shadow: var(--shadow-md); }
.form-title { font-family: var(--font-heading); font-size: 26px; font-weight: 700; text-align: center; margin-bottom: 32px; }
.form-grid { display: grid; grid-template-columns: 1fr 1fr; gap: 18px; }
.form-field { display: flex; flex-direction: column; gap: 5px; }
.form-field.full { grid-column: 1/-1; }
.form-field label { font-size: 13px; font-weight: 600; }
.req { font-weight: 400; color: var(--color-text-tertiary); font-size: 12px; }
.form-field input, .form-field select, .form-field textarea { padding: 10px 14px; border: 1.5px solid var(--color-border); border-radius: var(--radius-sm); font-size: 14px; outline: none; transition: border-color .2s; font-family: var(--font-body); background: var(--color-surface); }
.form-field input:focus, .form-field select:focus, .form-field textarea:focus { border-color: var(--color-accent); box-shadow: 0 0 0 3px rgba(0,113,227,.1); }
.ta { min-height: 140px; resize: vertical; }

.intro-progress { height: 3px; background: var(--color-border); border-radius: 2px; margin-top: 2px; overflow: hidden; }
.intro-bar { height: 100%; background: var(--color-accent); border-radius: 2px; transition: width 0.2s; }
.intro-bar.over { background: #ff3b30; }

.apply-trigger { max-width: 520px; margin: 0 auto; text-align: center; padding: 60px 20px; }
.apply-trigger p { color: var(--color-text-secondary); font-size: 15px; margin-bottom: 28px; }
.form-card-head { display: flex; align-items: center; justify-content: center; position: relative; margin-bottom: 32px; }
.form-card-head .form-title { margin-bottom: 0; }
.form-back {
  position: absolute; left: 0; display: flex; align-items: center; gap: 4px;
  background: none; border: none; color: var(--color-accent); font-size: 14px;
  cursor: pointer; padding: 4px 8px; border-radius: 6px; transition: background .2s;
  font-family: var(--font-body);
}
.form-back:hover { background: rgba(0,113,227,.06); }
.counter { font-size: 11px; color: var(--color-text-tertiary); text-align: right; }
.counter.over { color: #ff3b30; }

.spinner { animation: spin 0.8s linear infinite; }
@keyframes spin { 100% { transform: rotate(360deg); } }

.form-action { grid-column: 1/-1; display: flex; justify-content: center; padding-top: 8px; }

@media (max-width: 640px) {
  .benefits-grid { grid-template-columns: 1fr 1fr; }
  .form-grid { grid-template-columns: 1fr; }
  .form-card { padding: 24px 20px; }
}
</style>
