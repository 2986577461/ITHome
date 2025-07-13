import axios from "axios";

export const baseHost = "www.myzyitzj.cn";
export const baseURL = "www.myzyitzj.cn/ithome"
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
axiosInstance.interceptors.response.use((response) => {
    return response.data;
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
