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
  (response) => response.data,
  (error) => {
    if (error.response?.status === 401) {
      const token = localStorage.getItem("authorization");
      ElMessage.error(token ? "身份验证失败，请重新登录!" : "请登录后再尝试!");
      localStorage.removeItem("authorization");
    }
    return Promise.reject(error);
  },
);

axiosInstance.interceptors.request.use((config) => {
  const token = localStorage.getItem("authorization");
  if (token) config.headers.Authorization = token;
  return config;
});

export default axiosInstance;
