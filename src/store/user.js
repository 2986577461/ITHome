import { defineStore } from "pinia";
export const user_store = defineStore("user", {
  state: () => ({
    id: "",
    studentId: "",
    name: "",
    condition: false,
    position: "",
    academy: "",
  }),

  actions: {
    clear() {
      this.password = "";
      this.studentId = null;
      this.name = "";
      this.condition = false;
      this.academy = "";
      this.position = "";
    },

    loginsuccess(id, studentId, name, position, academy) {
      this.id = id;
      this.studentId = studentId;
      this.condition = true;
      this.name = name;
      this.position = position;
      this.academy = academy;
    },
  },
});
