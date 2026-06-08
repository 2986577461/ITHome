import { defineStore } from "pinia";
import { useUserStore } from "./user";

export const useVisibleStore = defineStore("visible", {
  state: () => ({
    visible: false,
    resetPasswordVisible: false,
  }),
  getters: {
    getText() {
      const userStore = useUserStore();
      return userStore.condition ? "注销" : "登录";
    },
  },
  actions: {
    onVisible() {
      this.visible = true;
    },
    offVisible() {
      this.visible = false;
    },
    loginOrLogoutButton() {
      const userStore = useUserStore();
      if (userStore.condition) {
        localStorage.removeItem("authorization");
        location.reload();
      } else {
        this.onVisible();
      }
    },
  },
});
