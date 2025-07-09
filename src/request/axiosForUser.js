import axiosInstance from "@/request/axiosInit.js";

export const login = async (loginMessage) => {
    return await axiosInstance.post("user/users/login", loginMessage);
};

//获取当前用户
export const getThis = async () => {
    return await axiosInstance.get("user/users");
};

export const getAll = async () => {
    return await axiosInstance.get("admin/users/all");
};

export const removeBatch = async (ids) => {
    return await axiosInstance.delete("admin/users", {
        data: ids
    });
};

export const update = async (studentMessage) => {
    return await axiosInstance.put("admin/users", studentMessage);
};

export const updatePassword = async (resetPassword) => {
    return await axiosInstance.patch("user/users", resetPassword);
};

//聊天获取用户列表
export const getUserList = async () => {
    return await axiosInstance.get("user/chat");
};

export const downloadExcel = async () => {
    return await axiosInstance.get("admin/users/excel", {
        responseType: "blob",
    });
}
