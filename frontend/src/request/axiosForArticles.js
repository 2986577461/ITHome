import axiosInstance from "./axiosInit.js";

export const getPage = async (page, size, type) => {
  const params = { page, size };
  if (type) params.type = type;
  return axiosInstance.get("user/articles/page", { params });
};

export const getArticleCount = async (type) => {
  const params = {};
  if (type) params.type = type;
  return axiosInstance.get("user/articles/count", { params });
};

export const upload = async (article) =>
  axiosInstance.post("user/articles", article);

export const update = async (article) =>
  axiosInstance.put("user/articles", article);

export const uploadArticleImageBatch = (formData) =>
  axiosInstance.post("user/articles/image", formData, {
    headers: { "Content-Type": "multipart/form-data" },
  });

export const deleteArticleImageBatch = (ids) =>
  axiosInstance.delete("user/articles/image", { data: ids });

export const deleteById = async (id) =>
  axiosInstance.delete("user/articles/" + id);
