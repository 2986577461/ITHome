import request from "./axiosInit.js";

// 点赞/取消点赞文章
export function toggleLike(articleId) {
  return request.post(`/user/likes/toggle`, { articleId });
}

// 获取文章点赞状态
export function getLikeStatus(articleId) {
  return request.get(`/user/likes/status`, { params: { articleId } });
}

// 获取文章点赞数
export function getLikeCount(articleId) {
  return request.get(`/user/likes/count`, { params: { articleId } });
}
