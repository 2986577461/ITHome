<template>
  <el-dialog
    v-model="governStore.updateStudentVisible"
    width="80%"
    style="margin-top: 10vh"
  >
    <template #header>
      <div class="dialog-header">学员信息修改</div>
    </template>

    <div class="newcomers-container">
      <el-form
        :model="governStore.updateStudent"
        label-width="80px"
        @submit.native.prevent="submitUpdate"
      >
        <div class="form-row">
          <el-form-item label="学号">
            <el-input v-model="governStore.updateStudent.studentId" />
          </el-form-item>
          <el-form-item label="姓名">
            <el-input v-model="governStore.updateStudent.name" />
          </el-form-item>
        </div>

        <div class="form-row">
          <el-form-item label="性别">
            <el-select v-model="governStore.updateStudent.sex">
              <el-option label="男" value="男" />
              <el-option label="女" value="女" />
            </el-select>
          </el-form-item>
          <el-form-item label="专业">
            <el-input v-model="governStore.updateStudent.major" />
          </el-form-item>
        </div>

        <div class="form-row">
          <el-form-item label="班级">
            <el-input v-model="governStore.updateStudent.claxx" />
          </el-form-item>
          <el-form-item label="学院">
            <el-input v-model="governStore.updateStudent.academy" />
          </el-form-item>
        </div>

        <div class="form-row">
          <el-form-item label="职位">
            <el-select v-model="governStore.updateStudent.position">
              <el-option label="会长" value="会长" />
              <el-option label="副会长" value="副会长" />
              <el-option label="学员" value="学员" />
            </el-select>
          </el-form-item>
          <el-form-item label="密码">
            <el-input v-model="governStore.updateStudent.password" required />
          </el-form-item>
        </div>

        <div class="form-footer">
          <el-button @click="governStore.updateStudentVisible = false"
            >取消</el-button
          >
          <el-button type="warning" native-type="submit">保存</el-button>
        </div>
      </el-form>
    </div>
  </el-dialog>
</template>

<script setup>
import { storeToRefs } from "pinia";
import { govern_store } from "@/store/govern.js";
import { updateStudentForAxios } from "@/axios/axios.js";
import { ElMessage } from "element-plus";
const governStore = govern_store();
const { updateStudent } = storeToRefs(governStore);
const submitUpdate = async () => {
  const resp = await updateStudentForAxios(
    governStore.oldStudentId,
    updateStudent.value
  );
  console.log(resp);
  ElMessage.success("更新成功");
  governStore.updateStudentVisible = false;
};
</script>

<style scoped>
.newcomers-container {
  max-height: 70vh;
  overflow-y: auto;
  padding: 10px 20px;
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

.form-row {
  height: 40px;
  display: flex;
  justify-content: space-between;
  gap: 20px;
  margin-bottom: 20px;
}

.form-row .el-form-item {
  flex: 1;
  margin-bottom: 0;
}

.form-footer {
  text-align: center;
  margin-top: 30px;
  display: flex;
  gap: 20px;
  justify-content: center;
}

:deep(.el-input__wrapper) {
  width: 100%;
}

:deep(.el-select) {
  width: 100%;
}

.empty-item :deep(.el-form-item__label) {
  visibility: hidden;
}
</style>
