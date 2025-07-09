<template>
  <el-dialog
    v-model="governStore.addStudentVisible"
    width="80%"
    style="height: 85vh; overflow-y: hidden; margin-top: 8vh"
  >
    <template #header>
      <div class="dialog-header">新学员列表申请</div>
    </template>
    <el-empty
      description="没有新成员申请加入"
      v-if="newcomers.length === 0"
      style="margin: 8% 0"
    />
    <div class="newcomers-container">
      <div
        v-for="(student, index) in newcomers"
        :key="student.studentId"
        class="student-item"
      >
        <div class="info-grid">
          <div class="info-column">
            <div class="info-row">
              <span class="label">学号：</span>
              <span>{{ student.studentId }}</span>
            </div>

            <div class="info-row">
              <span class="label">姓名：</span>
              <span>{{ student.name }}</span>
            </div>
            <div class="info-row">
              <span class="label">性别：</span>
              <span>{{ student.sex }}</span>
            </div>
          </div>
          <div class="info-column">
            <div class="info-row">
              <span class="label">班级：</span>
              <span>{{ student.major + student.className }}</span>
            </div>
            <div class="info-row">
              <span class="label">学院：</span>
              <span>{{ student.academy }}</span>
            </div>
            <div class="info-row">
              <span class="label">简介：</span>
              <span
                @mouseenter="introduceVisible[index] = true"
                @mouseleave="introduceVisible[index] = false"
                >...</span
              >
            </div>
            <div class="student-profile">
              <div v-if="introduceVisible[index]" class="tooltip">
                <p>{{ student.introduce }}</p>
              </div>
            </div>
          </div>
          <div class="action-column">
            <div class="button-group">
              <el-button type="success" @click="handleApprove(student.id)"
                >通过</el-button
              >
              <el-button type="danger" @click="handleRefuse(student.id)"
                >拒绝</el-button
              >
            </div>
          </div>
        </div>
      </div>
    </div>
  </el-dialog>
</template>

<script setup>
import { ref } from "vue";
import { ElMessage } from "element-plus";
import { govern_store } from "@/store/govern.js";
import { storeToRefs } from "pinia";
import { refuse,agree } from "@/request/axiosForNewcomers.js";

const governStore = govern_store();
let { newcomers } = storeToRefs(governStore);

const introduceVisible = ref([false]);

const handleApprove = async (id) => {
  const resp = await agree(id);
  if (resp) {
    ElMessage.success("已通过申请");
    const student = governStore.newcomers.find((item) => item.id === id);
    student.position = "学员";
    student.articleCount = 0;
    student.resourceCount = 0;
    governStore.member.push(student);

    governStore.newcomers = governStore.newcomers.filter(
      (item) => item.id !== id
    );
  } else {
    ElMessage.error("操作失败");
  }
};

const handleRefuse = async (id) => {
  governStore.newcomers = governStore.newcomers.filter(
    (item) => item.id !== id
  );
  const resp = await refuse(id);
  if (resp) {
    ElMessage.warning("已拒绝申请");
  } else {
    ElMessage.error("操作失败");
  }
};
</script>

<style scoped>
.newcomers-container {
  max-height: 70vh;
  overflow-y: auto;
  padding: 10px 20px;
}

.student-item {
  padding: 15px;
  border: 1px solid #eee;
  border-radius: 8px;
  margin-bottom: 15px;
  background-color: #fff;
  box-shadow: 0 2px 4px rgba(0, 0, 0, 0.05);
}

.info-grid {
  display: flex;
  gap: 20px;
  align-items: center;
}

.info-column {
  flex: 1;
  min-width: 0;
}

.action-column {
  display: flex;
  align-items: center;
  padding-left: 20px;
  border-left: 1px solid #eee;
}

.info-row {
  margin-bottom: 8px;
  font-size: 14px;
  line-height: 1.4;
  display: flex;
  align-items: center;
}

.label {
  color: #666;
  width: 60px;
  text-align: right;
  margin-right: 8px;
  flex-shrink: 0;
}

.dialog-header {
  font-size: 28px;
  font-family: "Microsoft JhengHei";
  font-weight: 400;
  color: #2c2c2c;
  padding: 20px;
  background-color: #f5f7fa;
  border-bottom: 1px solid #e4e7ed;
  text-align: center;
}

.button-group {
  display: flex;
  gap: 15px;
}

/* 调整按钮样式 */
:deep(.el-button) {
  width: 100px;
  height: 40px;
  font-size: 16px;
  margin: 0;
  /* 移除默认边距 */
}

:deep(.el-dialog) {
  border-radius: 8px;
}

:deep(.el-dialog__header) {
  margin: 0;
  padding: 0;
}

:deep(.el-dialog__headerbtn) {
  top: 20px;
  right: 20px;
}

.student-profile {
  position: absolute;
  display: inline-block;
}

.tooltip {
  position: absolute;
  top: 100%; /* 位于触发元素下方 */
  left: 0;
  background-color: #fff;
  border: 1px solid #ccc;
  padding: 10px;
  border-radius: 4px;
  box-shadow: 0 2px 5px rgba(0, 0, 0, 0.2);
  z-index: 1000;
  width: 200px; /* 根据需要调整宽度 */
}
</style>
