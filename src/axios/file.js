import { ElMessage } from "element-plus";
import axiosInstance from "./axiosInit";

export const downloadFile = async (id) => {
  const name = await axiosInstance.get("resources/" + id).catch((error) => {
    if (error.response.status == 401) {
      ElMessage.error("请先登录");
      return;
    }
  });
  if (name != null) {
    await axiosInstance({
      url: `resources/download/${id}`,
      method: "GET",
      responseType: "blob", // 重要，告诉axios这是一个二进制文件
    })
      .then((response) => {
        const blob = response.data;
        // 创建下载链接
        const link = document.createElement("a");
        const url = window.URL.createObjectURL(blob); // 创建一个URL指向Blob对象
        link.href = url;
        link.setAttribute("download", name.data); // 设置下载文件的名称
        document.body.appendChild(link);
        link.click();
        document.body.removeChild(link); // 下载完成后移除临时元素
        window.URL.revokeObjectURL(url); // 释放内存
      })
      .catch((error) => {
        ElMessage.error("文件不存在");
      });
  }
};

export const fetchResources = async () => {
  const resp = await axiosInstance.get("user/resources");
  console.log(resp)
  return resp.data;
};

export const uploadResource = async (formData) => {
  const resp = await axiosInstance.post("user/resources/upload", formData, {
    headers: {
      "Content-Type": "multipart/form-data", // 确保是 multipart/form-data 格式
    },
  });
  return resp.data;
};

export const getnewcomers = async () => {
  const resp = await axiosInstance.post("admin/newcomers");
  return resp.data;
};
