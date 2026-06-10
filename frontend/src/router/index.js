import { createRouter, createWebHashHistory } from "vue-router";
import { useUserStore } from "@/stores/user.js";

const router = createRouter({
  history: createWebHashHistory(),
  routes: [
    { path: "/", redirect: "/home" },
    {
      path: "/home",
      name: "home",
      component: () => import("@/views/HomePage.vue"),
    },
    {
      path: "/about",
      name: "about",
      component: () => import("@/views/AboutPage.vue"),
    },
    {
      path: "/tech-study",
      name: "tech-study",
      component: () => import("@/views/TechStudy.vue"),
    },
    {
      path: "/learning-resource",
      name: "learning-resource",
      component: () => import("@/views/LearningResource.vue"),
    },
    {
      path: "/ai-dialog",
      name: "ai-dialog",
      component: () => import("@/views/AiDialog.vue"),
      meta: { hideFooter: true },
    },
    {
      path: "/harvest",
      name: "harvest",
      component: () => import("@/views/Harvest.vue"),
    },
    {
      path: "/join-us",
      name: "join-us",
      component: () => import("@/views/JoinUs.vue"),
    },
    {
      path: "/colony-govern",
      name: "colony-govern",
      component: () => import("@/views/ColonyGovern.vue"),
      meta: { requiresAuth: true, requiresRole: ["会长", "副会长"] },
    },
    {
      path: "/upload-article",
      name: "upload-article",
      component: () => import("@/views/UploadArticle.vue"),
    },
    {
      path: "/profile",
      name: "profile",
      component: () => import("@/views/ProfilePage.vue"),
      meta: { requiresAuth: true },
    },
    {
      path: "/notifications",
      name: "notifications",
      component: () => import("@/views/NotificationsPage.vue"),
      meta: { requiresAuth: true },
    },
  ],
});

router.beforeEach((to, from, next) => {
  if (to.meta.requiresAuth) {
    const token = localStorage.getItem("authorization");
    if (token) {
      const userStore = useUserStore();
      if (
        to.meta.requiresRole &&
        !to.meta.requiresRole.includes(userStore.position)
      ) {
        next({ path: "/home" });
      } else {
        next();
      }
    } else {
      next({ path: "/home" });
    }
  } else {
    next();
  }
});

export default router;