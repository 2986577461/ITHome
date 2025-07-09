<template>
  <Header></Header>
  <div class="container">
    <!-- 文章类型选择区域 -->
    <form @submit.prevent="uploadArticle">
      <div class="type-selection">
        <h2>选择文章类型</h2>
        <div class="radio-group">
          <label class="radio-item">
            <input type="radio" v-model="article.type" value=1 />
            <span>C语言</span>
          </label>
          <label class="radio-item">
            <input type="radio" v-model="article.type" value=2 />
            <span>HTML</span>
          </label>
          <label class="radio-item">
            <input type="radio" v-model="article.type" value=3 />
            <span>CSS</span>
          </label>
          <label class="radio-item">
            <input type="radio" v-model="article.type" value=4 />
            <span>JavaScript</span>
          </label>
          <label class="radio-item">
            <input type="radio" v-model="article.type" value=5 />
            <span>Java</span>
          </label>
          <label class="radio-item">
            <input type="radio" v-model="article.type" value=6 />
            <span>MySql</span>
          </label>
        </div>
      </div>

      <!-- 编辑器区域（包含标题和内容） -->
      <div class="editor-container">
        <!-- 标题输入区域移到这里 -->
        <div class="title-section">
          <input
            type="text"
            v-model="article.head"
            placeholder="请输入文章标题..."
            class="title-field"
            required
          />
        </div>

        <!-- 编辑器工具栏和内容 -->
        <Toolbar
          :editor="editorRef"
          :defaultConfig="toolbarConfig"
          class="editor-toolbar"
        />
        <Editor
          class="editor-content"
          v-model="article.content"
          :defaultConfig="editorConfig"
          @onCreated="handleCreated"
          style="height: 400px"
        />
        <div class="button-container">
          <el-button type="primary" native-type="submit" class="submit-button">
            {{ articleStore.getText }}
          </el-button>
        </div>
      </div>
    </form>
  </div>
  <Foot></Foot>
</template>

<script setup>
import "@wangeditor/editor/dist/css/style.css"; // 引入 第三方富文本框css
import Header from "./Header.vue";
import Foot from "./Foot.vue";
import { onBeforeUnmount, onMounted, reactive, shallowRef } from "vue";
import { Editor, Toolbar } from "@wangeditor/editor-for-vue"; //引入 第三方富文本框
import { ElMessage } from "element-plus";
import { useRouter } from "vue-router";
import { ElLoading } from "element-plus";
import { article_store } from "@/store/updateArticle";
import { user_store } from "@/store/user";
import {update, upload} from "@/request/axiosForArticles.js";
const articleStore = article_store();
const userStore = user_store();
// 编辑器实例，必须用 shallowRef
const editorRef = shallowRef();
const router = useRouter();

const article = reactive({
  id: null,
  type: 1,
  head: "",
  content: "",
});

const uploadArticle = async () => {
  
  if (article.content.length < 30) {
    ElMessage.error("文章内容太少！");
    return;
  }
  if (article.content.length > 4000) {
    ElMessage.error("内容太多了，少写点吧");
    return;
  }
  let resp;
  if (article.id !== null) {
    
    resp = await update(article);
  } else {
    
    resp = await upload(article);
  }
  if (resp.code === 200) {
    ElMessage.success("文章上传成功!");
    setTimeout(() => {
      const load = ElLoading.service({
        fullscreen: true,
        text: "Loading",
        background: "white",
      });
      setTimeout(() => {
        router.push("/tech-study");
        load.close();
      }, 700);
    }, 700);
  }
};

// 配置工具栏，添加需要的工具按钮
const toolbarConfig = {
  // 可以通过菜单的名称来配置需要的按钮
  toolbarKeys: [
    "fontSize",
    "fontFamily",
    "bold", // 加粗
    "italic", // 斜体
    "color",
    "emotion",
    "lineHeight",
    "divider",

    "justifyLeft",
    "justifyCenter",
    "justifyRight",
  ],
};

