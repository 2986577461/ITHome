<template>
  <div
    class="modal-overlay"
    v-if="governStore.addStudentVisible"
    @click.self="governStore.addStudentVisible = false"
  >
    <div class="modal-card wide">
      <button
        class="modal-close"
        @click="governStore.addStudentVisible = false"
      >
        <svg width="18" height="18" viewBox="0 0 18 18" fill="none">
          <path
            d="M4 4l10 10M14 4L4 14"
            stroke="currentColor"
            stroke-width="1.5"
            stroke-linecap="round"
          />
        </svg>
      </button>

      <div class="dialog-head">
        <div class="dialog-head-icon">
          <svg width="22" height="22" viewBox="0 0 24 24" fill="none">
            <path
              d="M17 21v-2a4 4 0 00-4-4H5a4 4 0 00-4 4v2M9 11a4 4 0 100-8 4 4 0 000 8zM23 21v-2a4 4 0 00-3-3.87M16 3.13a4 4 0 010 7.75"
              stroke="currentColor"
              stroke-width="1.6"
              stroke-linecap="round"
              stroke-linejoin="round"
            />
          </svg>
        </div>
        <h2>新学员申请</h2>
        <p v-if="governStore.newcomers.length">
          {{ governStore.newcomers.length }} 人待审核
        </p>
      </div>

      <div v-if="governStore.newcomers.length === 0" class="empty-state">
        <svg width="48" height="48" viewBox="0 0 48 48" fill="none">
          <path
            d="M34 42v-4a8 8 0 00-8-8H10a8 8 0 00-8 8v4M18 22a8 8 0 100-16 8 8 0 000 16z"
            stroke="currentColor"
            stroke-width="1.5"
            opacity=".3"
            stroke-linecap="round"
            stroke-linejoin="round"
          />
        </svg>
        <p>暂无申请</p>
      </div>

      <div v-else class="student-list">
        <div
          v-for="(s, i) in governStore.newcomers"
          :key="s.id || i"
          class="student-card"
        >
          <div class="sc-body">
            <div class="sc-header">
              <span class="sc-name">{{ s.name }}</span>
              <span class="sc-id">{{ s.studentId }}</span>
            </div>
            <div class="sc-info-grid">
              <div class="sc-info-item">
                <span class="sc-label">性别</span>
                <span>{{ s.sex }}</span>
              </div>
              <div class="sc-info-item">
                <span class="sc-label">学院</span>
                <span>{{ s.academy }}</span>
              </div>
              <div class="sc-info-item">
                <span class="sc-label">专业</span>
                <span>{{ s.major }}</span>
              </div>
              <div class="sc-info-item">
                <span class="sc-label">班级</span>
                <span>{{ s.className }}</span>
              </div>
            </div>
            <p class="sc-intro">{{ s.introduce }}</p>
          </div>
          <div class="sc-actions">
            <button class="sc-btn-approve" @click="handleApprove(s.id)">
              <svg width="14" height="14" viewBox="0 0 16 16" fill="none">
                <path
                  d="M4 8l3 3 5-6"
                  stroke="currentColor"
                  stroke-width="2"
                  stroke-linecap="round"
                  stroke-linejoin="round"
                />
              </svg>
              通过
            </button>
            <button class="sc-btn-reject" @click="handleRefuse(s.id)">
              <svg width="14" height="14" viewBox="0 0 16 16" fill="none">
                <path
                  d="M4 4l8 8M12 4l-8 8"
                  stroke="currentColor"
                  stroke-width="1.8"
                  stroke-linecap="round"
                />
              </svg>
              拒绝
            </button>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { useGovernStore } from "@/stores/govern";
import { agree, refuse } from "@/request/axiosForNewcomers";
import { ElMessage } from "element-plus";

const governStore = useGovernStore();

