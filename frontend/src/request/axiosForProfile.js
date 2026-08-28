import request from "./axiosInit.js";

// 更新用户信息
export function updateProfile(data) {
  return request.put("/user/users/profile", data);
}

// 分页获取我的文章
export function getMyArticlesPage(page, size) {
  return request.get("/user/articles/my-page", { params: { page, size } });
}

// 获取我的资料列表
export function getMyResources() {
  return request.get("/user/resources/all", {
    params: { my: true },
  });
}
