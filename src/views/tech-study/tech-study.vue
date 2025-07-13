<script lang="ts" setup>
import { onMounted, reactive, ref } from "vue";
import Header from "@/components/Header.vue";
import { user_store } from "@/store/user";
import { useRouter } from "vue-router";
import { ElLoading, ElMessage, ElDialog } from "element-plus";
import { article_store } from "@/store/updateArticle";
import { deleteById,getAll } from "@/request/axiosForArticles";
import {Delete, Edit, MoreFilled} from "@element-plus/icons-vue";
const articleStore = article_store();
const userStore = user_store();
const router = useRouter();
const containerRef = ref<HTMLElement | null>(null);

const Article = reactive([]);

const removeDialogVisible = ref(false);
const articleToDelete = ref(null);

const handleClick = (e) => {
  e.preventDefault();
};

const getType=(type)=>{
  if(type==1)
    return "C语言";
  else if(type==2)
    return "HTML";
  else if(type==3)
    return "CSS"
  else if(type==4)
    return "JavaScript"
  else if(type==5)
    return "Java"
  else if(type==6)
    return "MySQL"
}

const handleEdit = (item) => {
  articleStore.setArticle(item.id, item.head, item.content, item.type);

  const load = ElLoading.service({
    fullscreen: true,
    text: "Loading",
    background: "white",
  });
  setTimeout(() => {
    router.push("/uploadArticle");
    load.close();
  }, 400);
};

const issueArticle = () => {
  articleStore.setArticle(null, "", "", "");

  const load = ElLoading.service({
    fullscreen: true,
    text: "Loading",
    background: "white",
  });
  setTimeout(() => {
    router.push("/uploadArticle");
    load.close();
  }, 400);
};

const handleDelete = async () => {
  if (!articleToDelete.value) return;
  const condition = await deleteById(articleToDelete.value.id);
  if (!condition) {
    ElMessage.error("删除失败");
  } else {
    ElMessage.success("删除成功");
    const index = Article.findIndex(
      (article) => article.id === articleToDelete.value.id
    );
    if (index > -1) {
      Article.splice(index, 1);
    }
  }
  removeDialogVisible.value = false;
  articleToDelete.value = null;
};

