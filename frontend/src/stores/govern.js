import { defineStore } from "pinia";

export const useGovernStore = defineStore("govern", {
  state: () => ({
    addStudentVisible: false,
    updateStudentVisible: false,
    newcomers: [],
    member: [],
    updateStudent: {},
    oldStudentId: "",
  }),
  actions: {
    toggleUpdate() {
      this.updateStudentVisible = !this.updateStudentVisible;
    },
    toggleAdd() {
      this.addStudentVisible = !this.addStudentVisible;
    },
  },
});