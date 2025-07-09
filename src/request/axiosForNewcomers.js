import axiosInstance from "@/request/axiosInit.js";

export const sendApply = async (newcomer) => {
    return await axiosInstance.post("user/newcomers/apply-join", newcomer);
};

export const refuse = async (id) => {
    return await axiosInstance.delete("admin/newcomers/" + id);
};

export const agree = async (id) => {
    return await axiosInstance.put("admin/newcomers/" + id);
};

export const getAllNewcomers = async () => {
    return await axiosInstance.get("admin/newcomers");
};