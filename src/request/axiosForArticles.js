import axiosInstance from "@/request/axiosInit.js";

export const getAll = async () => {
    return  await axiosInstance.get("user/articles/all");
};

export const getArticleCount = async () => {
    return  await axiosInstance.get("admin/articles");
};

export const upload = async (article) => {
    return  await axiosInstance.post("user/articles", article);
};
export const update = async (article) => {
    return  await axiosInstance.put("user/articles", article);
};

export const deleteById = async (id) => {
    return  await axiosInstance.delete("user/articles/" + id);
};