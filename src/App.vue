<script setup>
import {onMounted, ref} from "vue";
import {user_store} from "@/store/user.js";
import {getThis} from "@/request/axiosForUser.js";
import {chat_store} from "@/store/chatStore.js";
import {baseHost} from "@/request/axiosInit.js";

const userStore = user_store();
const chatStore = chat_store();
const isWsReady = ref(false); // 新增状态标记


const initWebsocket = async () => {
  const encodedUserName = encodeURIComponent(userStore.name);
  const wsUrl = `ws://${baseHost}/ws/${userStore.studentId}/${encodedUserName}`;

  return new Promise((resolve) => {
    if (chatStore.socket) {
      chatStore.socket.close();
    }

    chatStore.socket = new WebSocket(wsUrl);

    chatStore.socket.onopen = () => {
      console.log("WebSocket连接已建立");
      chatStore.connected = true;
      isWsReady.value = true; // 标记为就绪
      resolve();
    };

    chatStore.socket.onclose = () => {
      chatStore.connected = false;
      // if (!isWsReady.value) {
      //   setTimeout(initWebsocket, 3000);
      // }
    };
  });
};

onMounted(async () => {
  try {
    const resp = await getThis();
    if (resp.code !== 200) return;
    const student = resp.data;
    userStore.loginsuccess(
        student.id,
        student.studentId,
        student.name,
        student.position,
        student.academy
    );

    // await initWebsocket(); // 等待连接建立
  } catch (error) {
    console.error("初始化失败:", error);
  }
});

// 暴露连接状态给其他组件
defineExpose({
  isWsReady
});
</script>

<template>
  <router-view></router-view>
</template>

<style scoped></style>
