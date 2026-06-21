<template>
  <div
    class="modal-overlay"
    v-if="governStore.updateStudentVisible"
    @click.self="governStore.updateStudentVisible = false"
  >
    <div class="modal-card wide">
      <button
        class="modal-close"
        @click="governStore.updateStudentVisible = false"
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
              d="M16 3l5 5L8 21H3v-5L16 3z"
              stroke="currentColor"
              stroke-width="1.8"
              stroke-linejoin="round"
            />
          </svg>
        </div>
        <h2>学员信息修改</h2>
      </div>
      <form @submit.prevent="submitUpdate" class="update-form">
        <div class="form-row">
          <div class="form-field">
            <label>姓名</label>
            <input
              v-model="governStore.updateStudent.name"
              type="text"
              placeholder="请输入姓名"
            />
          </div>
          <div class="form-field">
            <label>性别</label>
            <div class="pill-group">
              <button
                type="button"
                :class="{ active: governStore.updateStudent.sex === '男' }"
                @click="governStore.updateStudent.sex = '男'"
              >
                男
              </button>
              <button
                type="button"
                :class="{ active: governStore.updateStudent.sex === '女' }"
                @click="governStore.updateStudent.sex = '女'"
              >
                女
              </button>
            </div>
          </div>
        </div>
        <div class="form-row">
          <div class="form-field">
            <label>专业</label>
            <input
              v-model="governStore.updateStudent.major"
              type="text"
              placeholder="例如：软件技术"
            />
          </div>
          <div class="form-field">
            <label>班级</label>
            <input
              v-model="governStore.updateStudent.className"
              type="text"
              placeholder="例如：252"
            />
          </div>
        </div>
        <div class="form-row">
          <div class="form-field">
            <label>学院</label>
            <input
              v-model="governStore.updateStudent.academy"
              type="text"
              placeholder="例如：人工智能"
            />
          </div>
          <div class="form-field">
            <label>职位</label>
            <div class="pill-group">
              <button
                type="button"
                :class="{
                  active: governStore.updateStudent.position === 'user',
                }"
                @click="governStore.updateStudent.position = 'user'"
              >
                学员
              </button>
              <button
                type="button"
                :class="{
                  active: governStore.updateStudent.position === 'admin',
                }"
                @click="governStore.updateStudent.position = 'admin'"
              >
                会长
              </button>
            </div>
          </div>
        </div>
        <div class="form-field">
          <label>新密码 <span class="req">(留空不修改)</span></label>
          <input
            v-model="governStore.updateStudent.password"
            type="text"
            placeholder="留空则不改密码"
          />
        </div>
        <div class="form-actions">
          <button
            type="button"
            class="btn-secondary"
            @click="governStore.updateStudentVisible = false"
          >
            取消
          </button>
          <button type="submit" class="btn-primary">保存修改</button>
        </div>
      </form>
    </div>
  </div>
</template>

<script setup>
import { useGovernStore } from "@/stores/govern";
import { update } from "@/request/axiosForUser";
import { ElMessage } from "element-plus";

const governStore = useGovernStore();

async function submitUpdate() {
  if (!governStore.updateStudent.name || !governStore.updateStudent.className) {
    ElMessage.error("姓名和班级不能为空");
    return;
  }
  if ((await update(governStore.updateStudent)).code === "200") {
    ElMessage.success("修改成功");
  }
  governStore.updateStudentVisible = false;
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
  background: linear-gradient(
    135deg,
    var(--color-accent),
    var(--color-gradient-mid)
  );
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
}

.update-form {
  display: flex;
  flex-direction: column;
  gap: 18px;
}
.form-row {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 18px;
}
.form-field {
  display: flex;
  flex-direction: column;
  gap: 6px;
}
.form-field label {
  font-size: 13px;
  font-weight: 600;
  color: var(--color-text);
}
.req {
  font-weight: 400;
  color: var(--color-text-tertiary);
  font-size: 12px;
}
.form-field input {
  padding: 10px 14px;
  border: 1.5px solid var(--color-border);
  border-radius: var(--radius-sm);
  font-size: 15px;
  outline: none;
  transition: border-color 0.2s;
  font-family: var(--font-body);
  background: var(--color-surface);
}
.form-field input:focus {
  border-color: var(--color-accent);
  box-shadow: 0 0 0 3px rgba(0, 113, 227, 0.1);
}

.pill-group {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
}
.pill-group button {
  padding: 8px 16px;
  border-radius: 980px;
  border: 1.5px solid var(--color-border);
  background: var(--color-surface);
  cursor: pointer;
  font-size: 13px;
  font-weight: 500;
  color: var(--color-text-secondary);
  transition: all 0.2s;
  font-family: var(--font-body);
}
.pill-group button:hover {
  border-color: var(--color-accent);
  color: var(--color-accent);
}
.pill-group button.active {
  background: var(--color-accent);
  color: #fff;
  border-color: var(--color-accent);
}

.form-actions {
  display: flex;
  gap: 12px;
  justify-content: flex-end;
  padding-top: 8px;
}
</style>
