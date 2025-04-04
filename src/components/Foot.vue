<script setup></script>

<template>
  <div class="foot">
    <div class="copyright">&copy;绵阳职业技术学院电子与信息学院IT之家协会</div>
    <div class="beian">
      <a href="https://beian.miit.gov.cn/" target="_blank">
        蜀ICP备2024116038号
      </a>
    </div>
  </div>
</template>

<style scoped>
.foot {
  background-color: rgb(50, 129, 194);
  color: white;
  font-family: "微软雅黑";
  font-size: 16px;
  text-align: center;
  width: 100%;
  height: 15vh;
}

.copyright {
  font-size: 18px;
}

.beian {
  font-size: 14px;
}

.beian a {
  color: white;
  text-decoration: none;
  transition: opacity 0.3s;
}

.beian a:hover {
  opacity: 0.8;
}
</style>
