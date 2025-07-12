import axiosInstance from "@/request/axiosInit.js";

export const getResourcesCount = async () => {
    return await axiosInstance.get("admin/resources/count");
};
export const getAll = async () => {
    return await axiosInstance.get("user/resources/all");
};

export const uploadResource = async (introduce) => {
    return await axiosInstance.post("user/resources", introduce, {
        headers: {
            "Content-Type": "multipart/form-data", // 确保是 multipart/form-data 格式
        },
    });
};

export const getDownloadUrl = async (object) => {
    return await axiosInstance.get("user/common/url", {
        params: {objectName: object},
    });
};