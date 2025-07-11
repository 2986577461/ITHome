<script lang="ts" setup>
import {onMounted, ref} from "vue";
import Foot from "@/components/Foot.vue";
import UpdateStudent from "./colonyMessage/updateStudent.vue";
import {govern_store} from "@/store/govern.js";
import AddStudent from "./colonyMessage/addStudent.vue";
import {ElMessage} from "element-plus";
import {downloadExcel} from "@/request/axiosForUser";
import {getArticleCount} from '@/request/axiosForArticles';
import {getResourcesCount} from '@/request/axiosForResources';
import {getAllNewcomers} from '@/request/axiosForNewcomers';
import {removeBatch, getAll} from '@/request/axiosForUser';
import Header from "@/components/Header.vue";
import {CirclePlusFilled, DeleteFilled} from "@element-plus/icons-vue";

const governStore = govern_store();

const checked = ref([false]);

const resourceCount = ref(null);

const articleCount = ref(null);

const removeDialogVisible = ref(false);

const studentForRemove = ref([]);

const updateDialog = () => {
  //从学生集合过滤出被单选框选中的学生
  const student = governStore.member.filter(
      //如果索引所对应的单选框中的元素为true，则返回该学生
      (_student, index) => checked.value[index]
  );
  if (student.length === 0) {
    ElMessage.error("请选择要修改的学生");
    return;
  }
  if (student.length > 1) {
    ElMessage.error("只能选择一个学生进行修改");
    return;
  }
  governStore.oldStudentId = student[0].studentId;
  governStore.updateStudent = student[0];

  governStore.turnUpdateSwitch();
};

const removeDialog = () => {
  studentForRemove.value = governStore.member.filter(
      // 计算被选中的学生数量
      (_student, index) => checked.value[index]
  );

  if (studentForRemove.value.length === 0) {
    ElMessage.error("请选择要删除的学生");
    return;
  }
  removeDialogVisible.value = true;
};
const download = async () => {
  // 下载学生信息
  const resp = await downloadExcel();
  console.log(resp)
  // 创建一个 Blob 对象
  const blob = new Blob([resp], {type: "application/vnd.ms-excel"});
  // 创建一个 URL 对象
  const url = window.URL.createObjectURL(blob);
  // 创建一个链接元素
  const link = document.createElement("a");
  link.href = url;
  link.download = "IT之家协会花名册.xlsx"; // 设置下载文件名
  // 模拟点击下载
  document.body.appendChild(link);
  link.click();
  // 清理资源
  document.body.removeChild(link);
  window.URL.revokeObjectURL(url); // 释放 URL 对象
  ElMessage.success("花名册已下载");
  // 这里可以添加下载成功的提示
};

const remove = async () => {
  // 所有被选中的学生;
  const students = ref([]);

  for (const student of studentForRemove.value)
    students.value.push(student.id);
  // 发送请求
  const condition = await removeBatch(students.value);

  if (condition.code === false) {
    ElMessage.error("删除失败");
    return;
  }
  ElMessage.success("删除成功");
  removeDialogVisible.value = false;

  // 删除学生：更新 member 数组
  governStore.member.splice(
      0,
      governStore.member.length,
      ...governStore.member.filter((_student, index) => !checked.value[index])
  );

  // 清空选中状态
  checked.value = checked.value.map(() => false);
};

onMounted(async () => {
  const resp = await getAll(); // 获取数据
  governStore.member.splice(0, governStore.member.length, ...resp.data);

  resourceCount.value = (await getResourcesCount()).data;
  articleCount.value = (await getArticleCount()).data;

  const list = await getAllNewcomers();
  // 更新学员列表
  governStore.newcomers.splice(0, governStore.newcomers.length, ...list.data);
});
</script>