// 配置编辑器，设置占位符等
const editorConfig = {
  placeholder: "请输入内容...",
};

// 组件销毁时，及时销毁编辑器
onBeforeUnmount(() => {
  const editor = editorRef.value;
  if (editor == null) return;
  editor.destroy();
});

// 编辑器创建后的回调，获取 editor 实例
const handleCreated = (editor) => {
  editorRef.value = editor; // 记录 editor 实例，重要！
};

onMounted(async () => {
  if (articleStore.id != null) {
    setTimeout(() => {
      article.content = articleStore.content;
      article.type = articleStore.type;
      article.head = articleStore.head;
      article.id = articleStore.id;
    }, 1);
  }
});
</script>

<style scoped>
/* 整体容器进一步优化 */
.container {
  margin: 0 auto;
  padding: 40px;
  background-color: #f2f6fb;
  min-height: calc(100vh - 200px);
  position: relative;
  box-shadow: 0 0 50px rgba(0, 0, 0, 0.05) inset;
}

/* 更炫酷的顶部装饰条 */
.container::before {
  content: "";
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  height: 3px;
  background: linear-gradient(
    90deg,
    #ff6b6b 0%,
    #4ecdc4 20%,
    #45b7d1 40%,
    #96e6a1 60%,
    #ffd93d 80%,
    #ff6b6b 100%
  );
  animation: gradientFlow 6s linear infinite;
  background-size: 300% 100%;
}

@keyframes gradientFlow {
  0% {
    background-position: 0 0;
  }
  100% {
    background-position: 300% 0;
  }
}

/* 类型选择区域进一步美化 */
.type-selection {
  background: rgba(255, 255, 255, 0.95);
  padding: 35px 45px;
  border-radius: 24px;
  box-shadow: 0 15px 35px rgba(0, 0, 0, 0.1);
  margin-bottom: 35px;
  border: 1px solid rgba(255, 255, 255, 0.8);
  backdrop-filter: blur(10px);
  position: relative;
  overflow: hidden;
}

/* 添加装饰性背景 */
.type-selection::after {
  content: "";
  position: absolute;
  top: -50%;
  right: -50%;
  width: 100%;
  height: 100%;
  background: linear-gradient(
    45deg,
    transparent 45%,
    rgba(78, 205, 196, 0.1) 100%
  );
  transform: rotate(30deg);
  z-index: 0;
}

/* 单选按钮组布局优化 */
.radio-group {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(150px, 1fr));
  gap: 20px;
  position: relative;
  z-index: 1;
}

