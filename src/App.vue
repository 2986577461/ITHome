<script setup>
import { onMounted } from "vue";
import { user_store } from "@/store/user.js";
import { getCurrentUser } from "@/axios/axios.js";
const userStore = user_store();

onMounted(async () => {
  //发送axios请求，登陆成功则返回true
  const student = await getCurrentUser();
  console.log(student)
  userStore.loginsuccess(
    student.data.id,
    student.data.name,
    student.data.position,
    student.data.academy
  );
});
</script>

<template><router-view></router-view></template>

<style scoped></style>
