import request from "./axiosInit.js";

// 点赞/取消点赞文章
export function toggleLike(articleId) {
  return request.post(`/user/likes/toggle`, { articleId });
}