<template>
  <div
    class="modal-overlay"
    v-if="uploadStore.loadFileVisible"
    @click.self="uploadStore.loadFileVisible = false"
  >
    <div class="modal-card wide">
      <button class="modal-close" @click="uploadStore.loadFileVisible = false">
        <svg width="18" height="18" viewBox="0 0 18 18" fill="none">
          <path
            d="M4 4l10 10M14 4L4 14"
            stroke="currentColor"
            stroke-width="1.5"
            stroke-linecap="round"
          />
        </svg>
      </button>
      <h2 class="dialog-title">学习资料上传</h2>
      <form @submit.prevent="submitUpload" class="upload-form">
        <div class="form-row">
          <div class="form-field">
            <label>标题</label>
            <input
              v-model="resource.head"
              type="text"
              required
              placeholder="请输入标题"
            />
          </div>
        </div>

        <div class="upload-row">
          <div class="upload-box">
            <span class="upload-label">封面图片</span>
            <div class="upload-zone" @click="coverInput.click()">
              <img
                v-if="coverPreview"
                :src="coverPreview"
                class="upload-preview"
              />
              <div v-else class="upload-placeholder">
                <svg width="32" height="32" viewBox="0 0 24 24" fill="none">
                  <path
                    d="M12 4v16M4 12h16"
                    stroke="currentColor"
                    stroke-width="1.5"
                    stroke-linecap="round"
                  />
                </svg>
                <span>添加封面</span>
              </div>
            </div>
            <input
              ref="coverInput"
              type="file"
              accept=".jpg,.jpeg,.png"
              @change="onCoverChange"
              hidden
            />
          </div>
          <div class="upload-box">
            <span class="upload-label">文件</span>
            <div class="upload-zone" @click="fileInput.click()">
              <div v-if="fileName" class="file-name">{{ fileName }}</div>
              <div v-else class="upload-placeholder">
                <svg width="32" height="32" viewBox="0 0 24 24" fill="none">
                  <path
                    d="M14 2H6a2 2 0 00-2 2v16a2 2 0 002 2h12a2 2 0 002-2V8l-6-6z"
                    stroke="currentColor"
                    stroke-width="1.5"
                    stroke-linecap="round"
                    stroke-linejoin="round"
                  />
                  <path
                    d="M14 2v6h6"
                    stroke="currentColor"
                    stroke-width="1.5"
                    stroke-linecap="round"
                    stroke-linejoin="round"
                  />
                </svg>
                <span>选择文件</span>
              </div>
            </div>
            <input ref="fileInput" type="file" @change="onFileChange" hidden />
          </div>
        </div>

        <div class="form-field">
          <label>简介</label>
          <textarea
            v-model="resource.introduce"
            class="textarea"
            required
            placeholder="请输入资源简介"
          ></textarea>
        </div>

        <button
          type="submit"
          class="btn-primary submit-btn"
          :disabled="uploading"
        >
          {{ uploading ? "上传中..." : "提交" }}
        </button>
      </form>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive } from "vue";
import { useUploadStore } from "@/stores/upload";
import { uploadResource } from "@/request/axiosForResources";
import { ElMessage } from "element-plus";

const uploadStore = useUploadStore();
const coverInput = ref(null);
const fileInput = ref(null);
const coverPreview = ref("");
const fileName = ref("");
const coverFile = ref(null);
const uploadFile = ref(null);
const uploading = ref(false);

const resource = reactive({ head: "", introduce: "" });

function onCoverChange(e) {
  const file = e.target.files[0];
  if (!file) return;
  coverFile.value = file;
  coverPreview.value = URL.createObjectURL(file);
}
function onFileChange(e) {
  const file = e.target.files[0];
  if (!file) return;
  uploadFile.value = file;
  fileName.value = file.name;
}

async function submitUpload() {
  if (!coverFile.value || !uploadFile.value) {
    ElMessage.error("请上传封面和文件");
    return;
  }
  if (!resource.head || !resource.introduce) {
    ElMessage.error("请填写标题和简介");
    return;
  }

  uploading.value = true;
  const fd = new FormData();
  fd.append("file", uploadFile.value);
  fd.append("cover", coverFile.value);
  fd.append("head", resource.head);
  fd.append("introduce", resource.introduce);

  if ((await uploadResource(fd)).code === "200") {
    ElMessage.success("上传成功");
  }
  uploadStore.loadFileVisible = false;
  setTimeout(() => location.reload(), 700);

  uploading.value = false;
}
</script>

<style scoped>
.dialog-title {
  font-family: var(--font-heading);
  font-size: 24px;
  font-weight: 700;
  text-align: center;
  margin-bottom: 28px;
}
.upload-form {
  display: flex;
  flex-direction: column;
  gap: 20px;
}
.form-row {
  display: flex;
  gap: 20px;
}
.form-field {
  display: flex;
  flex-direction: column;
  gap: 6px;
  flex: 1;
}
.form-field label {
  font-size: 13px;
  font-weight: 600;
  color: var(--color-text);
}
.form-field input,
.form-field textarea {
  padding: 10px 14px;
  border: 1.5px solid var(--color-border);
  border-radius: var(--radius-sm);
  font-size: 14px;
  outline: none;
  transition: border-color 0.2s;
  font-family: var(--font-body);
}
.form-field input:focus,
.form-field textarea:focus {
  border-color: var(--color-accent);
  box-shadow: 0 0 0 3px rgba(0, 113, 227, 0.1);
}
.textarea {
  min-height: 120px;
  resize: vertical;
}
.upload-row {
  display: flex;
  gap: 20px;
}
.upload-box {
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: 6px;
}
.upload-label {
  font-size: 13px;
  font-weight: 600;
  color: var(--color-text);
}
.upload-zone {
  height: 160px;
  border: 2px dashed var(--color-border);
  border-radius: var(--radius-sm);
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  transition: all 0.2s;
  overflow: hidden;
}
.upload-zone:hover {
  border-color: var(--color-accent);
  background: rgba(0, 113, 227, 0.03);
}
.upload-placeholder {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 8px;
  color: var(--color-text-tertiary);
  font-size: 13px;
}
.upload-preview {
  width: 100%;
  height: 100%;
  object-fit: cover;
}
.file-name {
  font-size: 13px;
  color: var(--color-text);
  padding: 8px;
}
.submit-btn {
  align-self: center;
  padding: 12px 48px;
  margin-top: 8px;
}
.submit-btn:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}
</style>
