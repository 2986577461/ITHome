<template>
  <div class="profile-page">
    <div
      class="page-hero hero-accent-green"
      style="background: linear-gradient(180deg, #f0fdf4 0%, #f5f5f7 100%)"
    >
      <div class="hero-bg-mesh"></div>
      <div class="section-inner">
        <div class="pill">个人中心</div>
        <h1>{{ userStore.name || "我的" }}</h1>
      </div>
    </div>

    <section class="section">
      <div class="section-inner">
        <div class="profile-layout">
          <!-- Left: Avatar card -->
          <div class="profile-side">
            <div class="avatar-card">
              <div
                class="avatar-wrap"
                @click="openAvatarDialog"
                title="修改头像"
              >
                <img
                  :src="avatarPreview || userStore.effectiveAvatar"
                  alt=""
                  class="profile-avatar"
                />
                <div class="avatar-overlay">
                  <svg width="18" height="18" viewBox="0 0 18 18" fill="none">
                    <path
                      d="M13 2l3 3L7 14H4v-3L13 2z"
                      stroke="currentColor"
                      stroke-width="1.5"
                      stroke-linejoin="round"
                    />
                  </svg>
                  <span>修改头像</span>
                </div>
              </div>
              <div class="avatar-info">
                <span>{{ userStore.name }}</span>
                <span class="avatar-pos">{{
                  userStore.position || "学员"
                }}</span>
              </div>
            </div>
          </div>

          <!-- Right: Info + tabs -->
          <div class="profile-main">
            <div class="profile-tabs">
              <button :class="{ active: tab === 'info' }" @click="tab = 'info'">
                个人信息
              </button>
              <button
                :class="{ active: tab === 'articles' }"
                @click="
                  tab = 'articles';
                  fetchMyArticles(1);
                "
              >
                我的文章
              </button>
              <button
                :class="{ active: tab === 'resources' }"
                @click="tab = 'resources'"
              >
                我的资料
              </button>
              <button
                :class="{ active: tab === 'password' }"
                @click="tab = 'password'"
              >
                修改密码
              </button>
            </div>

            <!-- Info tab -->
            <div v-if="tab === 'info'" class="tab-content">
              <div class="form-grid">
                <div class="form-field">
                  <label>学号</label>
                  <input v-model="form.studentId" disabled />
                </div>
                <div class="form-field">
                  <label>姓名</label>
                  <input v-model="form.name" />
                </div>
                <div class="form-field">
                  <label>学院</label>
                  <input v-model="form.academy" />
                </div>
                <div class="form-field">
                  <label>职位</label>
                  <input :value="userStore.position" disabled />
                </div>
              </div>
              <div class="form-action">
                <button class="btn-primary" @click="saveProfile">
                  保存修改
                </button>
              </div>
            </div>

            <!-- Articles tab -->
            <div v-if="tab === 'articles'" class="tab-content">
              <div
                v-for="a in myArticles"
                :key="a.id"
                class="item-card"
                @click="editArticle(a)"
              >
                <div class="item-info">
                  <h4>{{ a.head }}</h4>
                  <span class="item-type">{{ typeLabel(a.type) }}</span>
                </div>
                <time>{{ a.updatedDateTime }}</time>
              </div>
              <div
                v-if="myArticles.length === 0 && !myArticleLoading"
                class="empty-tab"
              >
                <p>还没有发表过文章</p>
                <router-link to="/upload-article" class="btn-primary btn-sm"
                  >去发表</router-link
                >
              </div>
              <div
                v-if="myArticleHasMore || myArticlePage > 1"
                class="page-bar"
              >
                <button
                  :disabled="myArticlePage === 1"
                  @click="prevArticlePage()"
                >
                  <svg width="14" height="14" viewBox="0 0 14 14" fill="none">
                    <path
                      d="M9 2L4 7l5 5"
                      stroke="currentColor"
                      stroke-width="1.5"
                      stroke-linecap="round"
                      stroke-linejoin="round"
                    />
                  </svg>
                </button>
                <span>第 {{ myArticlePage }} 页</span>
                <button
                  :disabled="!myArticleHasMore"
                  @click="nextArticlePage()"
                >
                  <svg width="14" height="14" viewBox="0 0 14 14" fill="none">
                    <path
                      d="M5 2l5 5-5 5"
                      stroke="currentColor"
                      stroke-width="1.5"
                      stroke-linecap="round"
                      stroke-linejoin="round"
                    />
                  </svg>
                </button>
              </div>
            </div>

            <!-- Resources tab -->
            <div v-if="tab === 'resources'" class="tab-content">
              <div v-for="r in myResources" :key="r.id" class="item-card">
                <div class="item-info">
                  <h4>{{ r.head }}</h4>
                  <span class="item-type">{{ r.type }}</span>
                </div>
                <time>{{ r.releaseDateTime }}</time>
              </div>
              <div v-if="myResources.length === 0" class="empty-tab">
                <p>还没有上传过资料</p>
                <router-link to="/learning-resource" class="btn-primary btn-sm"
                  >去上传</router-link
                >
              </div>
            </div>

            <!-- Password tab -->
            <div v-if="tab === 'password'" class="tab-content">
              <div class="form-grid">
                <div class="form-field full">
                  <label>当前密码</label>
                  <input
                    v-model="pwdForm.oldPassword"
                    type="password"
                    placeholder="输入当前密码"
                  />
                </div>
                <div class="form-field full">
                  <label>新密码</label>
                  <input
                    v-model="pwdForm.newPassword"
                    type="password"
                    placeholder="输入新密码"
                  />
                </div>
                <div class="form-field full">
                  <label>确认新密码</label>
                  <input
                    v-model="pwdForm.confirmPassword"
                    type="password"
                    placeholder="再次输入新密码"
                  />
                </div>
              </div>
              <div class="form-action">
                <button class="btn-primary" @click="changePassword">
                  修改密码
                </button>
              </div>
            </div>
          </div>
        </div>
      </div>
    </section>

    <!-- Avatar dialog -->
    <div
      class="avatar-dialog-overlay"
      v-if="avatarDialog"
      @click.self="avatarDialog = false"
    >
      <div class="avatar-dialog">
        <div class="dialog-title">修改头像</div>
        <div class="dialog-body">
          <div class="dialog-left">
            <div class="avatar-preview" @click="triggerUpload">
              <img :src="dialogAvatar" />
              <div class="upload-hint">点击上传</div>
            </div>
            <input
              ref="fileInput"
              type="file"
              accept="image/*"
              hidden
              @change="onFileChange"
            />
          </div>
          <div class="dialog-right">
            <button class="random-btn" @click="doRandomAvatar">随机头像</button>
            <button
              class="confirm-btn"
              @click="submitAvatar"
              :disabled="submittingAvatar"
            >
              {{ submittingAvatar ? "提交中..." : "确定" }}
            </button>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from "vue";
