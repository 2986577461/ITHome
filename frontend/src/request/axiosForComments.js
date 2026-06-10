import request from "./axiosInit.js";



// 发表评论
export function addComment(data) {
  return request.post("/user/comments", data);
}


// 回复评论
export function replyComment(data) {
  return request.post("/user/comments/reply", data);
}

// 获取评论回复列表
export function getReplies(commentId) {
  return request.get(`/user/comments/${commentId}/replies`);
}