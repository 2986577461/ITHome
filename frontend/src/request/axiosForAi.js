import axiosInstance from "./axiosInit.js";

export const getConversations = () => axiosInstance.get("/api/conversations");

export const getMessages = (threadId) =>
  axiosInstance.get(`/api/conversations/${threadId}/messages`);

export const deleteConversation = (threadId) =>
  axiosInstance.delete(`/api/conversations/${threadId}`);

export const getKbDocuments = () => axiosInstance.get("/api/kb/documents");

export const uploadKbDocument = (filename, content_b64) =>
  axiosInstance.post("/api/kb/upload", { filename, content_b64 });

export const deleteKbDocument = (docId) =>
  axiosInstance.delete(`/api/kb/documents/${docId}`);

export const createTask = (question, threadId) =>
  axiosInstance.post("/api/tasks", { question, thread_id: threadId });

export const getTaskState = (taskId) =>
  axiosInstance.get(`/api/tasks/${taskId}`);