/* 单选按钮样式优化 */
.radio-item {
  display: flex;
  align-items: center;
  cursor: pointer;
  padding: 15px 25px;
  border-radius: 16px;
  background: linear-gradient(145deg, #ffffff, #f8f9fa);
  box-shadow: 5px 5px 15px #e6e6e6, -5px -5px 15px #ffffff;
  transition: all 0.4s cubic-bezier(0.4, 0, 0.2, 1);
  position: relative;
  overflow: hidden;
}

.radio-item:hover {
  background: linear-gradient(145deg, #f8f9fa, #ffffff);
  transform: translateY(-3px) scale(1.02);
  box-shadow: 0 15px 25px rgba(0, 0, 0, 0.1);
}

.radio-item::before {
  content: "";
  position: absolute;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  background: linear-gradient(
    120deg,
    transparent,
    rgba(78, 205, 196, 0.1),
    transparent
  );
  transform: translateX(-100%);
  transition: 0.5s;
}

.radio-item:hover::before {
  transform: translateX(100%);
}

/* 标题输入区域优化 */

/* 标题输入框优化 */
.title-field {
  width: 100%;
  padding: 18px 25px;
  border: 2px solid #e1e8ef;
  border-radius: 16px;
  font-size: 16px;
  transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
  outline: none;
  background: #ffffff;
  box-shadow: inset 0 2px 4px rgba(0, 0, 0, 0.05);
}

.title-field:focus {
  border-color: #4ecdc4;
  box-shadow: 0 0 0 4px rgba(78, 205, 196, 0.15);
  transform: translateY(-1px);
}

/* 标题样式优化 */
h2 {
  background: linear-gradient(45deg, #2c3e50, #3498db, #2c3e50);
  background-clip: text;
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  font-size: 26px !important;
  letter-spacing: 2px;
  margin-bottom: 30px !important;
  animation: gradientText 6s linear infinite;
  background-size: 200% auto;
}

@keyframes gradientText {
  0% {
    background-position: 0 50%;
  }
  50% {
    background-position: 100% 50%;
  }
  100% {
    background-position: 0 50%;
  }
}

/* 编辑器容器优化 */
.editor-container {
  background: rgba(255, 255, 255, 0.95);
  border-radius: 24px;
  box-shadow: 0 15px 35px rgba(0, 0, 0, 0.1);
  overflow: hidden;
  backdrop-filter: blur(10px);
}

/* 新增标题区域样式 */
.title-section {
  padding: 25px 30px;
  border-bottom: 1px solid rgba(226, 232, 240, 0.5);
  background: linear-gradient(to right, #f8fafc, #f1f5f9);
}

/* 修改标题输入框样式 */
.title-field {
  width: 95%;
  padding: 15px 20px;
  border: 2px solid #e1e8ef;
  border-radius: 12px;
  font-size: 18px;
  font-weight: 500;
  transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
  outline: none;
  background: #ffffff;
  box-shadow: inset 0 2px 4px rgba(0, 0, 0, 0.05);
}

.title-field::placeholder {
  color: #94a3b8;
}

.title-field:focus {
  border-color: #4ecdc4;
  box-shadow: 0 0 0 4px rgba(78, 205, 196, 0.15);
}

/* 按钮容器优化 */
.button-container {
  padding: 30px;
  text-align: center;
  background: linear-gradient(to right, #f8fafc, #f1f5f9);
  border-top: 1px solid rgba(226, 232, 240, 0.5);
}

/* 提交按钮进一步优化 */
.submit-button {
  padding: 18px 45px !important;
  font-size: 16px !important;
  border-radius: 16px !important;
  background: linear-gradient(45deg, #4ecdc4, #45b7d1, #4ecdc4) !important;
  background-size: 200% auto !important;
  border: none !important;
  color: white !important;
  cursor: pointer;
  transition: all 0.5s cubic-bezier(0.4, 0, 0.2, 1) !important;
  font-weight: 600 !important;
  letter-spacing: 2px;
  text-transform: uppercase;
  position: relative;
  overflow: hidden;
}

.submit-button:hover {
  background-position: right center !important;
  transform: translateY(-3px) scale(1.02);
  box-shadow: 0 20px 30px rgba(78, 205, 196, 0.3);
}

/* 编辑器工具栏进一步优化 */
.editor-toolbar {
  border-bottom: 1px solid rgba(226, 232, 240, 0.5);
  padding: 12px;
  background: linear-gradient(to right, #f8fafc, #f1f5f9) !important;
}

:deep(.w-e-toolbar button) {
  border-radius: 10px !important;
  transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1) !important;
  margin: 0 2px !important;
}

:deep(.w-e-toolbar button:hover) {
  background: rgba(78, 205, 196, 0.15) !important;
  color: #4ecdc4 !important;
  transform: translateY(-1px);
}

/* 编辑器内容区优化 */
.editor-content {
  padding: 25px 30px 0 !important;
}

/* 响应式设计优化 */
@media (max-width: 768px) {
  .container {
    padding: 20px;
  }

  .radio-group {
    grid-template-columns: repeat(auto-fit, minmax(120px, 1fr));
    gap: 15px;
  }

  .radio-item {
    padding: 12px 20px;
  }

  h2 {
    font-size: 22px !important;
  }

  .submit-button {
    padding: 15px 35px !important;
  }
}
</style>
