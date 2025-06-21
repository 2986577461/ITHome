import { defineStore } from "pinia";
export const user_store = defineStore("user", {
  state: () => ({
    studentID: null,
    name: "",
    condition: false,
    position: "",
    academy: "",
  }),

  actions: {
    clear() {
      this.password = "";
      this.studentID = null;
      this.name = "";
      this.condition = false;
      this.academy = "";
      this.position = "";
    },

    loginsuccess(ID, name, position, academy) {
      this.studentID = ID;
      this.condition = true;
      this.name = name;
      this.position = position;
      this.academy = academy;
    },
  },
});
