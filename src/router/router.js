import { createRouter, createWebHashHistory } from "vue-router";

const router = createRouter({
  history: createWebHashHistory(),
  routes: [
    {
      path: "/",
      redirect: "/home",
    },
    {
      path: "/ai-dialog",
      component: () => import("@/views/ai-dialog/ai-dialog.vue"),
    },
    {
      path: "/home",
      component: () => import("@/views/home/home.vue"),
    },
    {
      path: "/tech-study",
      component: () => import("@/views/tech-study/tech-study.vue"),
    },
    {
      path: "/harvest",
      component: () => import("@/views/harvest/harvest.vue"),
    },
    {
      path: "/leraningResource",
      component: () =>
        import("@/views/learning-resource/learning-resource.vue"),
    },
    {
      path: "/join-us",
      component: () => import("@/views/join-us/join-us.vue"),
    },
    {
      path: "/colonyGovern",
      component: () => import("@/views/colonyGovern/colonyGovern.vue"),
    },

    {
      path: "/uploadArticle",
      component: () => import("@/components/uploadArticle.vue"),
    },
  ],
});

export default router;