import { useRouter } from "vue-router";
import { useUserStore } from "@/stores/user";
import { useArticleStore } from "@/stores/updateArticle";
import {
  updateProfile,
  getMyArticlesPage,
  getMyResources,
} from "@/request/axiosForProfile.js";
import { ElMessage } from "element-plus";
import { getThis, uploadAvatar } from "@/request/axiosForUser";

const router = useRouter();
const userStore = useUserStore();
const articleStore = useArticleStore();
const tab = ref("info");

const fileInput = ref(null);
const avatarDialog = ref(false);
const avatarPreview = ref("");

// dialogAvatar mirrors the current avatar state, updated on upload or random
const dialogAvatar = ref("");
const selectedFile = ref(null);
const submittingAvatar = ref(false);

const form = reactive({
  studentId: "",
  name: "",
  academy: "",
});

const pwdForm = reactive({
  oldPassword: "",
  newPassword: "",
  confirmPassword: "",
});

const myArticles = ref([]);
const myArticlePage = ref(1);
const myArticleLoading = ref(false);
const myArticleHasMore = ref(false);
const articlePageSize = 5;
const myResources = ref([]);

function typeLabel(t) {
  return (
    { 1: "C/C++", 2: "前端", 3: "数据结构与算法", 4: "MySQL数据库", 5: "Java", 6: "Python/AI" }[
      String(t)
    ] || "其他"
  );
}

