import axiosInstance from "./axiosInit.js";

export const sendApply = async (newcomer) =>
  axiosInstance.post("user/newcomers/apply-join", newcomer);

export const refuse = async (id) =>
  axiosInstance.delete("admin/newcomers/" + id);

export const agree = async (id) => axiosInstance.put("admin/newcomers/" + id);

export const getAllNewcomers = async () => axiosInstance.get("admin/newcomers");
