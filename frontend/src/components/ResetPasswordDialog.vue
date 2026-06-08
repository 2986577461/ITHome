<template>
  <div class="modal-overlay" v-if="visibleStore.resetPasswordVisible" @click.self="visibleStore.resetPasswordVisible = false">
    <div class="modal-card">
      <button class="modal-close" @click="visibleStore.resetPasswordVisible = false">
        <svg width="18" height="18" viewBox="0 0 18 18" fill="none"><path d="M4 4l10 10M14 4L4 14" stroke="currentColor" stroke-width="1.5" stroke-linecap="round"/></svg>
      </button>
      <h2 class="dialog-title">重置密码</h2>
      <form @submit.prevent="handleSubmit" class="dialog-form">
        <div class="form-field">
          <label>新密码</label>
          <input v-model="oldPassword" type="password" placeholder="输入新密码" required />
        </div>
        <div class="form-field">
          <label>确认密码</label>
          <input v-model="newPassword" type="password" placeholder="再次输入新密码" required />
        </div>
        <button type="submit" class="btn-primary dialog-submit">确认修改</button>
      </form>
    </div>
  </div>
</template>

<script setup>
import { ref } from "vue";
import { useVisibleStore } from "@/stores/visible";
import { updatePassword } from "@/request/axiosForUser.js";
import { ElMessage } from "element-plus";

const visibleStore = useVisibleStore();
const oldPassword = ref("");
const newPassword = ref("");

const handleSubmit = async () => {
  if (oldPassword.value !== newPassword.value) {
    ElMessage.error("两次密码必须相同");
    return;
  }
  const resp = await updatePassword(newPassword.value);
  if (resp === false) {
    ElMessage.error("密码修改失败");
    return;
  }
  ElMessage.success("密码修改成功");
  visibleStore.resetPasswordVisible = false;
};
</script>

<style scoped>
.dialog-title {
  font-family: var(--font-heading);
  font-size: 28px; font-weight: 700;
  text-align: center; margin-bottom: 28px;
}
.dialog-form { display: flex; flex-direction: column; gap: 20px; }
.form-field { display: flex; flex-direction: column; gap: 6px; }
.form-field label { font-size: 13px; font-weight: 600; color: var(--color-text); }
.form-field input {
  padding: 12px 16px;
  border: 1.5px solid var(--color-border);
  border-radius: var(--radius-sm);
  font-size: 15px; outline: none;
  transition: border-color 0.2s;
  font-family: var(--font-body);
}
.form-field input:focus {
  border-color: var(--color-accent);
  box-shadow: 0 0 0 3px rgba(0, 113, 227, 0.1);
}
.dialog-submit { width: 100%; justify-content: center; padding: 14px; margin-top: 4px; }
</style>
