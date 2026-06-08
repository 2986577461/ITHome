import { defineStore } from "pinia";

export const useNotificationStore = defineStore("notification", {
  state: () => ({
    list: [],
    unreadCount: 0,
  }),
  actions: {
    setNotifications(list) {
      this.list = list;
      this.unreadCount = list.filter((n) => !n.read).length;
    },
    markRead(id) {
      const n = this.list.find((x) => x.id === id);
      if (n) n.read = true;
      this.unreadCount = this.list.filter((n) => !n.read).length;
    },
    markAllRead() {
      this.list.forEach((n) => (n.read = true));
      this.unreadCount = 0;
    },
  },
});
