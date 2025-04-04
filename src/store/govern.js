import { defineStore } from "pinia";

export const govern_store = defineStore("govern", {
  state: () => ({
    addStudentVisible: false,
    updateStudentVisible: false,
    newcomers: [],
    member: [],
    updateStudent: {},
    oldStudentId: "",
  }),

  actions: {
    turnUpdateSwitch() {
      this.updateStudentVisible = !this.updateStudentVisible;
    },
    turnaddStudentSwitch() {
      this.addStudentVisible = !this.addStudentVisible;
    },
    displayStudentCount() {
      return this.newcomers.length > 0;
    },
  },
});
