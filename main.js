import { createApp } from "vue";
import App from "./src/App.vue";
import router from "./src/router/router.js";
import ElementPlus from "element-plus";
// 导入 Element Plus 样式
import "element-plus/dist/index.css";
import { createPinia } from "pinia";
import * as ElementPlusIconsVue from "@element-plus/icons-vue";
import axios from "axios";
let app = createApp(App);

for (const [key, component] of Object.entries(ElementPlusIconsVue)) {
  app.component(key, component);
}

app.use(router);
app.use(ElementPlus);
axios.defaults.withCredentials = true;

app.use(createPinia());
app.mount("#app");
