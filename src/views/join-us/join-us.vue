<template>
  <HeaderVue></HeaderVue>
  <div class="main">
    <div class="head">我们为你提供</div>
    <div class="boxDiv">
      <div class="innerBox">
        <h3>💻计算机学习教学</h3>
        <p>
          我们提供了大一所应具备的计算机知识,如C语言, HTML, CSS, JavaScript,
          Java, Mysql, 更好地解决你的编程问题。
        </p>
      </div>
      <div class="innerBox">
        <h3>🧑‍💻编程学习路线</h3>
        <p>
          依据最新的企业就业环境，为你指导计算机目前的最流行的技术栈，避免学到"无用"的专业知识。
        </p>
      </div>
      <div class="innerBox">
        <h3>🏫工作室学习环境</h3>
        <p>
          将你在教室里上晚自习调整为在我们工作室学习，其余不问，不会占用你的空闲时间。
        </p>
      </div>
      <div class="innerBox">
        <h3>💾学习资料</h3>
        <p>秉承着开放和共享的原则，我们允许学员自行上传和下载学习资料😄。</p>
      </div>
      <div class="innerBox">
        <h3>🆚计算机竞赛</h3>
        <p>
          蓝桥杯、职业技能大赛是我们的最爱。门槛不高，只需会使用任意一门编程语言。
        </p>
      </div>
      <div class="innerBox">
        <h3>🎡协会活动</h3>
        <p>
          近几年，在电子与信息学院团学会的带领下，开展了很多社团活动，学习之余也不忘让你的大学生活更加美好。
        </p>
      </div>
    </div>

    <div class="form-container">
      <h2>加入我们</h2>
      <form @submit.prevent="submitForm">
        <div class="form-item">
          <label for="studentID">学号：</label>
          <input type="text" id="studentID" v-model="form.studentId" required />
        </div>

        <div class="form-item">
          <label for="name">姓名：</label>
          <input type="text" id="name" v-model="form.name" required />
        </div>

        <div class="form-item">
          <label for="sex">性别：</label>
          <select id="sex" v-model="form.sex" required>
            <option value=""></option>
            <option value="男">男</option>
            <option value="女">女</option>
          </select>
        </div>
        <div class="form-item">
          <label for="claxx">专业：</label>
          <input
            type="text"
            id="major"
            v-model="form.major"
            required
            placeholder="例如：软件技术"
          />
        </div>
        <div class="form-item">
          <label for="className">班级：</label>
          <input
            type="text"
            id="claxx"
            v-model="form.className"
            required
            placeholder="例如：252"
          />
        </div>

        <div class="form-item">
          <label for="academy">学院：</label>
          <select id="academy" v-model="form.academy" required>
            <option value=""></option>
            <option v-for="academy in academys" :value="academy">
              {{ academy + "学院" }}
            </option>
          </select>
        </div>

        <div class="form-item">
          <label for="introduce">简介：</label>
          <textarea
            id="introduce"
            v-model="form.introduce"
            placeholder="介绍一下你的信息，爱好，学习计划等。"
            required
            class="introduce-input"
          ></textarea>
        </div>

        <button type="submit">提交</button>
      </form>
    </div>
  </div>

  <Foot></Foot>
</template>

<script setup>
import { ElLoading, ElMessage } from "element-plus";
import { sendApply } from "@/request/axiosForNewcomers.js";
import Foot from "@/components/Foot.vue";
import HeaderVue from "@/components/Header.vue";
import { ref } from "vue";
import { useRouter } from "vue-router";
const router = useRouter();

const academys = [
  "马克思主义",
  "公共教育",
  "材料与建造",
  "电子与信息",
  "智能制造",
  "财经商贸",
  "旅游与管理",
  "艺术设计",
];

const form = ref({
  studentId: null,
  name: "",
  sex: "",
  major: "",
  className: "",
  academy: "",
  introduce: "",
});
const submitForm = async () => {
  const introduceLength = form.value.introduce.length;
  if (introduceLength >= 1000) {
    ElMessage.error("自我介绍太长了。");
    return;
  }
  if (introduceLength < 50) {
    ElMessage.error("自我介绍太短了。");
    return;
  }

  const resp = await sendApply(form.value);
  if (resp) {
    ElMessage.success("申请成功。");
    setTimeout(() => {
      const load = ElLoading.service({
        fullscreen: true,
        text: "Loading",
        background: "white",
      });
      setTimeout(() => {
        router.push("/home");
        load.close();
      }, 700);
    }, 1200);
  } else ElMessage.error("重复申请。");
};
</script>

