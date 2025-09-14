import {createRouter, createWebHashHistory} from "vue-router";
import {ElMessage} from "element-plus";
import {user_store} from "@/store/user.js";

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
        {
            path: "/chat",
            component: () => import("@/views/chat/chat.vue"),
        },
    ],
});

router.beforeEach((to, from, next) => {
    console.log(`导航从 ${from.path} 到 ${to.path}`);

    // 检查目标路由是否需要认证
    if (to.path === '/colonyGovern') {
        const token = localStorage.getItem('token'); // 从本地存储获取 token

        if (token) {
            const userStore = user_store();
            if (userStore.position === '会长' || userStore.position === '副会长')
                next(); // 继续导航
            else {
                next({path: '/'});
            }
        } else {
            next({path: '/'});
        }
    } else {
        next(); // 直接放行
    }
});


export default router;
