import axios from "axios";
// export const baseURL = "localhost:8080";
export const baseURL = "47.108.61.239";
const axiosInstance = axios.create({
  //部署到云服务器需要修改为其公网地址
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
// 响应拦截器
axiosInstance.interceptors.response.use((response) => {
  const res = response.data;
  return res;
});

// 请求拦截器：自动添加Authorization头
axiosInstance.interceptors.request.use((config) => {
  const token = localStorage.getItem("token");
  if (token != null) {
    config.headers.Authorization = token;
  }
  return config;
});

export default axiosInstance;
