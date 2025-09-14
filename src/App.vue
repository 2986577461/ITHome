<script setup>
import { onMounted, ref } from "vue";
import { user_store } from "@/store/user.js";
import { getThis } from "@/request/axiosForUser.js";
import { chat_store } from "@/store/chatStore.js";
import { baseHost } from "@/request/axiosInit.js";
import { getUserList } from "@/request/axiosForAI.js";

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
      chatStore.connected = true;
      isWsReady.value = true; // 标记为就绪
      resolve();
    };

    chatStore.socket.onclose = () => {
      chatStore.connected = false;
      if (!isWsReady.value) {
        setTimeout(initWebsocket, 3000);
      }
    };
    // App.vue 的 onmessage 处理器
    chatStore.socket.onmessage = (event) => {
      try {
        const message = JSON.parse(event.data);

        if (Array.isArray(message)) {
          // 历史消息
          message.forEach((msg) => {
            const conversationPartnerId =
              msg.senderId === userStore.studentId
                ? msg.receiverId
                : msg.senderId;

            chatStore.addMessage(conversationPartnerId, {
              id: msg.id, // 添加消息ID [cite: 25, 26, 27]
              senderId: msg.senderId, // 统一使用 senderId
              receiverId: msg.receiverId, // 统一使用 receiverId
              content: msg.content,
              timestamp: msg.createDateTime,
            });
          });
        } else {
          // 单条实时消息
          const conversationPartnerId =
            message.senderId === userStore.studentId
              ? message.receiverId
              : message.senderId;

          chatStore.addMessage(conversationPartnerId, {
            id: message.id, // 添加消息ID [cite: 28, 29]
            senderId: message.senderId, // 统一使用 senderId
            receiverId: message.receiverId, // 统一使用 receiverId
            content: message.content,
            timestamp: message.createDateTime,
          });
        }
      } catch (e) {
        console.error("App.vue 消息解析错误:", e);
      }
    };
  });
};

onMounted(async () => {
  try {
    if (localStorage.getItem("token") !== null) {
      const resp = await getThis();
      if (resp.code !== "200") return;
      const student = resp.data;
      userStore.loginsuccess(
        student.id,
        student.studentId,
        student.name,
        student.position,
        student.academy
      );
      await initWebsocket(); // 等待连接建立
      chatStore.onlineUsers = (await getUserList()).data;
    }
  } catch (error) {
    console.error("初始化失败:", error);
  }
});

// 暴露连接状态给其他组件
defineExpose({
  isWsReady,
});
</script>

<template>
  <router-view></router-view>
</template>

<style scoped></style>