onMounted(async () => {
  const resp = await getAll();
  Article.splice(0, 0, ...resp.data);
});
</script>
<template>
  <Header></Header>
  <div style="display: flex">
    <!-- 左侧导航栏 -->
    <el-scrollbar class="sidebar">
      <h1 class="logo">技术教学</h1>
      <el-anchor
        :container="containerRef"
        direction="vertical"
        :offset="30"
        type="default"
        style="background: rgba(0, 0, 0, 0)"
      >
        <nav class="nav-menu">
          <div class="nav-group">
            <div class="nav-item parent">C语言</div>
            <div class="sub-items">
              <el-anchor-link
                v-for="item in Article.filter((item) => item.type == 1)"
                :href="`#part${item.id}`"
                @click="handleClick"
                ><div class="nav-sub-item">
                  <span class="asideText">{{ item.head }}</span>
                </div></el-anchor-link
              >
            </div>
          </div>
          <div class="nav-group">
            <div class="nav-item parent">HTML</div>
            <div class="sub-items">
              <el-anchor-link
                v-for="item in Article.filter((item) => item.type == 2)"
                :href="`#part${item.id}`"
                @click="handleClick"
                ><div class="nav-sub-item">
                  {{ item.head }}
                </div></el-anchor-link
              >
            </div>
          </div>
          <div class="nav-group">
            <div class="nav-item parent">CSS</div>
            <div class="sub-items">
              <el-anchor-link
                v-for="item in Article.filter((item) => item.type == 3)"
                :href="`#part${item.id}`"
                @click="handleClick"
                ><div class="nav-sub-item">
                  <span class="asideText">{{ item.head }}</span>
                </div></el-anchor-link
              >
            </div>
          </div>
          <div class="nav-group">
            <div class="nav-item parent">JavaScript</div>
            <div class="sub-items">
              <el-anchor-link
                v-for="item in Article.filter((item) => item.type == 4)"
                :href="`#part${item.id}`"
                @click="handleClick"
                ><div class="nav-sub-item">
                  <span class="asideText">{{ item.head }}</span>
                </div></el-anchor-link
              >
            </div>
          </div>
          <div class="nav-group">
            <div class="nav-item parent">Java</div>
            <div class="sub-items">
              <el-anchor-link
                v-for="item in Article.filter((item) => item.type == 5)"
                :href="`#part${item.id}`"
                @click="handleClick"
                ><div class="nav-sub-item">
                  <span class="asideText">{{ item.head }}</span>
                </div></el-anchor-link
              >
            </div>
          </div>
          <div class="nav-group">
            <div class="nav-item parent">Mysql</div>
            <div class="sub-items">
              <el-anchor-link
                v-for="item in Article.filter((item) => item.type == 6)"
                :href="`#part${item.id}`"
                @click="handleClick"
                ><div class="nav-sub-item">
                  <span class="asideText">{{ item.head }}</span>
                </div></el-anchor-link
              >
            </div>
          </div>
        </nav>
      </el-anchor>
    </el-scrollbar>

    <!-- 右侧内容区域 -->
    <div class="content-area" ref="containerRef">
      <div>
        <div class="article" v-for="item in Article" :id="`part${item.id}`">
          <div class="more">
            <el-dropdown
              trigger="hover"
              style="cursor: pointer"
              v-show="item.studentId == userStore.studentId"
            >
              <el-icon size="20"><MoreFilled /></el-icon>
              <template #dropdown>
                <el-dropdown-menu>
                  <el-dropdown-item @click="handleEdit(item)">
                    <el-icon><Edit /></el-icon>
                    <span>修改</span>
                  </el-dropdown-item>
                  <el-dropdown-item
                    @click="
                      articleToDelete = item;
                      removeDialogVisible = true;
                    "
                  >
                    <el-icon><Delete /></el-icon>
                    <span>删除</span>
                  </el-dropdown-item>
                </el-dropdown-menu>
              </template>
            </el-dropdown>
          </div>

          <!-- 文章标题 -->
          <h1 class="article-title" v-text="item.head"></h1>
          <!-- 文章信息栏 -->
          <div class="article-meta">
            <span class="meta-item">
              <span class="article-type">{{ getType(item.type) }}</span>
            </span>
            <span class="meta-item">{{ `作者：${item.name}` }}</span>
            <span class="meta-item"> 最后修改时间: {{ item.updatedDateTime }} </span>
          </div>

          <!-- 文章内容 -->
          <div class="article-content" v-html="item.content"></div>
        </div>
      </div>
    </div>

    <div class="uploadBt" v-if="userStore.condition" @click="issueArticle">
      去发表
    </div>
  </div>

  <el-dialog
    v-model="removeDialogVisible"
    title="确认删除"
    width="30%"
    top="30vh"
    :before-close="
      () => {
        removeDialogVisible = false;
        articleToDelete = null;
      }
    "
  >
    <span>确定要删除这篇文章吗？</span>
    <template #footer>
      <span class="dialog-footer">
        <el-button
          @click="
            removeDialogVisible = false;
            articleToDelete = null;
          "
          >取消</el-button
        >
        <el-button type="danger" @click="handleDelete">确定</el-button>
      </span>
    </template>
  </el-dialog>
</template>
<style scoped>
.sidebar {
  width: 18%;
  background-color: #4a8edc;
  color: white;
  padding: 0 20px;
  height: calc(100vh - 8vh);
  position: fixed;
  left: 0;
  top: 8vh;
}
.asideText {
  font-size: 14px;
  font-weight: 500;
  transition: color 0.3s ease;
  /* 文本过长时显示省略号 */
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  max-width: 180px;
}
.logo {
  font-size: 24px;
  margin-bottom: 40px;
  text-align: center;
  position: relative;
  transition: all 0.3s ease;
  padding: 0 10px;
}
.more {
  text-align: right;
  padding: 10px 20px;
}

