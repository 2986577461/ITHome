<script lang="ts" setup>
import { user_store } from "@/store/user.js";
import { visible_store } from "@/store/visible.js";
import loginVue from "./login.vue";
import { useRouter } from "vue-router";

const router = useRouter();
const visible = visible_store();
const userStore = user_store();
</script>

<template>
  <div id="main">
    <img
      src="/ITLogo.png"
      alt="无法加载"
      class="logo"
      @click="router.push('/home')"
    />

    <nav class="nav-links">
      <router-link to="/tech-study" class="nav-item">技术教学</router-link>
      <router-link to="/leraningResource" class="nav-item"
        >学习资料</router-link
      >

      <router-link
        v-if="userStore.position == '会长' || userStore.position == '副会长'"
        to="/colonyGovern"
        class="nav-item"
        >学员管理</router-link
      >
      <router-link to="/harvest" class="nav-item">协会信息</router-link>
      <router-link to="/ai-dialog" class="nav-item">AI对话</router-link>
      <router-link to="/join-us" class="nav-item">加入我们</router-link>
    </nav>

    <div class="user-section">
      <div class="welcome-text">
        <span v-if="userStore.condition"> 欢迎您，{{ userStore.name }}</span>
        <el-button
          class="logout-btn"
          type="default"
          @click="visible.loginOrLogoutButton()"
        >
          {{ visible.getText }}
        </el-button>
      </div>
    </div>

    <loginVue></loginVue>
  </div>
</template>

<style scoped>
#main {
  width: 100%;
  height: 8vh;
  background-color: white;
  display: flex;
  align-items: center;
  padding: 0 2%;
  box-sizing: border-box;
  border-bottom: 1px solid #e4e7ed;
}

.logo {
  margin-left: 4%;
  height: 45px;
  cursor: pointer;
  margin-right: 20px;
}

.nav-links {
  display: flex;
  flex: 1;
  gap: 25px;
  margin-left: 40px;
}

.nav-item {
  white-space: nowrap;
  font-family: "Microsoft JhengHei";
  text-decoration: none;
  color: #333;
  font-size: 16px;
  font-weight: 400;
  padding: 8px 12px;
  border-radius: 4px;
  transition: all 0.3s ease;
}

.nav-item:hover {
  color: #3f7bc4;
}

.user-section {
  display: flex;
  align-items: center;
  gap: 20px;
  margin-left: auto;
}

.welcome-text {
  display: flex;
  align-items: center;
  gap: 10px;
  font-size: 14px;
  color: #333;
}

.logout-btn {
  font-size: 14px;
  color: #666;
}

.logout-btn:hover {
  color: #3f7bc4;
}

.login-btn {
  height: 36px;
  padding: 0 20px;
  font-size: 14px;
  border-radius: 4px;
}

@media (max-width: 1200px) {
  .nav-links {
    gap: 15px;
  }

  .nav-item {
    font-size: 15px;
    padding: 6px 10px;
  }
}

@media (max-width: 768px) {
  #main {
    padding: 0 10px;
  }

  .logo {
    height: 40px;
  }

  .nav-links {
    gap: 8px;
    margin-left: 20px;
  }

  .nav-item {
    font-size: 14px;
    padding: 5px 8px;
  }

  .avatar {
    width: 35px;
    height: 35px;
  }
}
</style>
