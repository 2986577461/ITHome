import request from "./axiosInit.js";

// 获取文章评论列表
export function getComments(articleId) {
  return request.get(`/user/comments`, { params: { articleId } });
}

// 发表评论
export function addComment(data) {
  return request.post("/user/comments", data);
}

// 删除评论
export function deleteComment(commentId) {
  return request.delete(`/user/comments/${commentId}`);
}

// 回复评论
export function replyComment(data) {
  return request.post("/user/comments/reply", data);
}

// 获取评论回复列表
export function getReplies(commentId) {
  return request.get(`/user/comments/${commentId}/replies`);
}
