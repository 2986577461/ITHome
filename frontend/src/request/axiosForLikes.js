import request from "./axiosInit.js";

// 点赞/取消点赞文章
export function toggleLike(articleId) {
  return request.post("/user/likes/toggle", { articleId });
}

// 点赞/取消点赞评论
export function toggleCommentLike(commentId) {
  return request.post("/user/likes/comments/toggle", { commentId });
}

// 批量拿文章点赞数、是否已赞、评论数。articleIds 保持字符串，避免长整型丢精度
export function getLikeSummary(articleIds) {
  return request.get("/user/likes/summary", {
    params: { articleIds: (articleIds || []).join(",") },
  });
}
