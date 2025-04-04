<template>
  <el-dialog
    v-model="visibleStore.resetPasswordVisible"
    width="550px"
    :show-close="false"
    style="
      padding: 0;
      background: linear-gradient(135deg, #90caf9 0%, #1976d2 100%);
      box-shadow: 0 4px 10px rgba(0, 0, 0, 0.3);
      height: 440px;
      border-radius: 18px;
    "
  >
    <form @submit.prevent="handleSubmit">
      <div class="headLine">重置密码</div>

      <div class="input-group">
        <div>新密码：</div>
        <input v-model="oldPassword" type="password" required />
      </div>
      <div class="input-group">
        <div>确认密码：</div>
        <input v-model="newPassword" type="password" required />
      </div>

      <div class="submit-div">
        <el-button type="primary" native-type="submit" class="submit-btn">
          确认修改
        </el-button>
      </div>
    </form>
  </el-dialog>
</template>

<script setup>
import { ref } from "vue";
import { visible_store } from "@/store/visible";
import { ElMessage } from "element-plus";
import { logoutAxios, updatePassword } from "@/axios/axios.js";
import { user_store } from "@/store/user";
const visibleStore = visible_store();
const userStore = user_store();
const oldPassword = ref("");
const newPassword = ref("");
const handleSubmit = async () => {
  if (oldPassword.value !== newPassword.value) {
    ElMessage.error(" 两次密码必须相同。");
    return;
  }

  const resp = await updatePassword(userStore.studentID, newPassword.value);
  if (resp == false) {
    ElMessage.error("密码修改失败。");
    return;
  }

  ElMessage.success("密码修改成功。");
  setTimeout(async () => {
    await logoutAxios();
    location.reload();
  }, 1000);
  visibleStore.resetPasswordVisible = false;
};
</script>

<style scoped>
.headLine {
  text-align: center;
  color: white;
  font-size: 50px;
  font-family: "Microsoft JhengHei", sans-serif;
  margin-top: 3%;
  font-weight: 500;
  letter-spacing: 2px;
}

.input-group {
  width: 70%;
  margin: 20px auto 30px;
}

.input-group div {
  font-family: "Microsoft JhengHei";
  color: #e2e2e3;
  font-size: 20px;
  pointer-events: none;
  transition: all 0.3s ease;
}

.input-group input {
  width: 95%;
  height: 40px;
  padding: 0 10px;
  font-size: 16px;
  border: none;
  border-radius: 5px;
  outline: none;
  transition: all 0.3s ease;
  background-color: rgba(255, 255, 255, 0.3);
  box-shadow: 0 2px 4px rgba(0, 0, 0, 0.1), inset 0 1px 2px rgba(0, 0, 0, 0.1);
}

.input-group input:focus {
  transform: scale(1.02);
  box-shadow: 0 4px 8px rgba(0, 0, 0, 0.2), inset 0 1px 2px rgba(0, 0, 0, 0.1);
}

.submit-div {
  padding-top: 15px;
  font-size: 18px;
  text-align: center;
}

.submit-btn {
  width: 140px;
  height: 44px;
  font-size: 16px;
  letter-spacing: 2px;
  background: rgba(255, 255, 255, 0.2) !important;
  backdrop-filter: blur(10px);
  border: 1px solid rgba(255, 255, 255, 0.3) !important;
  border-radius: 22px !important;
  color: white !important;
  transition: all 0.3s ease;
}

.submit-btn:hover {
  background: rgba(255, 255, 255, 0.3) !important;
  transform: translateY(-2px);
  box-shadow: 0 5px 15px rgba(255, 255, 255, 0.2);
}
</style>
