<template>
  <div
    class="modal-overlay"
    v-if="visibleStore.visible"
    @click.self="visibleStore.offVisible()"
  >
    <div class="modal-card">
      <button class="modal-close" @click="visibleStore.offVisible()">
        <svg width="18" height="18" viewBox="0 0 18 18" fill="none">
          <path
            d="M4 4l10 10M14 4L4 14"
            stroke="currentColor"
            stroke-width="1.5"
            stroke-linecap="round"
          />
        </svg>
      </button>
      <h2 class="dialog-title">IT之家协会</h2>
      <p class="dialog-subtitle">登录账户</p>
      <form @submit.prevent="handleSubmit" class="dialog-form">
        <div class="form-field">
          <label>学号</label>
          <input
            v-model="studentId"
            type="text"
            placeholder="请输入学号"
            required
          />
        </div>
        <div class="form-field">
          <label>密码</label>
          <input
            v-model="password"
            type="password"
            placeholder="请输入密码"
            required
          />
        </div>
        <button
          type="submit"
          class="btn-primary dialog-submit"
          :disabled="loading"
        >
          {{ loading ? "登录中..." : "登录" }}
        </button>
      </form>
    </div>
  </div>
</template>

<script setup>
import { ref } from "vue";
import { useVisibleStore } from "@/stores/visible";
import { useUserStore } from "@/stores/user";
import { login } from "@/request/axiosForUser.js";
import { ElMessage } from "element-plus";

const visibleStore = useVisibleStore();
const userStore = useUserStore();
const studentId = ref("");
const password = ref("");
const loading = ref(false);

const handleSubmit = async () => {
  loading.value = true;
  try {
    const resp = await login({
      studentId: studentId.value,
      password: password.value,
    });
    if (resp.code === "200") {
      ElMessage.success("登录成功");
      localStorage.setItem("authorization", resp.data.token);
      visibleStore.offVisible();
      userStore.setUser(resp.data);
      setTimeout(function () {
        window.location.reload();
      }, 800);
    } else {
      ElMessage.error("账号或密码错误");
    }
  } catch {
    ElMessage.error("登录失败");
  } finally {
    loading.value = false;
  }
};
</script>

<style scoped>
.dialog-title {
  font-family: var(--font-heading);
  font-size: 28px;
  font-weight: 700;
  text-align: center;
  margin-bottom: 4px;
}
.dialog-subtitle {
  text-align: center;
  color: var(--color-text-secondary);
  font-size: 14px;
  margin-bottom: 28px;
}
.dialog-form {
  display: flex;
  flex-direction: column;
  gap: 20px;
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
.form-field input {
  padding: 12px 16px;
  border: 1.5px solid var(--color-border);
  border-radius: var(--radius-sm);
  font-size: 15px;
  outline: none;
  transition: border-color 0.2s;
  font-family: var(--font-body);
}
.form-field input:focus {
  border-color: var(--color-accent);
  box-shadow: 0 0 0 3px rgba(0, 113, 227, 0.1);
}
.dialog-submit {
  width: 100%;
  justify-content: center;
  padding: 14px;
  margin-top: 4px;
}
.dialog-submit:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}
</style>
