<script setup>
import {visible_store} from "@/store/visible.js";
import {user_store} from "@/store/user.js";
import {ref} from "vue";
import {ElLoading} from "element-plus";
import {ElMessage} from "element-plus";
import {login} from "@/request/axiosForUser.js";
import router from "@/router/router.js";

const visibleStore = visible_store();
const userStore = user_store();
const studentId = ref("");
const password = ref("");

const handleSubmit = async (e) => {
  e.preventDefault();

  const loadingInstance = ElLoading.service({
    text: "登陆中...",
    background: "rgba(0, 0, 0, 0.5)",
  });

  setTimeout(async () => {
    loadingInstance.close(); //关闭加载动画

    const loginMessage = {
      studentId: studentId.value,
      password: password.value,
    };
    const resp = await login(loginMessage);
    if (resp.code === 200) {
      ElMessage.success("登陆成功。");

      // 1. 存储Token到本地存储
      localStorage.setItem("token", resp.data.token);

      setTimeout(() => {
        visibleStore.offvisible();
        userStore.loginsuccess(
            resp.data.id,
            resp.data.name,
            resp.data.position,
            resp.data.academy
        );
        router.push("/")
      }, 600);
    } else {
      ElMessage.error("账号或密码错误，请重试。");
    }
  }, 700);
};
</script>

<template>
  <el-dialog
      width="550px"
      :show-close="false"
      style="
      padding: 0;
      background: linear-gradient(135deg, #90caf9 0%, #1976d2 100%);
      box-shadow: 0 4px 10px rgba(0, 0, 0, 0.3);
      height: 440px;
      border-radius: 18px;
    "
      v-model="visibleStore.visible"
  >
    <form @submit="handleSubmit">
      <div class="headLine">IT之家协会</div>

      <div class="input-group">
        <div>请输入账号：</div>
        <input v-model="studentId" required/>
      </div>
      <div class="input-group">
        <div>请输入密码：</div>
        <input v-model="password" type="password" required/>
      </div>

      <div class="submit-div">
        <el-button type="success" native-type="submit" class="glass-btn"
        >登录
        </el-button>
      </div>
    </form>
  </el-dialog>
</template>

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

/* 玻璃按钮效果 */
.glass-btn {
  width: 90px;
  height: 40px;
  background: rgba(255, 255, 255, 0.2) !important;
  backdrop-filter: blur(10px);
  border: 1px solid rgba(255, 255, 255, 0.3) !important;
  border-radius: 5px !important;
  color: white !important;
  transition: all 0.3s ease;
}

.glass-btn:hover {
  background: rgba(255, 255, 255, 0.3) !important;
  transform: translateY(-2px);
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
  border-color: #007bff;
  transform: scale(1.02);
  box-shadow: 0 4px 8px rgba(0, 0, 0, 0.2), inset 0 1px 2px rgba(0, 0, 0, 0.1);
}

.submit-div {
  padding-top: 15px;
  font-size: 18px;
  text-align: center;
}
</style>
