import axiosInstance from "./axiosInit.js";

export const getResourcesCount = async () =>
  axiosInstance.get("user/resources/count");

export const getAll = async () => axiosInstance.get("user/resources/all");

export const uploadResource = async (formData) =>
  axiosInstance.post("user/resources", formData, {
    headers: { "Content-Type": "multipart/form-data" },
  });

export const deleteById = async (id) =>
  axiosInstance.delete("user/resources/" + id);

export const getDownloadUrl = async (object) =>
  axiosInstance.get("user/common/url", { params: { objectName: object } });
