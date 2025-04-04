<script setup>
import { onMounted } from "vue";
import { user_store } from "@/store/user.js";
import { autoLogin, selectByID } from "@/axios/axios.js";
const userStore = user_store();

onMounted(async () => {
  //发送axios请求，登陆成功则返回true
  const apply = await autoLogin();
  const id = apply.data;
  if (id != null) {
    const student = await selectByID(id);
    userStore.loginsuccess(
      student.studentId,
      student.name,
      student.position,
      student.academy
    );
  }
});
</script>

<template><router-view></router-view></template>

<style scoped></style>
