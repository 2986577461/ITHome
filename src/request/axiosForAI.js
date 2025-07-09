import axiosInstance from "@/request/axiosInit.js";

//todo如果可以实现的话请在AI对话页面实现文字一点一点加载的效果
export const sendMessage = async (message) => {
    return  await axiosInstance.post("user/ai",message)
};
