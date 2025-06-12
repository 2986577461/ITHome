import { defineStore } from "pinia";
import { user_store } from "./user";

export const visible_store = defineStore("change", {
  state: () => ({
    visible: false,
    resetPasswordVisible: false,
  }),
  getters: {
    getText() {
      const userStore = user_store();
      return userStore.condition ? "注销" : "登陆";
    },
  },
  actions: {
    onvisible() {
      this.visible = true;
    },
    offvisible() {
      this.visible = false;
    },
    async loginOrLogoutButton() {
      const userStore = user_store();
      if (userStore.condition) {
        localStorage.removeItem("token");
        location.reload();
      } else {
        this.onvisible();
      }
    },
  },
});
