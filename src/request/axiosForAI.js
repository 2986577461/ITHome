import axiosInstance from "@/request/axiosInit.js";

export const saveAnswer = async (message) => {
    return  await axiosInstance.post("/user/ai-dialog/assistant-answer",message)
};