.nav-menu {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.nav-item {
  color: white;
  text-decoration: none;
  padding: 8px 16px;
  cursor: pointer;
  transition: all 0.3s ease;
  transform-origin: left;
  position: relative;
}

.nav-item:hover {
  padding-left: 24px;
  color: #ffffff;
  text-shadow: 0 0 8px rgba(255, 255, 255, 0.4);
}

.nav-item:before {
  content: "";
  position: absolute;
  left: 0;
  top: 0;
  width: 100%;
  height: 100%;
  background-color: rgba(255, 255, 255, 0.1);
  transform: scaleX(0);
  transform-origin: left;
  transition: transform 0.3s ease;
  border-radius: 4px;
}

.nav-item:hover:before {
  transform: scaleX(1);
}

.nav-sub-item {
  color: rgba(255, 255, 255, 0.8);
  text-decoration: none;
  padding: 8px 16px;

  transition: all 0.3s ease;

  position: relative;
}

.nav-sub-item:hover {
  transform: scale(1.05);
  padding-left: 24px;
  color: white;
  text-shadow: 0 0 8px rgba(255, 255, 255, 0.4);
  letter-spacing: 0.5px;
}

.sub-items {
  transition: all 0.3s ease;
  opacity: 0;
  transform: translateY(-10px);
  margin-left: 20px;
}

.nav-group:hover .sub-items {
  opacity: 1;
  transform: translateY(0);
}

.nav-item.parent:after {
  content: "▼";
  position: absolute;
  right: 16px;
  top: 50%;
  transform: translateY(-50%) rotate(-90deg);
  font-size: 0.8em;
  transition: all 0.3s ease;
  opacity: 0.8;
}

.nav-group:hover .nav-item.parent:after {
  transform: translateY(-50%) rotate(0);
  opacity: 1;
}

.nav-item.active,
.nav-sub-item.active {
  background-color: rgba(255, 255, 255, 0.15);
  transform: scale(1.05);
}

.nav-group {
  position: static;
}

.nav-group .sub-items {
  display: none;
  position: static;
  background-color: transparent;
  box-shadow: none;
  padding: 0;
  margin-left: 20px;
}

.nav-group.active .sub-items,
.nav-group:hover .sub-items {
  display: block;
}

.nav-item.parent:after {
  content: "▼";
  position: absolute;
  right: 16px;
  top: 50%;
  transform: translateY(-50%) rotate(-90deg);
  font-size: 0.8em;
  transition: transform 0.3s;
}

.nav-group:hover .nav-item.parent:after {
  transform: translateY(-50%) rotate(0);
}

.nav-sub-item {
  padding: 8px 16px;
  color: rgba(255, 255, 255, 0.8);
  font-size: 0.95em;
  transition: color 0.3s;
}

.nav-sub-item:hover {
  background-color: transparent;
  color: white;
}

.sub-items {
  transition: all 0.3s ease;
}

.uploadBt {
  position: fixed;
  right: 20px;
  bottom: 20px;
  width: 100px;
  height: 40px;
  line-height: 40px;
  text-align: center;
  background-color: #2f98e4;
  color: white;
  border-radius: 4px;
  cursor: pointer;
  transition: all 0.3s ease;
}

.article {
  display: flex;
  flex-direction: column;
  margin: 20px auto 40px;
  background: white;

  border-radius: 2px;
  min-height: 85vh;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.1);
  transition: all 0.3s ease;
  border: 1px solid #eaeaea;
  width: 90%;
  box-sizing: border-box;
}

.article:hover {
  transform: translateY(-1px);
  box-shadow: 0 4px 20px rgba(0, 0, 0, 0.15);
}

.article-title {
  font-size: 32px;
  color: #333;
  margin: 25px 0 0 50px;
  font-weight: bold;
  line-height: 1.4;
}

.article-meta {
  border-radius: 6px;
  background-color: #edebeb;
  margin: 15px 50px;
  color: #666;
  font-size: 14px;
  display: flex;
  align-items: center;
  height: 50px;
}

.meta-item {
  display: inline-flex;
  align-items: center;
  padding: 4px 8px;
  border-radius: 4px;

  transition: background-color 0.2s ease;
}
.collect,
.likes {
  cursor: pointer;
}

.collect:hover,
.likes:hover {
  color: #409eff;
}

.article-tags {
  margin: 15px 0;
  color: #666;
  font-size: 14px;
}

.article-tags span {
  margin-right: 10px;
}

.el-tag {
  margin-right: 10px;
  transition: all 0.2s ease;
}

.el-tag:hover {
  transform: scale(1.05);
}

.article-content {
  flex-grow: 1;
  line-height: 1.8;
  margin: 30px 50px;
  padding: 20px 0;
  max-width: 100%;
  height: auto;
  word-wrap: break-word;
  box-sizing: border-box;
}

.article-content a {
  color: #409eff;
  text-decoration: none;
}

.article-content a:hover {
  text-decoration: underline;
}

.content-area {
  height: 92vh;
  overflow-x: hidden;
  margin-left: 20%;
  width: 82%;
  background-color: #f2f6fb;
}

.article-type {
  background-color: #1976d2;
  color: white;
  padding: 2px 8px;
  border-radius: 4px;
  font-size: 14px;
  font-weight: 500;
  margin-right: 15px;
  box-shadow: 0 2px 4px rgba(0, 0, 0, 0.1);
}

:deep(.el-dropdown-menu__item) {
  display: flex;
  align-items: center;
  gap: 5px;
}

:deep(.el-dropdown-menu__item:hover) {
  background-color: #f5f7fa;
}

:deep(.el-icon) {
  margin-right: 5px;
}
</style>
