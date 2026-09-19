import axiosInstance from "./axiosInit.js";

export const getResourcesCount = async () =>
  axiosInstance.get("user/resources/count");

export const getAll = async () => axiosInstance.get("user/resources/all");

export const getMyResources = async () => axiosInstance.get("user/resources/my");

export const uploadResource = async (formData) =>
  axiosInstance.post("user/resources", formData, {
    headers: { "Content-Type": "multipart/form-data" },
  });

export const deleteById = async (id) =>
  axiosInstance.delete("user/resources/" + id);

// 返回一条能直接塞进 <a href> 的地址：
// 文件在 OSS 上是预签名 URL，还在后端本机是 /user/common/local/xxx。
// 后端会按文件当前在哪自动选，前端不用区分。
export const getDownloadUrl = async (object) =>
  axiosInstance.get("user/common/url", { params: { objectName: object } });
