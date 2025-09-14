import axios from "axios";
import { ElMessage } from "element-plus";

export const baseHost = "localhost:8080/";
export const baseURL = "localhost:8080/ithome";
const axiosInstance = axios.create({
  baseURL: "http://" + baseURL,
  // 设置请求超时时间60秒
  timeout: 60000,
  headers: {
    // 默认以json格式传递数据
    "Content-Type": "application/json",
  },
  //允许跨域携带cookie
  withCredentials: true,
});
// // 响应拦截器
axiosInstance.interceptors.response.use(
  (response) => {
    return response.data;
  },
  (error) => {
    if (error.response.status === 401) {
      const token = localStorage.getItem("token");
      if (token != null) {
        ElMessage.error("身份验证失败，请重新登录!");
        localStorage.removeItem("token");
      } else {
        ElMessage.error("请登录后再尝试!");
      }
      localStorage.removeItem("token");

      setTimeout(() => {
        location.reload();
      }, 1000);
    }
    return Promise.reject(error);
  }
);

// 请求拦截器：自动添加Authorization头
axiosInstance.interceptors.request.use((config) => {
  const token = localStorage.getItem("token");
  if (token != null) {
    config.headers.Authorization = token;
  }
  return config;
});

export default axiosInstance;
