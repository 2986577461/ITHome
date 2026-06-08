import request from "./axiosInit.js";

// 获取通知列表
export function getNotifications() {
  return request.get("/user/notifications");
}

// 标记通知为已读
export function markNotificationRead(id) {
  return request.put(`/user/notifications/${id}/read`);
}

// 标记所有通知为已读
export function markAllNotificationsRead() {
  return request.put("/user/notifications/read-all");
}

// 获取未读通知数
export function getUnreadCount() {
  return request.get("/user/notifications/unread-count");
}
