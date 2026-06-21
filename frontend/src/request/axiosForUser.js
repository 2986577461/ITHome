import axiosInstance from "./axiosInit.js";

export const login = async (loginMessage) =>
  axiosInstance.post("user/users/login", loginMessage);

export const getThis = async () => axiosInstance.get("user/users");

export const getAll = async () => axiosInstance.get("admin/users/all");

export const removeBatch = async (studentIds) =>
  axiosInstance.delete("admin/users", { data: studentIds });

export const update = async (studentMessage) =>
  axiosInstance.put("user/users", studentMessage);

// 上传头像
export const uploadAvatar = async (formData) =>
  axiosInstance.post("user/users/avatar", formData, {
    headers: { "Content-Type": "multipart/form-data" },
  });

export const downloadExcel = async () =>
  axiosInstance.get("admin/users/excel", { responseType: "blob" });
