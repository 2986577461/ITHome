import axios from "axios";

const axiosInstance = axios.create({
  //部署到云服务器需要修改为其公网地址
  // 例： baseURL: "http://1.14.65.37:8080/",
  baseURL: "http://localhost:8080/",
  // 设置请求超时时间60秒
  timeout: 60000,
  headers: {
    // 默认以json格式传递数据
    "Content-Type": "application/json",
  },
  //允许跨域携带cookie
  withCredentials: true,
});

export default axiosInstance;