<style scoped>
.main {
  height: 100%;
  background-color: #f2f6fb;
}
.head {
  padding-top: 3%;
  text-align: center;
  font-family: "Microsoft JhengHei";
  text-shadow: 0 0 15px rgba(255, 255, 255, 0.5);
  font-size: 60px;
}
.boxDiv {
  margin: 20px auto;
  width: 80%;
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  grid-template-rows: repeat(2, auto);
  gap: 30px;
  padding: 20px;
}

.innerBox {
  font-family: "Microsoft JhengHei";
  padding: 25px;
  background: white;
  border-radius: 8px;
  box-shadow: 0 0 15px rgba(0, 0, 0, 0.1);
  transition: all 0.3s ease;
  min-height: 180px;
  display: flex;
  flex-direction: column;
}

.innerBox:hover {
  transform: translateY(-7px);
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

.form-container {
  width: 80%;
  margin: 60px auto;
  padding: 30px;
  background: white;
  border-radius: 8px;
  box-shadow: 0 0 20px rgba(0, 0, 0, 0.1);
  transition: all 0.3s ease;
  max-width: 800px;
  position: relative;
}

.form-container:hover {
  transform: translateY(-5px);
  box-shadow: 0 5px 30px rgba(0, 0, 0, 0.15);
}

.form-item {
  margin-bottom: 25px;
  display: flex;
  align-items: center;
  width: 100%;
  max-width: 600px;
  margin-left: auto;
  margin-right: auto;
}

label {
  width: 80px;
  text-align: right;
  margin-right: 15px;
  font-size: 16px;
  flex-shrink: 0;
  line-height: 1;
}

input,
select {
  flex: 1;
  padding: 12px 15px;
  border: 2px solid #ddd;
  border-radius: 6px;
  font-size: 16px;
  transition: all 0.3s ease;
  width: 100%;
  height: 45px;
  box-sizing: border-box;
}

input:focus,
select:focus {
  border-color: #4caf50;
  box-shadow: 0 0 8px rgba(76, 175, 80, 0.2);
  outline: none;
}

button {
  position: static;
  display: block;
  margin: 30px auto 0;
  padding: 12px 40px;
  background-color: #4caf50;
  color: white;
  border: none;
  border-radius: 6px;
  cursor: pointer;
  font-size: 16px;
  transition: all 0.3s ease;
  min-width: 120px;
}

button:hover {
  background-color: #45a049;
  transform: scale(1.05);
}

h2 {
  text-align: center;
  margin-bottom: 30px;
  color: #333;
  font-size: 28px;
}

/* 响应式布局 */
@media (max-width: 1200px) {
  .boxDiv {
    grid-template-columns: repeat(2, 1fr);
    grid-template-rows: repeat(3, auto);
    gap: 25px;
  }
}

@media (max-width: 768px) {
  .boxDiv,
  .form-container {
    width: 90%;
    padding: 15px;
  }

  .boxDiv {
    grid-template-columns: 1fr;
    grid-template-rows: repeat(6, auto);
    gap: 20px;
  }

  .innerBox {
    min-height: 150px;
  }

  .form-item {
    flex-direction: column;
    align-items: flex-start;
  }

  label {
    width: 100%;
    text-align: left;
    margin-bottom: 8px;
  }

  input,
  select {
    width: 100%;
  }

  button {
    width: 100%;
    margin-top: 20px;
    padding: 12px 20px;
  }

  .introduce-input {
    width: 100%;
  }
}

.introduce-input {
  flex: 1;
  padding: 12px 15px;
  border: 2px solid #ddd;
  border-radius: 6px;
  font-size: 16px;
  transition: all 0.3s ease;
  width: 100%;
  height: 120px;
  min-height: 120px;
  max-height: 200px;
  box-sizing: border-box;
  resize: vertical;
  font-family: inherit;
  line-height: 1.5;
}

.introduce-input:focus {
  border-color: #4caf50;
  box-shadow: 0 0 8px rgba(76, 175, 80, 0.2);
  outline: none;
}

.introduce-input::-webkit-scrollbar {
  width: 8px;
}

.introduce-input::-webkit-scrollbar-track {
  background: #f1f1f1;
  border-radius: 4px;
}

.introduce-input::-webkit-scrollbar-thumb {
  background: #888;
  border-radius: 4px;
}

.introduce-input::-webkit-scrollbar-thumb:hover {
  background: #555;
}
</style>
