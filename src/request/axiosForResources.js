import axiosInstance from "@/request/axiosInit.js";

export const getResourcesCount = async () => {
    return  await axiosInstance.get("admin/resources/count");
};
export const getAll = async () => {
    return  await axiosInstance.get("user/resources/all");
};
export const save = async (formData) => {
    return  await axiosInstance.post("user/resources/upload", formData, {
        headers: {
            "Content-Type": "multipart/form-data", // 确保是 multipart/form-data 格式
        },
    });
};
export const saveIntroduce = async (introduce) => {
    return  await axiosInstance.post("user/resources", introduce);
};

export const getDownloadUrl = async (object, name) => {
    return await axiosInstance.get("user/resources/url", {
        params: { objectName: object, friendlyName: name },
    });
};