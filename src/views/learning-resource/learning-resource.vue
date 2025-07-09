<template>
  <Header></Header>

  <div class="content-container">
    <!-- 左侧导航栏 -->
    <el-scrollbar class="aside">
      <div class="aside-header">
        <h2>学习资料</h2>
      </div>
      <el-anchor
        :container="containerRef"
        direction="vertical"
        :offset="30"
        type="default"
      >
        <el-anchor-link
          v-for="(item, index) in resources"
          :key="index"
          :href="`#part${index}`"
          @click="handleClick"
        >
          <div class="nav-item">
            <div class="nav-index">{{ index + 1 }}</div>
            <span class="asideText">{{ item.head }}</span>
          </div>
        </el-anchor-link>
      </el-anchor>
    </el-scrollbar>

    <!-- 主内容区域         这个ref绑定了element-plus的anchor组件-->
    <div class="main" ref="containerRef">
      <div class="main-content">
        <!-- 资源列表 -->
        <div
          v-for="(item, index) in resources"
          :key="item.releaseTime"
          :id="`part${index}`"
          class="resource-item"
        >
          <div class="resource-content">
            <div class="resource-header">
              <h2>{{ item.head }}</h2>
            </div>
            <div class="resource-body">
              <div class="resource-image">
                <img
                  :src="item.coverUrl"
                  :alt="item.head"
                  class="fixed-image"
                />
              </div>
              <div class="resource-right">
                <div class="resource-description">
                  <p class="content-text">{{ item.introduce }}</p>
                </div>

                <button
                  class="download-button"
                  @click="download(item.fileUrl, item.fileName)"
                >
                  <el-icon :size="24">
                    <Download />
                  </el-icon>
                  <span>下载资料</span>
                </button>
              </div>
            </div>
          </div>
        </div>
      </div>
      <Foot></Foot>
    </div>
    <div
      class="uploadBt"
      @click="uploadStore.loadFile_visible = true"
      v-if="condition"
    >
      上传
    </div>
    <uploadVue></uploadVue>
  </div>
</template>

<script lang="ts" setup>
import uploadVue from "@/components/uploadResources.vue";
import Header from "@/components/Header.vue";
import { reactive, onMounted, ref } from "vue";
import Foot from "@/components/Foot.vue";
import { Download } from "@element-plus/icons-vue";
import { user_store } from "@/store/user";
import { storeToRefs } from "pinia";
import { upload_sotre } from "@/store/upload";
import {getDownloadUrl,getAll} from "@/request/axiosForResources"
const uploadStore = upload_sotre();
const userStore = user_store();
let { condition } = storeToRefs(userStore);
const containerRef = ref<HTMLElement | null>(null);

const resources = reactive([]);

const handleClick = (e) => {
  e.preventDefault();
};


const download = async (url, name) => {
  const urlObj = new URL(url);
  const pathname = urlObj.pathname; // 获取路径部分：/a5b8f572-f785-408d-915f-e22983c9c80b.jpeg
  const objectName = pathname.substring(pathname.lastIndexOf("/") + 1); // 获取最后一个斜杠后的部分
  const resp = await getDownloadUrl(objectName, name);

  const link = document.createElement("a");
  link.href = resp.data;
  link.download = name; // 设置下载文件名
  document.body.appendChild(link);
  link.click();
  document.body.removeChild(link);
};

onMounted(async () => {
  const res = await getAll(); //加载文章文字
  resources.splice(0, 0, ...res.data); //返回文字
  console.log(res.data)
});
</script>

<style scoped>
.content-container {
  display: flex;
  width: 100%;
  overflow: hidden;
}

/* 侧边栏样式 */
.aside {
  width: 20%;
  background: white;
  left: 0;
  bottom: 0;
  box-shadow: 2px 0 8px rgba(0, 0, 0, 0.05);
  height: 92vh;
}

.aside-header {
  text-align: center;
  padding: 20px 24px;
  border-bottom: 1px solid #e6e9ed;
}

.aside-header h2 {
  color: #2c3e50;
  margin: 0;
  font-weight: 600;
}

.nav-item {
  display: flex;
  align-items: center;
  padding: 16px 24px;
  transition: all 0.3s ease;
  position: relative;
}

.nav-item:hover {
  background-color: #eef5fe;
  padding-left: 28px;
}

.nav-index {
  width: 24px;
  height: 24px;
  background: #e6e9ed;
  border-radius: 12px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 14px;
  color: #606266;
  margin-right: 12px;
  transition: all 0.3s ease;
}

