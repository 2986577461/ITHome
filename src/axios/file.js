import { ElMessage } from "element-plus";
import axiosInstance from "./axiosInit";

export const fetchResources = async () => {
  const resp = await axiosInstance.get("user/resources/all");
  return resp.data;
};

export const uploadFile = async (formData) => {
  const resp = await axiosInstance.post("user/resources/upload", formData, {
    headers: {
      "Content-Type": "multipart/form-data", // 确保是 multipart/form-data 格式
    },
  });
  return resp.data;
};
export const uploadResource = async (resource) => {
  const resp = await axiosInstance.post("user/resources", resource);
  return resp.data;
};

export const getnewcomers = async () => {
  const resp = await axiosInstance.post("admin/newcomers");
  return resp.data;
};

export const getUrl = async (object, name) => {
  const resp = await axiosInstance.get("user/resources/url", {
    params: { objectName: object, friendlyName: name },
  });
  return resp.data;
};