// --- Avatar ---
function openAvatarDialog() {
  dialogAvatar.value = avatarPreview.value || userStore.avatar;
  selectedFile.value = null;
  avatarDialog.value = true;
}

function triggerUpload() {
  fileInput.value?.click();
}

function onFileChange(e) {
  const file = e.target.files?.[0];
  if (!file) return;
  selectedFile.value = file;
  const reader = new FileReader();
  reader.onload = (ev) => {
    dialogAvatar.value = ev.target?.result || "";
  };
  reader.readAsDataURL(file);
  e.target.value = "";
}

function doRandomAvatar() {
  selectedFile.value = null;
  dialogAvatar.value = userStore.getRandomAvatar();
}

async function submitAvatar() {
  submittingAvatar.value = true;
  try {
    const fd = new FormData();
    if (selectedFile.value) {
      fd.append("avatar", selectedFile.value);
    } else {
      const resp = await fetch(dialogAvatar.value);
      const blob = await resp.blob();
      const file = new File([blob], "avatar.svg", { type: blob.type });
      fd.append("avatar", file);
    }
    const resp = await uploadAvatar(fd);
    if (resp?.code === "200") {
      avatarPreview.value = resp.data || dialogAvatar.value;
      userStore.setAvatar(avatarPreview.value);
      ElMessage.success("头像已更新");
    } else {
      avatarPreview.value = dialogAvatar.value;
      userStore.setAvatar(dialogAvatar.value);
      ElMessage.success("头像已更新");
    }
    avatarDialog.value = false;
  } catch {
    avatarPreview.value = dialogAvatar.value;
    userStore.setAvatar(dialogAvatar.value);
    ElMessage.success("头像已更新（本地）");
    avatarDialog.value = false;
  } finally {
    submittingAvatar.value = false;
  }
}

async function saveProfile() {
  try {
    const resp = await updateProfile({
      name: form.name,
      academy: form.academy,
    });
    if (resp?.code === "200") {
      userStore.name = form.name;
      userStore.academy = form.academy;
      ElMessage.success("保存成功");
    }
  } catch {
    ElMessage.error("保存失败");
  }
}

async function changePassword() {
  if (!pwdForm.oldPassword || !pwdForm.newPassword) {
    ElMessage.error("请填写完整");
    return;
  }
  if (pwdForm.newPassword !== pwdForm.confirmPassword) {
    ElMessage.error("两次密码不一致");
    return;
  }
  try {
    const resp = await updatePassword({
      oldPassword: pwdForm.oldPassword,
      newPassword: pwdForm.newPassword,
    });
    if (resp?.code === "200") {
      ElMessage.success("密码修改成功");
      pwdForm.oldPassword = "";
      pwdForm.newPassword = "";
      pwdForm.confirmPassword = "";
    } else {
      ElMessage.error(resp?.msg || "修改失败");
    }
  } catch {
    ElMessage.error("修改失败");
  }
}

async function fetchMyArticles(page) {
  myArticleLoading.value = true;
  try {
    const resp = await getMyArticlesPage(page, articlePageSize);
    if (resp?.data) {
      const list = resp.data.records ?? resp.data;
      myArticles.value = Array.isArray(list) ? list : [];
      myArticleHasMore.value = list.length === articlePageSize;
      myArticlePage.value = page;
    }
  } catch {
    myArticles.value = [];
  } finally {
    myArticleLoading.value = false;
  }
}
function prevArticlePage() {
  if (myArticlePage.value > 1) fetchMyArticles(myArticlePage.value - 1);
}
function nextArticlePage() {
  if (myArticleHasMore.value) fetchMyArticles(myArticlePage.value + 1);
}

function editArticle(a) {
  articleStore.setArticle(a.id, a.head, a.content, a.type);
  router.push("/upload-article");
}