.nav-item:hover .nav-index {
  background: #409eff;
  color: white;
}

.asideText {
  color: #606266;
  font-size: 15px;
  font-weight: 500;
  transition: color 0.3s ease;
  /* 文本过长时显示省略号 */
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  max-width: 180px;
}

.nav-item:hover .asideText {
  color: #409eff;
}

/* 激活状态的样式 */
:deep(.el-anchor-link.is-active) {
  .nav-item {
    background-color: #ecf5ff;
  }

  .nav-index {
    background: #409eff;
    color: white;
  }

  .asideText {
    color: #409eff;
    font-weight: 600;
  }
}

:deep(.el-anchor-link.is-active) .nav-item::before {
  content: "";
  position: absolute;
  left: 0;
  top: 0;
  bottom: 0;
  width: 3px;
  background-color: #409eff;
}

/* 滚动条样式 */
.aside :deep(.el-scrollbar__wrap) {
  overflow-x: hidden;
}

.aside :deep(.el-scrollbar__thumb) {
  background-color: #c0c4cc;
  opacity: 0.3;
}

.aside :deep(.el-scrollbar__thumb:hover) {
  opacity: 0.5;
}

/* 主容器样式 */
.main {
  flex: 1;
  height: 92vh;
  background-color: #f2f6fb;

  overflow-x: hidden;
}

/* 资源卡片样式 */
.resource-item {
  background: white;
  border-radius: 12px;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.1);
  margin: 30px auto;
  width: 90%;
  transition: transform 0.3s ease;
}

.resource-content {
  padding: 3%;
}

.resource-header h2 {
  width: 40%;
  color: #2c3e50;
  font-size: 28px;
  margin-bottom: 0; /* 减小标题下方间距 */
  text-align: center;
}

/* 图片容器样式 */
.resource-image {
  flex: 0 0 35%; /* 改用百分比宽度 */
  height: 300px;
  display: flex;
  justify-content: center;
  align-items: center;
  overflow: hidden;
}

/* 固定图片尺寸 */
.fixed-image {
  width: 100%;
  height: 100%;
  object-fit: cover;
  border-radius: 8px;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.1);
}

/* 内容描述样式 */
.resource-description {
  width: 90%; /* 从100%减少到90% */
  padding: 4%;
  background-color: #f8f9fa;
  border-radius: 8px;
  min-height: 160px;
  margin-bottom: 20px;
}

.content-text {
  color: #5e6d82;
  font-size: 18px;
  line-height: 1.8;
  margin: 0;
  text-align: justify;
  display: -webkit-box;
  -webkit-box-orient: vertical;
  overflow: hidden;
  max-height: 160px;
  font-weight: 400;
  letter-spacing: 0.3px;
}

/* 下载按钮样式 */

.download-button {
  margin: 20px auto 0; /* 居中对齐，移除左边距 */
  width: 150px;
  background: #409eff;
  border: none;
  color: white;
  border-radius: 8px;
  padding: 12px 24px;
  font-size: 16px;
  cursor: pointer;
  display: inline-flex;
  align-items: center;
  gap: 10px;
  transition: all 0.3s ease;
  font-weight: 500;
  align-self: center; /* 在flex容器中居中 */
  text-decoration: none;
}

.download-button:hover {
  background: #66b1ff;
  transform: translateY(-2px);
  box-shadow: 0 4px 12px rgba(64, 158, 255, 0.2);
}

.download-button .el-icon {
  font-size: 20px;
}

/* 修改固定标题样式 */

/* 调整资源项的上边距 */
.resource-item:first-child {
  margin-top: 0;
}

.resource-body {
  display: flex;
  gap: 4%; /* 增加间距 */
  width: 100%;
  margin-top: 2%;
  padding: 2%;
}

.resource-right {
  flex: 1;
  display: flex;
  flex-direction: column;
  justify-content: space-between;
  max-width: 50%; /* 从60%减少到50% */
  padding-right: 2%;
}

.uploadBt {
  /* 获得焦点后鼠标样式变化 */
  cursor: pointer;
  position: fixed;
  right: 50px;
  bottom: 40px;
  line-height: 100px;
  text-align: center;
  border-radius: 50%;
  width: 100px;
  background-color: #2990f7;
  height: 100px;
  color: rgb(255, 255, 255);
  font-family: "Microsoft JhengHei";
  font-size: 20px;
}

.uploadBt:hover {
  background-color: #509fef;
}
</style>
