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
      style="border-radius: 20px; width: 800px">
    <div class="main">

      <div class="head">学习资料上传</div>
      <div class="contentForm">
        <el-form label-width="auto">
          <div class="headItem">
            <b>标题：</b>
            <el-input class="el_input" v-model="resource.head" required/>
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
        >提交
        </el-button
        >
      </div>
    </div>
  </el-dialog>
</template>

<script lang="ts" setup>
import {reactive, ref} from "vue";
import {storeToRefs} from "pinia";
import {ElLoading, ElMessage} from "element-plus";
import type {UploadProps} from "element-plus";
import {upload_sotre} from "@/store/upload";
import {ElUpload, ElButton} from "element-plus";
import {uploadResource} from '@/request/axiosForResources'
const uploadStore = upload_sotre();
const {loadFile_visible} = storeToRefs(uploadStore);


const coverFileList = ref([]);
const fileList = ref([]);
const resource = reactive({
  head: "",
  introduce: "",
});

const submitUpload = async () => {
  const load = ElLoading.service({
    fullscreen: true,
    text: "上传中",
    background: "rgba(122, 122, 122, 0.8)",
  });
  if (coverFileList.value.length === 0 || fileList.value.length === 0) {
    ElMessage.error("请上传封面和文件");
    load.close()
    return;
  }
  if (resource.head === "" || resource.introduce === "") {
    ElMessage.error("请填写标题和简介");
    load.close()
    return;
  }


  let formData = new FormData();
  formData.append("file", fileList.value[0].raw);
  formData.append("cover", coverFileList.value[0].raw);
  formData.append("head",resource.head);
  formData.append("introduce",resource.introduce)
  await uploadResource(formData);

  ElMessage.success("上传成功");

  setTimeout(() => {
    load.close();
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