<template>
  <Header></Header>
  <div class="main">
    <div class="headDiv">
      <div class="title">IT学员管理系统</div>
    </div>
    <div class="content-wrapper">
      <div class="control-panel">
        <el-badge
            :value="governStore.newcomers.length"
            :max="50"
            type="danger"
            :offset="[-20, 5]"
            :hidden="!governStore.displayStudentCount()"
        >
          <el-button
              type="success"
              class="controllerBt"
              @click="governStore.turnaddStudentSwitch()"
          >
            <span>新增</span>
            <el-icon>
              <CirclePlusFilled/>
            </el-icon>
          </el-button>
        </el-badge>
        <el-button type="warning" class="controllerBt" @click="updateDialog"
        ><span>修改</span>
          <el-icon>
            <Edit/>
          </el-icon>
        </el-button>
        <el-button type="danger" class="controllerBt" @click="removeDialog"
        ><span>删除</span>
          <el-icon>
            <DeleteFilled/>
          </el-icon>
        </el-button>
        <el-button type="success" class="controllerBt" @click="download()">
          <span>下载名单</span>
          <el-icon>
            <CirclePlusFilled/>
          </el-icon>
        </el-button>
      </div>

      <div class="stats-panel">
        <div class="stat-card">
          <div class="stat-label">协会人数</div>
          <div class="stat-value">{{ governStore.member.length }}</div>
        </div>
        <div class="stat-card">
          <div class="stat-label">文章数量</div>
          <div class="stat-value">{{ articleCount }}</div>
        </div>
        <div class="stat-card">
          <div class="stat-label">资料数量</div>
          <div class="stat-value">{{ resourceCount }}</div>
        </div>
      </div>

      <div class="table-container">
        <div class="table-header">
          <ul>
            <li>选择</li>
            <li>学号</li>
            <li>姓名</li>
            <li>性别</li>
            <li>专业</li>
            <li>班级</li>
            <li>学院</li>
            <li>职位</li>
            <li>文章数量</li>
            <li>资料数量</li>
          </ul>
        </div>

        <div
            class="table-row"
            v-for="(student, index) in governStore.member"
            :key="student.claxx"
        >
          <div class="table-cell checkbox-cell">
            <el-checkbox v-model="checked[index]" size="large"/>
          </div>
          <div class="table-cell">{{ student.studentId }}</div>
          <div class="table-cell">{{ student.name }}</div>
          <div class="table-cell">{{ student.sex }}</div>
          <div class="table-cell">{{ student.major }}</div>
          <div class="table-cell">{{ student.className }}</div>
          <div class="table-cell">{{ student.academy }}</div>
          <div class="table-cell">{{ student.position }}</div>
          <div class="table-cell">{{ student.articleCount }}</div>
          <div class="table-cell">{{ student.resourceCount }}</div>
        </div>
      </div>
    </div>
    <UpdateStudent></UpdateStudent>
    <AddStudent></AddStudent>
    <el-dialog
        v-model="removeDialogVisible"
        title="警告"
        width="450"
        top="30vh"
        style="font-family: '微软雅黑'; letter-spacing: 2px"
    >
      <h3 style="font-size: 16px; font-weight: 400">
        <!-- 通过监测删除学生的数量决定模版字符串所展示的内容 -->
        {{
          `确定删除${
              studentForRemove.length === 1
                  ? studentForRemove[0].name + "同学"
                  : "这" + studentForRemove.length + "个学生"
          }吗`
        }}
      </h3>
      <template #footer>
        <div>
          <el-button @click="removeDialogVisible = false">返回</el-button>
          <el-button type="danger" @click="remove"> 确定</el-button>
        </div>
      </template>
    </el-dialog>
  </div>
  <Foot></Foot>
</template>

<style scoped>
.main {
  min-height: 100vh;
  background-color: #f2f6fb;
}

.headDiv {
  background-color: #4f98ff;
  padding: 1rem;
  box-shadow: 0 2px 4px rgba(0, 0, 0, 0.2);
}

.title {
  font-family: "幼圆", serif;
  font-size: 40px;
  color: #ffffff;
  text-align: center;
}

.content-wrapper {
  padding: 1.5rem;
  max-width: 1400px;
  margin: 0 auto;
}

.control-panel {
  background-color: #ffffff;
  padding: 1rem;
  border-radius: 8px;
  margin-bottom: 1.5rem;
  box-shadow: 0 2px 4px rgba(0, 0, 0, 0.1);
}

.controllerBt {
  width: 100px;
  height: 40px;
  font-size: 17px;
  font-family: "微软雅黑", serif;
  margin: 0 13px;
}

.stats-panel {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(200px, 1fr));
  gap: 1rem;
  margin-bottom: 1.5rem;
}

.stat-card {
  background-color: #ffffff;
  padding: 1.5rem;
  border-radius: 8px;
  text-align: center;
  box-shadow: 0 2px 4px rgba(0, 0, 0, 0.1);
}

.stat-label {
  color: #666;
  margin-bottom: 0.5rem;
}

.stat-value {
  font-size: 2rem;
  color: #4a9eff;
}

.table-container {
  background-color: #ffffff;
  border-radius: 12px;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.1);
  overflow: hidden;
}

.table-header ul {
  display: grid;
  grid-template-columns: repeat(11, 1fr);
  margin: 0;
  padding: 0;
  list-style: none;
  background-color: #4f98ff;
  border-bottom: 2px solid #3d86eb;
}

.table-header li {
  padding: 1rem;
  text-align: center;
  font-weight: 600;
  color: #ffffff;
  font-size: 0.95rem;
  letter-spacing: 1px;
}

.table-row {
  display: grid;
  grid-template-columns: repeat(11, 1fr);
  border-bottom: 1px solid #eef2f7;
  transition: background-color 0.2s ease;
}

.table-row:hover {
  background-color: #f8fafd;
}

.table-cell {
  padding: 1rem 0.5rem;
  vertical-align: middle;
  text-align: center;
  color: #5a6c84;
  font-size: 0.9rem;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.table-cell:nth-child(9) {
  font-weight: 500;
}

.table-cell:nth-child(9):contains("在线") {
  color: #67c23a;
}

.table-cell:nth-child(9):contains("离线") {
  color: #909399;
}

.table-cell:nth-child(10),
.table-cell:nth-child(11) {
  color: #409eff;
  font-weight: 500;
}

.checkbox-cell {
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 0.5rem;
}

@media screen and (max-width: 1200px) {
  .table-cell {
    font-size: 0.85rem;
    padding: 0.75rem 0.25rem;
  }

  .table-header li {
    font-size: 0.9rem;
    padding: 0.75rem 0.25rem;
  }
}
</style>
