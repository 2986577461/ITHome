import axiosInstance from "./axiosInit";

export const login = async (loginMessage) => {
  const resp = await axiosInstance.post("users/login", loginMessage);
  return resp;
};

export const getCurrentUser = async () => {
  const resp = await axiosInstance.get("users");
  return resp;
};

export const selectAllMenber = async () => {
  const resp = await axiosInstance.get("users/all");
  return resp.data;
};

export const removeStudents = async (students) => {
  //传递数组用data
  const resp = await axiosInstance.delete("users", { data: students });
  return resp.data;
};
export const selectArticleCount = async () => {
  const resp = await axiosInstance.get("articles");
  return resp.data;
};

export const applyJoin = async (newcomer) => {
  const resp = await axiosInstance.post("newcomers/applyJoin", newcomer);
  return resp.data;
};

export const refuseNewcomer = async (id) => {
  const resp = await axiosInstance.delete("newcomers/" + id);
  return resp.data;
};

export const agreeNewcomer = async (id) => {
  const resp = await axiosInstance.put("newcomers/" + id);
  return resp.data;
};

export const getAllArticle = async () => {
  const resp = await axiosInstance.get("articles/all");
  return resp.data;
};

export const updateStudentForAxios = async (id, studentMessage) => {
  const resp = await axiosInstance.put("users/" + id, studentMessage);
  return resp.data;
};

export const updatePassword = async (id, resetPassword) => {
  const resp = await axiosInstance.patch("users/" + id, resetPassword);
  return resp.data;
};

export const selectResourceCount = async () => {
  const resp = await axiosInstance.get("resources/count");
  return resp.data;
};

export const uploadArticle = async (article) => {
  const resp = await axiosInstance.post("articles", article);
  return resp;
};
export const updateArticle = async (article) => {
  const resp = await axiosInstance.put("articles", article);
  return resp;
};

export const deleteArticle = async (id) => {
  const resp = await axiosInstance.delete("articles/" + id);
  return resp.data;
};

export const aiDialog = async (userMessage) => {
  const response = await axiosInstance.post("aiDialog", userMessage);
  return response.data;
};

export const getUserList = async () => {
  const response = await axiosInstance.get("chat");
  return response.data;
};
