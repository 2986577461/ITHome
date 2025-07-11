<script setup>
import Header from "@/components/Header.vue";
import Footer from "@/components/Foot.vue";
import {user_store} from "@/store/user.js";
import {onMounted, reactive, computed} from "vue";
import {ref} from "vue";
import {useRouter} from "vue-router";
import {visible_store} from "@/store/visible.js";
import ResetPassword from "@/components/resetPassword.vue";
import {getAll} from "@/request/axiosForResources.js";

const visibleStore = visible_store();
const router = useRouter();

const imgArr = ["https://liuxingjielihaichuanlai.oss-cn-chengdu.aliyuncs.com/3.jpg",
  "https://liuxingjielihaichuanlai.oss-cn-chengdu.aliyuncs.com/2.jpg",
  "https://liuxingjielihaichuanlai.oss-cn-chengdu.aliyuncs.com/1.jpg",
  "https://liuxingjielihaichuanlai.oss-cn-chengdu.aliyuncs.com/4.jpg"];

const userStore = user_store();

let imgLink = ref("");

function getimg(item) {
  return imgArr[item - 1];
}

const gotoPage = (page) => {
  router.push(page);
};


let news = reactive([]);

function intoUrl(index) {
  switch (index) {
    case 1:
      imgLink.value = `https://dasai.lanqiao.cn/pages/
v7/dasai/competition/individual_competition.html`;
      break;
    case 2:
      imgLink.value = "https://jwc.mypt.edu.cn/web/web/student/";
      break;
    case 3:
      imgLink.value = "https://chat.chat826.com/#/login";
      break;
  }
}

const currentPage = ref(1);
const pageSize = 8; // 每页显示8条新闻

// 计算当前页应该显示的新闻
const currentPageNews = computed(() => {
  const startIndex = (currentPage.value - 1) * pageSize;
  const endIndex = startIndex + pageSize;
  return news.slice(startIndex, endIndex);
});

const handlePageChange = (page) => {
  currentPage.value = page;
  // 可以在这里添加滚动到新闻区域顶部的效果
  const newsBar = document.querySelector(".newsBar");
  if (newsBar) {
    newsBar.scrollIntoView({behavior: "smooth"});
  }
};

onMounted(async () => {
  const res = await getAll();
  news.splice(0, news.length, ...res.data);
});
</script>

<template>
  <Header></Header>
  <div class="main">
    <div class="headBar">
      <div class="picture">
        <el-carousel height="500px" arrow="always" direction="vertical">
          <el-carousel-item v-for="(item, index) in 4" :key="index">
            <a :href="imgLink" target="_blank" @click="intoUrl(index)"
            ><img :src="getimg(item)" alt="ngm" class="img"
            /></a>
          </el-carousel-item>
        </el-carousel>
      </div>
      <div class="messageBar">
        <div class="user-info">
          <h2 class="info-title">用户信息 / UserInfo</h2>
          <div class="info-item">
            <span class="label">账 号</span>
            <span class="value">{{ userStore.studentId }}</span>
          </div>
          <div class="info-item">
            <span class="label">用户名</span>
            <span class="value">{{ userStore.name }}</span>
          </div>
          <div class="info-item">
            <span class="label">身 份</span>
            <span class="value">{{ userStore.position }}</span>
          </div>
          <div class="info-item">
            <span class="label">单 位</span>
            <span class="value">{{ userStore.academy }}</span>
          </div>

          <div
              @click="visibleStore.resetPasswordVisible = true"
              class="change-password-btn"
              v-if="userStore.condition"
          >
            修改密码
          </div>
          <div
              class="logout-btn"
              @click="visibleStore.loginOrLogoutButton()"
              :class="{ 'full-width': !userStore.condition }"
          >
            {{ visibleStore.getText }}
          </div>
        </div>
      </div>
    </div>

    <div class="boxDiv">
      <div class="innerBox" @click="gotoPage('/tech-study')">
        <h3>🧑‍🏫技术教学</h3>
        <p>
          我们提供了大一所应具备的计算机知识,如C语言, HTML, CSS, JavaScript,
          Java, Mysql, 更好地解决你的编程问题。
        </p>
      </div>
      <div class="innerBox" @click="gotoPage('/leraningResource')">
        <h3>✅协会晚自习签到</h3>
        <p>
          协会通过网站实现对成员晚自习学习管理，方便社团管理部的的考勤检查，解决了😡查人慢，🤬找不到人的问题。
        </p>
      </div>
      <div class="innerBox" @click="gotoPage('/harvest')">
        <h3>🏅成果和信息</h3>
        <p>
          协会自2013年创办以来，收获了许多成果，这里展示了历年以来获得的优秀成果和协会信息，方便你快速地了解我们，🤗我们期待你的加入！
        </p>
      </div>
    </div>

    <div class="newsBar">
      <div
          class="newsStyle"
          v-for="item in currentPageNews"
          :key="item.id"
          @click="gotoPage('/leraningResource')"
      >
        <img :src="item.coverUrl" class="news-image">
        <div class="text">
          <div class="head">
            {{ item.head }}
          </div>
          <div class="content">
            {{ item.releaseDateTime + "上传" }}
          </div>
        </div>
      </div>
    </div>
    <!-- 分页器容器 -->
    <div class="pagination-container">
      <el-pagination
          class="pageBar"
          background
          size="large"
          layout="prev, pager, next"
          :total="news.length"
          :page-size="pageSize"
          :current-page="currentPage"
          @current-change="handlePageChange"
      />
    </div>

    <ResetPassword></ResetPassword>
    <Footer></Footer>
  </div>
