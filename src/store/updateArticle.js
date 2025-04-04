import { uploadArticle } from "@/axios/axios";
import { defineStore } from "pinia";

export const article_store = defineStore("article_store", {
  state: () => ({
    id: 0,
    head: "",
    content: "",
    type: "",
  }),
  getters: {
    getText() {
      return this.id == 0 ? "发布文章" : "保存文章";
    },
  },
  actions: {
    setArticle(id, head, content, type) {
      this.id = id;
      this.head = head;
      this.content = content;
      this.type = type;
    },
  },
});
