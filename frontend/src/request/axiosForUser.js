import axiosInstance from "./axiosInit.js";

export const login = async (loginMessage) =>
  axiosInstance.post("user/users/login", loginMessage);

export const getThis = async () => axiosInstance.get("user/users");

export const getAll = async () => axiosInstance.get("admin/users/all");

export const removeBatch = async (ids) =>
  axiosInstance.delete("admin/users", { data: ids });

export const update = async (studentMessage) =>
  axiosInstance.put("admin/users", studentMessage);

export const updatePassword = async (resetPassword) =>
  axiosInstance.put("user/users", { password: resetPassword });

// 上传头像
export function uploadAvatar(formData) {
  return axiosInstance.post("user/users/avatar", formData, {
    headers: { "Content-Type": "multipart/form-data" },
  });
}

export const downloadExcel = async () =>
  axiosInstance.get("admin/users/excel", { responseType: "blob" });