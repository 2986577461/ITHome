<style scoped>
.main {
  width: 640px;
  margin: 0 auto;
}

.head {
  font-weight: bold;
  text-align: center;
  font-size: 50px;
  font-family: "微软雅黑", cursive;
  color: #7ec050;
}

.contentForm {
  font-size: 23px;
}

.headItem {
  vertical-align: top;
  width: 240px;
  height: 110px;
  display: inline-block;
}
.el_input {
  font-size: 23px;
  height: 40px;
}
.fileItem {
  /* width: 300px; */
  display: inline-block;
}
.uploadDiv {
  vertical-align: top;
  /* margin: 0 20px; */
  display: inline-block;
  width: 190px;
  text-align: center;
}

.btItem {
  text-align: center;
  width: 300px;
  margin: 30px auto;
}
.introduce-input {
  border: 1px solid #dcdfe6;
  flex: 1;
  padding: 12px 15px;
  border-radius: 6px;
  font-size: 16px;
  transition: all 0.3s ease;
  width: 100%;
  height: 120px;
  min-height: 120px;
  max-height: 270px;
  box-sizing: border-box;
  resize: vertical;
  font-family: inherit;
  line-height: 1.5;
}
</style>
<template>
  <el-dialog
    v-model="loadFile_visible"
    :show-close="false"
    top="8vh"
    style="border-radius: 20px; width: 800px"
  >
    <div class="main">
      <div class="head">学习资料上传</div>
      <div class="contentForm">
        <el-form label-width="auto">
          <div class="headItem">
            <b>标题：</b>
            <el-input class="el_input" v-model="resource.head" required />
          </div>

          <div class="fileItem">
            <div class="uploadDiv">
              <span>封面</span>
              <el-upload
                ref="coverUpload"
                accept=".jpg,.jpeg,.png"
                v-model:file-list="coverFileList"
                :on-preview="handlePreview"
                :on-remove="handleRemove"
                :on-exceed="handleExceed"
                :auto-upload="false"
                :limit="1"
              >
                <template #trigger>
                  <el-button type="primary">上传</el-button>
                </template>
              </el-upload>
            </div>
            <div class="uploadDiv">
              <div>文件</div>
              <el-upload
                ref="fileupload"
                v-model:file-list="fileList"
                :on-preview="handlePreview"
                :on-remove="handleRemove"
                :limit="2"
                :on-exceed="handleExceed"
                :auto-upload="false"
              >
                <template #trigger>
                  <el-button type="primary">上传</el-button>
                </template>
              </el-upload>
            </div>
          </div>

          <div>
            <div>简介</div>
            <textarea
              v-model="resource.introduce"
              class="introduce-input"
              style="width: 600px; font-size: 20px; height: 400px"
              required
            />
          </div>
        </el-form>
      </div>
      <div class="btItem">
        <el-button
          type="success"
          @click="submitUpload"
          style="width: 100px; height: 45px; font-size: 20px"
          >提交</el-button
        >
      </div>
    </div>
  </el-dialog>
</template>

<script lang="ts" setup>
import { reactive, ref } from "vue";

import { storeToRefs } from "pinia";
import { ElMessage } from "element-plus";
import type { UploadProps } from "element-plus";
import { upload_sotre } from "@/store/upload";
import { user_store } from "@/store/user";
import { uploadFile, uploadResource } from "@/axios/file";
import { ElUpload, ElButton } from "element-plus";
const uploadStore = upload_sotre();
const userStore = user_store();
const { loadFile_visible } = storeToRefs(uploadStore);

const coverFileList = ref([]);
const fileList = ref([]);
const resource = reactive({
  head: "",
  introduce: "",
  fileUrl: "",
  coverUrl: "",
  fileName: "",
});

const submitUpload = async () => {
  if (coverFileList.value.length === 0 || fileList.value.length === 0) {
    ElMessage.error("请上传封面和文件");
    return;
  }
  if (resource.head === "" || resource.introduce === "") {
    ElMessage.error("请填写标题和简介");
    return;
  }

  const formData = new FormData();

  formData.append("file", fileList.value[0].raw);
  const file = await uploadFile(formData);
  formData.append("file", coverFileList.value[0].raw);
  const cover = await uploadFile(formData);

  resource.fileUrl = file.fileUrl;
  resource.coverUrl = cover.fileUrl;
  resource.fileName = file.fileName;
  await uploadResource(resource);

  ElMessage.success("上传成功");

  setTimeout(() => {
    location.reload();
  }, 700);
};

const handleRemove: UploadProps["onRemove"] = (file, uploadFiles) => {
  console.log(file, uploadFiles);
};

const handlePreview: UploadProps["onPreview"] = (uploadFile) => {
  console.log(uploadFile);
};

const handleExceed: UploadProps["onExceed"] = () => {
  ElMessage.warning(`最多选择1份文件`);
};
</script>