onMounted(async () => {
  try {
    const resp = await getThis();
    if (resp?.code === "200" && resp.data) {
      userStore.setUser(resp.data);
    }
  } catch {
    // 后端不可用时使用本地 store 数据
  }

  form.studentId = userStore.studentId || "";
  form.name = userStore.name || "";
  form.academy = userStore.academy || "";

  fetchMyArticles(1);
  getMyResources()
    .then((r) => {
      if (r?.data) myResources.value = r.data;
    })
    .catch(() => {});
});
</script>

<style scoped>
.profile-page {
  padding-top: var(--nav-height);
}
.page-hero {
  position: relative;
  padding: 64px 0 40px;
  text-align: center;
  background: linear-gradient(180deg, #f0fff4 0%, #f5f5f7 100%);
  overflow: hidden;
}
.ph-bg {
  position: absolute;
  inset: 0;
  background: radial-gradient(
    ellipse 50% 60% at 50% 100%,
    rgba(52, 199, 89, 0.05) 0%,
    transparent 70%
  );
}
.pill {
  display: inline-flex;
  font-size: 12px;
  font-weight: 600;
  color: #34c759;
  background: rgba(52, 199, 89, 0.08);
  padding: 6px 16px;
  border-radius: 980px;
  margin-bottom: 16px;
}
.page-hero h1 {
  font-family: var(--font-heading);
  font-size: clamp(32px, 4vw, 48px);
  font-weight: 700;
  letter-spacing: -1.5px;
}

.profile-layout {
  display: flex;
  gap: 28px;
  align-items: flex-start;
}

/* Avatar card */
.profile-side {
  width: 220px;
  flex-shrink: 0;
}
.avatar-card {
  background: var(--color-surface);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-md);
  padding: 28px 20px;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 14px;
}
.avatar-wrap {
  position: relative;
  width: 96px;
  height: 96px;
  border-radius: 50%;
  overflow: hidden;
  cursor: pointer;
}
.profile-avatar {
  width: 100%;
  height: 100%;
  object-fit: cover;
}
.avatar-overlay {
  position: absolute;
  inset: 0;
  border-radius: 50%;
  background: rgba(0, 0, 0, 0.45);
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 4px;
  opacity: 0;
  transition: opacity 0.2s;
  color: #fff;
  font-size: 11px;
  font-weight: 500;
}
.avatar-wrap:hover .avatar-overlay {
  opacity: 1;
}
.avatar-info {
  text-align: center;
}
.avatar-info span {
  display: block;
  font-size: 14px;
  font-weight: 600;
}
.avatar-pos {
  font-size: 12px;
  color: var(--color-text-tertiary);
  margin-top: 2px;
}

/* Main */
.profile-main {
  flex: 1;
  min-width: 0;
}
.profile-tabs {
  display: flex;
  gap: 4px;
  margin-bottom: 24px;
  background: var(--color-surface);
  border-radius: var(--radius-sm);
  padding: 4px;
  border: 1px solid var(--color-border);
}
.profile-tabs button {
  flex: 1;
  padding: 10px;
  border-radius: 8px;
  border: none;
  background: transparent;
  cursor: pointer;
  font-size: 13px;
  font-weight: 500;
  color: var(--color-text-secondary);
  transition: all 0.2s;
  font-family: var(--font-body);
}
.profile-tabs button.active {
  background: var(--color-accent);
  color: #fff;
}

.tab-content {
  background: var(--color-surface);
  border-radius: var(--radius-md);
  border: 1px solid var(--color-border);
  padding: 24px;
}
.form-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 16px;
}
.form-field {
  display: flex;
  flex-direction: column;
  gap: 5px;
}
.form-field.full {
  grid-column: 1/-1;
}
.form-field label {
  font-size: 13px;
  font-weight: 600;
}
.form-field input,
.form-field select {
  padding: 10px 14px;
  border: 1.5px solid var(--color-border);
  border-radius: var(--radius-sm);
  font-size: 14px;
  outline: none;
  transition: border-color 0.2s;
  font-family: var(--font-body);
  background: var(--color-surface);
}
.form-field input:focus {
  border-color: var(--color-accent);
}
.form-field input:disabled {
  background: var(--color-bg);
  color: var(--color-text-tertiary);
}
.form-action {
  margin-top: 20px;
  display: flex;
  gap: 10px;
}

