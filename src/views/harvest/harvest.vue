<template>
  <Header></Header>
  <div class="main">
    <div class="card-container">
      <el-divider content-position="center">奖状展示</el-divider>

      <el-row :gutter="20">
        <el-col :span="8" v-for="i in 6">
          <el-card class="box-card certificate-card">
            <template #header>
              <div class="card-header">
                <span>获奖证书 {{ i }}</span>
              </div>
            </template>
            <div class="certificate-content">
              <div class="certificate">
                <div class="certificate-image">
                  <el-image
                    :src="certificateImages[i - 1]"
                    fit="cover"
                    class="award-image"
                    :preview-src-list="certificateImages"
                    :initial-index="i - 1"
                    preview-teleported
                  />
                </div>
                <h3 class="certificate-title">
                  {{ certificateTitles[i - 1] }}
                </h3>
                <div class="certificate-details">
                  <h4>{{ certificateNames[i - 1] }}</h4>
                  <p>{{ certificateDescriptions[i - 1] }}</p>
                </div>
              </div>
            </div>
          </el-card>
        </el-col>
      </el-row>
    </div>
  </div>
  <Foot></Foot>
</template>

<script setup>
import Foot from "@/components/Foot.vue";
import Header from "@/components/Header.vue";
import Introduce from "@/components/Introduce.vue";

// 证书相关数据
const certificateImages = [
  "https://liuxingjielihaichuanlai.oss-cn-chengdu.aliyuncs.com/medal1.png",
  "https://liuxingjielihaichuanlai.oss-cn-chengdu.aliyuncs.com/medal3.png",
  "https://liuxingjielihaichuanlai.oss-cn-chengdu.aliyuncs.com/medal6.png",
  "https://liuxingjielihaichuanlai.oss-cn-chengdu.aliyuncs.com/medal7.png",
  "https://liuxingjielihaichuanlai.oss-cn-chengdu.aliyuncs.com/medal2.png",
  "https://liuxingjielihaichuanlai.oss-cn-chengdu.aliyuncs.com/medal9.png",
];

const certificateTitles = [
  "荣誉证书",
  "荣誉证书",
  "荣誉证书",
  "荣誉证书",
  "荣誉证书",
  "荣誉证书",
];

const certificateNames = [
  "孙宇",
  "任周燃",
  "李仁杰",
  "李毅 曹峥",
  "靳玉超",
  "曹峥",
];

const certificateDescriptions = [
  "在2022-2023学年中表现优异,成绩优秀。",
  "在2020-2021学年中表现优异,成绩优秀。",
  "在四川技能大赛物联网安装调试中，成绩优秀。",
  "在四川技能大赛计算机程序设计中，成绩优秀。",
  "第十五届蓝桥杯C/C++本科组获得省赛二等奖。",
  "鲲鹏应用创新大赛省赛二等奖。",
];
</script>

<style scoped>
.main {
  background-color: #f2f6fb;
}
.card-container {
  padding: 20px;
  max-width: 1200px;
  margin: 0 auto;
}

.box-card {
  margin-bottom: 20px;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.certificate-card {
  background: #fafafa;
  height: 450px;
  transition: all 0.3s ease;
  transform-origin: center;
}

.certificate-card:hover {
  transform: scale(1.02);
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.1);
}

.certificate-content {
  height: calc(100% - 60px);

  display: flex;
  align-items: center;
}

.certificate {
  width: 100%;
  height: 100%;
  display: flex;
  flex-direction: column;
  justify-content: space-between;
}

.certificate-title {
  color: #b80b22;
  font-size: 20px;
}

.certificate-details h4 {
  color: #303133;
  font-size: 18px;
  margin: 8px 0;
}

.certificate-details p {
  color: #606266;
  margin: 6px 0;
}

.award-image {
  height: 24vh;
  object-fit: cover;
  border-radius: 4px;
}
</style>
