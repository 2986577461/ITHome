import axiosInstance from "./axiosInit";

export const login = async (loginMessage) => {
    const resp = await axiosInstance.post("user/users/login", loginMessage);
    return resp;
};

export const getCurrentUser = async () => {
    const resp = await axiosInstance.get("admin/users");
    return resp;
};

export const selectAllMenber = async () => {
    const resp = await axiosInstance.get("admin/users");
    return resp.data;
};

export const removeStudents = async (students) => {
    //传递数组用data
    const resp = await axiosInstance.delete("admin/users", {data: students});
    return resp.data;
};
export const selectArticleCount = async () => {
    const resp = await axiosInstance.get("admin/articles");
    return resp.data;
};

export const applyJoin = async (newcomer) => {
    const resp = await axiosInstance.post("user/newcomers/applyJoin", newcomer);
    return resp.data;
};

export const refuseNewcomer = async (id) => {
    const resp = await axiosInstance.delete("admin/newcomers/" + id);
    return resp.data;
};

export const agreeNewcomer = async (id) => {
    const resp = await axiosInstance.put("admin/newcomers/" + id);
    return resp.data;
};

export const getAllArticle = async () => {
    const resp = await axiosInstance.get("user/articles/all");
    return resp.data;
};

export const updateStudentForAxios = async (id, studentMessage) => {
    const resp = await axiosInstance.put("admin/users/" + id, studentMessage);
    return resp.data;
};

export const updatePassword = async (id, resetPassword) => {
    const resp = await axiosInstance.patch("user/users/" + id, resetPassword);
    return resp.data;
};

export const selectResourceCount = async () => {
    const resp = await axiosInstance.get("admin/resources/count");
    return resp.data;
};

export const uploadArticle = async (article) => {
    const resp = await axiosInstance.post("user/articles", article);
    return resp;
};
export const updateArticle = async (article) => {
    const resp = await axiosInstance.put("user/articles", article);
    return resp;
};

export const deleteArticle = async (id) => {
    const resp = await axiosInstance.delete("user/articles/" + id);
    return resp.data;
};

export const aiDialog = async (userMessage) => {
    const response = await axiosInstance.post("user/ai-dialog", null,{
      params:{userMessage}
    });
    return response.data;
};

export const getUserList = async () => {
    const response = await axiosInstance.get("user/chat");
    return response.data;
};