</template>
<style scoped>
.main {
  background-color: #f2f6fb;
}

.headBar {
  margin: 0 auto;
  width: 95%;
  max-width: 1400px;
  height: auto;
  background-color: #fff;
  padding: 15px;
  display: flex;
  flex-wrap: wrap;
  gap: 20px;
}

.picture {
  margin: 0;
  flex: 1;
  min-width: 300px;
}

.messageBar {
  width: 350px;
  min-width: 300px;
  height: 500px;
}

.img {
  width: 100%;
  height: 500px;
}

.messageBar {
  display: inline-block;
  width: 400px;
  height: 500px;
  margin-left: 10px;
  border-radius: 12px;
  background-color: rgb(69, 163, 240);
  box-shadow: 0 8px 24px rgba(0, 0, 0, 0.15), 0 3px 8px rgba(0, 0, 0, 0.1);
  vertical-align: top;
  transition: all 0.3s ease;
  position: relative;
}

.messageBar:hover {
  transform: translateY(-2px);
  box-shadow: 0 12px 28px rgba(0, 0, 0, 0.2), 0 4px 10px rgba(0, 0, 0, 0.15);
}

.user-info {
  padding: 30px;
  color: white;
  height: calc(100% - 85px);
}

.info-title {
  font-size: 28px;
  margin-bottom: 40px;
  font-weight: normal;
  border-bottom: 1px solid rgba(0, 0, 0, 0.1);
  padding-bottom: 15px;
  color: white;
}

.info-item {
  display: flex;
  margin-bottom: 30px;
  font-size: 20px;
}

.label {
  width: 80px;
  color: white;
}

.value {
  flex: 1;
  padding-left: 20px;
  color: white;
}

.boxDiv {
  margin: 0 auto;
  width: 95%;
  display: grid;
  grid-template-columns: repeat(3, 1fr); /* 三列等宽布局 */
  gap: 30px; /* 增加间距 */
  padding: 20px;
  height: auto; /* 移除固定高度 */
}

.innerBox {
  cursor: pointer;
  font-family: "Microsoft JhengHei", serif;
  padding: 25px;
  background: white;
  border-radius: 8px;
  box-shadow: 0 0 15px rgba(0, 0, 0, 0.1);
  transition: all 0.3s ease;
  min-height: 180px; /* 设置最小高度 */
  display: flex;
  flex-direction: column;
  border: none; /* 移除红色边框 */
}

.innerBox:hover {
  transform: translateY(-10px);
  box-shadow: 0 15px 30px rgba(0, 0, 0, 0.15);
}

.innerBox h3 {
  color: #333;
  margin-bottom: 15px;
  font-size: 1.2em;
}

.innerBox p {
  color: #666;
  line-height: 1.6;
  margin: 0;
  flex-grow: 1;
}