async function handleApprove(id) {
  const resp = await agree(id);
  if (resp.code === "200") {
    ElMessage.success("已通过申请");
    const s = governStore.newcomers.find((item) => item.id === id);
    if (s) {
      s.position = "学员";
      s.articleCount = 0;
      s.resourceCount = 0;
      governStore.member.push(s);
    }
    governStore.newcomers = governStore.newcomers.filter(
      (item) => item.id !== id,
    );
  }
}

async function handleRefuse(id) {
  const s = governStore.newcomers.find((item) => item.id === id);
  governStore.newcomers = governStore.newcomers.filter(
    (item) => item.id !== id,
  );
  const resp = await refuse(id);
  if (resp) {
    ElMessage.warning("已拒绝申请");
  }
}
</script>

<style scoped>
.dialog-head {
  text-align: center;
  margin-bottom: 24px;
}
.dialog-head-icon {
  width: 48px;
  height: 48px;
  border-radius: 50%;
  background: linear-gradient(135deg, #34c759, #30b350);
  color: #fff;
  display: flex;
  align-items: center;
  justify-content: center;
  margin: 0 auto 14px;
}
.dialog-head h2 {
  font-family: var(--font-heading);
  font-size: 22px;
  font-weight: 700;
  margin-bottom: 4px;
}
.dialog-head p {
  font-size: 14px;
  color: var(--color-text-tertiary);
}

.empty-state {
  text-align: center;
  padding: 60px 20px;
  color: var(--color-text-tertiary);
}
.empty-state p {
  margin-top: 12px;
}

.student-list {
  display: flex;
  flex-direction: column;
  gap: 16px;
  max-height: 60vh;
  overflow-y: auto;
}
.student-card {
  display: flex;
  gap: 20px;
  padding: 20px;
  border: 1px solid var(--color-border);
  border-radius: var(--radius-md);
  background: var(--color-surface);
  transition: border-color 0.2s;
}
.student-card:hover {
  border-color: var(--color-accent);
}

.sc-body {
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: 12px;
}
.sc-header {
  display: flex;
  align-items: baseline;
  gap: 12px;
}
.sc-name {
  font-size: 18px;
  font-weight: 700;
}
.sc-id {
  font-size: 13px;
  color: var(--color-text-tertiary);
}

.sc-info-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 8px 20px;
}
.sc-info-item {
  display: flex;
  gap: 8px;
  font-size: 14px;
}
.sc-label {
  color: var(--color-text-tertiary);
  min-width: 36px;
  flex-shrink: 0;
}
.sc-intro {
  font-size: 15px;
  color: var(--color-text-secondary);
  line-height: 1.7;
  padding: 12px;
  background: var(--color-bg);
  border-radius: var(--radius-sm);
  word-break: break-word;
  white-space: pre-wrap;
}

.sc-actions {
  display: flex;
  flex-direction: column;
  gap: 8px;
  flex-shrink: 0;
  justify-content: center;
}
.sc-btn-approve {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 8px 22px;
  border-radius: 980px;
  border: none;
  background: linear-gradient(135deg, #34c759, #30b350);
  color: #fff;
  font-size: 14px;
  font-weight: 600;
  cursor: pointer;
  font-family: var(--font-heading);
  transition: all 0.25s var(--easing-spring);
  box-shadow: 0 3px 10px rgba(52, 199, 89, 0.25);
}
.sc-btn-approve:hover {
  transform: translateY(-1px);
  box-shadow: 0 5px 16px rgba(52, 199, 89, 0.35);
}
.sc-btn-reject {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 8px 22px;
  border-radius: 980px;
  border: 1.5px solid #ff3b30;
  background: rgba(255, 59, 48, 0.06);
  color: #ff3b30;
  font-size: 14px;
  font-weight: 500;
  cursor: pointer;
  font-family: var(--font-heading);
  transition: all 0.2s;
}
.sc-btn-reject:hover {
  background: #ff3b30;
  color: #fff;
}
</style>