.item-card {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 14px 16px;
  border: 1px solid var(--color-border);
  border-radius: var(--radius-sm);
  margin-bottom: 10px;
  cursor: pointer;
  transition: all 0.15s;
}
.item-card:hover {
  background: var(--color-bg);
}
.item-info {
  display: flex;
  align-items: center;
  gap: 10px;
}
.item-info h4 {
  font-size: 14px;
  font-weight: 600;
}
.item-type {
  font-size: 11px;
  background: var(--color-accent);
  color: #fff;
  padding: 2px 10px;
  border-radius: 980px;
}
.item-card time {
  font-size: 12px;
  color: var(--color-text-tertiary);
}

.empty-tab {
  text-align: center;
  padding: 48px 20px;
  color: var(--color-text-tertiary);
}
.empty-tab p {
  margin-bottom: 16px;
}

.page-bar {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 16px;
  padding-top: 20px;
}
.page-bar span {
  font-size: 14px;
  color: var(--color-text-secondary);
}
.page-bar button {
  width: 32px;
  height: 32px;
  border-radius: 50%;
  border: 1.5px solid var(--color-border);
  background: var(--color-surface);
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  color: var(--color-text-secondary);
  transition: all 0.2s;
}
.page-bar button:hover:not(:disabled) {
  border-color: var(--color-accent);
  color: var(--color-accent);
}
.page-bar button:disabled {
  opacity: 0.3;
  cursor: not-allowed;
}

/* Avatar dialog */
.avatar-dialog-overlay {
  position: fixed;
  inset: 0;
  background: rgba(0, 0, 0, 0.4);
  backdrop-filter: blur(8px);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 300;
}
.avatar-dialog {
  background: #fff;
  border-radius: 16px;
  padding: 32px;
  width: 460px;
  max-width: 92vw;
  box-shadow: 0 20px 60px rgba(0, 0, 0, 0.15);
}
.dialog-title {
  font-size: 20px;
  font-weight: 700;
  text-align: center;
  margin-bottom: 28px;
}
.dialog-body {
  display: flex;
  gap: 40px;
  min-height: 160px;
}
.dialog-left {
  flex-shrink: 0;
}
.dialog-right {
  flex: 1;
  display: flex;
  flex-direction: column;
  justify-content: center;
  gap: 12px;
  padding: 8px 0;
}
.avatar-preview {
  width: 120px;
  height: 120px;
  border-radius: 50%;
  overflow: hidden;
  cursor: pointer;
  position: relative;
  border: 3px solid #e5e7eb;
}
.avatar-preview:hover {
  border-color: #34c759;
}
.avatar-preview img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}
.upload-hint {
  position: absolute;
  inset: 0;
  display: flex;
  align-items: center;
  justify-content: center;
  background: rgba(0, 0, 0, 0.35);
  color: #fff;
  font-size: 12px;
  font-weight: 500;
  opacity: 0;
  transition: opacity 0.2s;
}
.avatar-preview:hover .upload-hint {
  opacity: 1;
}
.random-btn {
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 12px 20px;
  border: 1.5px solid #d1d5db;
  border-radius: 10px;
  background: #fff;
  cursor: pointer;
  font-size: 14px;
  font-weight: 500;
  color: #333;
  transition: all 0.2s;
}
.random-btn:hover {
  border-color: #34c759;
  color: #34c759;
}
.confirm-btn {
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 14px 20px;
  border: none;
  border-radius: 10px;
  background: #34c759;
  color: #fff;
  cursor: pointer;
  font-size: 15px;
  font-weight: 600;
  transition: opacity 0.2s;
}
.confirm-btn:hover {
  opacity: 0.85;
}
.confirm-btn:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

@media (max-width: 640px) {
  .dialog-body {
    flex-direction: column;
    align-items: center;
    gap: 24px;
  }
  .dialog-right {
    width: 100%;
  }
  .avatar-dialog {
    padding: 24px;
  }
}
</style>
