import { defineStore } from "pinia";

const AVATARS = [
  "https://api.dicebear.com/7.x/adventurer-neutral/svg?seed=",
  "https://api.dicebear.com/7.x/bottts-neutral/svg?seed=",
  "https://api.dicebear.com/7.x/fun-emoji/svg?seed=",
  "https://api.dicebear.com/7.x/notionists-neutral/svg?seed=",
];

function randomAvatar(name) {
  const style = AVATARS[Math.floor(Math.random() * AVATARS.length)];
  return style + encodeURIComponent(name || "user");
}

export const useUserStore = defineStore("user", {
  state: () => ({
    id: "",
    studentId: "",
    name: "",
    condition: false,
    position: "",
    academy: "",
    sex: "",
    major: "",
    className: "",
    avatar: null,
  }),
  getters: {
    effectiveAvatar(state) {
      return state.avatar;
    },
  },
  actions: {
    clear() {
      this.id = "";
      this.studentId = "";
      this.name = "";
      this.condition = false;
      this.position = "";
      this.academy = "";
      this.sex = "";
      this.major = "";
      this.className = "";
      this.avatar = null;
    },
    setAvatar(url) {
      this.avatar = url;
    },
    setUser(data) {
      if (data.id !== undefined) this.id = data.id;
      if (data.studentId !== undefined) this.studentId = data.studentId;
      if (data.name !== undefined) this.name = data.name;
      if (data.position !== undefined) this.position = data.position;
      if (data.academy !== undefined) this.academy = data.academy;
      if (data.sex !== undefined) this.sex = data.sex;
      if (data.major !== undefined) this.major = data.major;
      if (data.className !== undefined) this.className = data.className;
      if (data.avatar !== undefined) this.avatar = data.avatar;
      this.condition = true;
    },
    getRandomAvatar() {
      return randomAvatar(this.name);
    },
  },
});
