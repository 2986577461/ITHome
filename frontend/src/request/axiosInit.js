import axios from "axios";
import { ElMessage } from "element-plus";

// 生产：空 baseURL，请求走同域 Nginx 路由
// 开发：空 baseURL，请求走 Vite proxy（vite.config.js）
//    /user/* /admin/* → localhost:8080 (Java)
//    /api/* /chat-stream → localhost:8000 (Python)
const axiosInstance = axios.create({
  baseURL: "",
  timeout: 60000,
  headers: { "Content-Type": "application/json" },
  withCredentials: true,
});

axiosInstance.interceptors.response.use(
  (response) => {
    var body = response.data;
    if (body && body.code != null && Number(body.code) !== 200) {
      ElMessage.error(body.msg);
    }
    return body;
  },
  (error) => {
    if (error.response?.status === 500) {
      ElMessage.error("服务器内部出错！");
    } else if (error.response?.status === 401) {
      const token = localStorage.getItem("authorization");
      ElMessage.error(token ? "身份验证失败，请重新登录!" : "请登录后再尝试!");
    } else if (error.response?.status === 503) {
      ElMessage.error("访问频率过高，请稍后尝试！");
    }
  },
);

axiosInstance.interceptors.request.use((config) => {
  const token = localStorage.getItem("authorization");
  if (token) config.headers.Authorization = token;
  return config;
});

export default axiosInstance;