/* 响应式布局 */
@media (max-width: 1200px) {
  .boxDiv {
    grid-template-columns: repeat(2, 1fr);
    gap: 25px;
  }
}

@media (max-width: 768px) {
  .boxDiv {
    grid-template-columns: 1fr;
    width: 90%;
    padding: 15px;
    gap: 20px;
  }

  .innerBox {
    min-height: 150px;
  }
}

.newsBar {
  border: 1px solid rgb(201, 199, 199);
  margin: 0 auto;
  width: 95%;
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 20px;
  background-color: #fff;
  border-radius: 20px;
  padding: 20px 0;
  min-height: 700px; /* 固定高度，防止分页切换时页面跳动 */
}

.text {
  display: inline-block;
  width: 280px;
  height: 150px;
  vertical-align: top;
  font-family: "Microsoft JhengHei", serif;
}

.newsStyle {
  margin: 0;
  width: auto;
  height: 150px;
}

.newsStyle:hover {
  background-color: #f4f1f1;
  cursor: pointer;
}

.newsStyle .head {
  text-align: center;
  overflow: hidden;
  text-overflow: ellipsis;
  height: 30px;
  font-weight: 400;
  font-size: 25px;
}

.newsStyle .head:hover {
  text-decoration: underline;
}

.newsStyle .content {
  margin-top: 8px;
  overflow: hidden;
  text-overflow: ellipsis;
  font-size: 21px;
  height: 120px;
  color: #8c8c94;
}

.pageBar {
  display: inline-flex;
  justify-content: center;
  align-items: center;
}

/* 自定义分页器样式 */


.pagination-container {
  display: flex;
  justify-content: center;
  width: 100%;
  margin: 30px 0;
  padding: 10px 0;
}

.logout-btn {
  position: absolute;
  bottom: 20px;
  right: 20px;
  width: 180px;
  height: 45px;
  background-color: #3f7bc4;
  color: white;
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  font-size: 18px;
  transition: all 0.3s ease;
  border-radius: 8px;
  box-shadow: 0 2px 6px rgba(0, 0, 0, 0.1);
}

.logout-btn:hover {
  background-color: #2a63ab;
  transform: translateY(-2px);
  box-shadow: 0 4px 8px rgba(0, 0, 0, 0.15);
}

/* 添加响应式布局 */
@media (max-width: 1200px) {
  .headBar {
    width: 98%;
  }

  .picture {
    width: 100%;
  }

  .messageBar {
    width: 100%;
    height: auto;
    min-height: 400px;
  }
}

/* 修改修改密码按钮样式 */
.change-password-btn {
  position: absolute;
  bottom: 20px;
  left: 40px;
  width: 150px;
  height: 45px;
  background-color: #3f7bc4;
  color: white;
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  font-size: 18px;
  transition: all 0.3s ease;
  border-radius: 8px;
  box-shadow: 0 2px 6px rgba(0, 0, 0, 0.1);
}

.change-password-btn:hover {
  background-color: #2a63ab;
  transform: translateY(-2px);
  box-shadow: 0 4px 8px rgba(0, 0, 0, 0.15);
}

/* 修改登出按钮样式 */
.logout-btn {
  position: absolute;
  bottom: 20px;
  right: 40px;
  width: 150px;
  height: 45px;
  background-color: #3f7bc4;
  color: white;
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  font-size: 18px;
  transition: all 0.3s ease;
  border-radius: 8px;
  box-shadow: 0 2px 6px rgba(0, 0, 0, 0.1);
}

/* 未登录时的样式 */
.logout-btn.full-width {
  left: 20px;
  right: 20px;
  width: auto;
}
.news-image {
  width: 150px;          /* 固定宽度 */
  height: 150px;         /* 固定高度，与 .text 区域对齐 */
  object-fit: cover;     /* 保持图片比例并填充容器 */
  border-radius: 8px;    /* 圆角效果 */
  margin-right: 15px;    /* 与右侧文字间隔 */
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1); /* 轻微阴影提升层次感 */
  transition: transform 0.3s ease; /* 悬停动画 */
}

.newsStyle:hover .news-image {
  transform: scale(1.02); /* 悬停时轻微放大 */
}
</style>
