import axiosInstance from "@/request/axiosInit.js";

export const getUserList=async ()=>{
    return  await axiosInstance.get("user/chat/all");
}