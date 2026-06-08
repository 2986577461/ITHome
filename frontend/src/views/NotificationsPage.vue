<template>
  <div class="notif-page">
    <div class="page-hero hero-accent-purple" style="background: linear-gradient(180deg, #f0f0ff 0%, #f5f5f7 100%);">
      <div class="hero-bg-mesh"></div>
      <div class="section-inner">
        <div class="pill">消息通知</div>
        <h1>通知中心</h1>
        <p>查看系统通知与互动消息</p>
      </div>
    </div>

    <section class="section">
      <div class="section-inner">
        <div class="notif-bar">
          <span class="notif-summary">共 {{ notifStore.list.length }} 条通知</span>
          <button v-if="notifStore.unreadCount > 0" class="btn-secondary btn-sm" @click="markAllRead">全部标为已读</button>
        </div>

        <div class="notif-list">
          <template v-if="loading">
            <div class="skeleton-card" v-for="n in 4" :key="n">
              <div class="skeleton-title"></div>
              <div class="skeleton-text"></div>
              <div class="skeleton-text"></div>
            </div>
          </template>
          <template v-else>
          <div
            v-for="n in notifStore.list"
            :key="n.id"
            class="notif-item"
            :class="{ unread: !n.read }"
            @click="markRead(n.id)"
          >
            <div class="notif-icon" :class="n.type">
              <svg v-if="n.type === 'like'" width="18" height="18" viewBox="0 0 18 18" fill="none"><path d="M9 4.5c-1.5-1.5-4-1.5-4 1.5s4 4.5 4 4.5 4-3 4-4.5-2.5-3-4-1.5z" stroke="currentColor" stroke-width="1.4" stroke-linecap="round"/></svg>
              <svg v-else-if="n.type === 'comment'" width="18" height="18" viewBox="0 0 18 18" fill="none"><path d="M3 3h12a1 1 0 011 1v9a1 1 0 01-1 1H8l-3 2v-2H3a1 1 0 01-1-1V4a1 1 0 011-1z" stroke="currentColor" stroke-width="1.4" stroke-linecap="round"/></svg>
              <svg v-else width="18" height="18" viewBox="0 0 18 18" fill="none"><circle cx="9" cy="9" r="7" stroke="currentColor" stroke-width="1.4"/><path d="M9 5v4l3 2" stroke="currentColor" stroke-width="1.4" stroke-linecap="round"/></svg>
            </div>
            <div class="notif-body">
              <div class="notif-text">{{ n.content }}</div>
              <div class="notif-time">{{ n.time }}</div>
            </div>
            <div v-if="!n.read" class="notif-dot"></div>
          </div>

          <div v-if="notifStore.list.length === 0" class="empty-notif">
            <svg width="48" height="48" viewBox="0 0 48 48" fill="none"><path d="M24 6a9 9 0 00-9 9v6l-3 6h24l-3-6v-6a9 9 0 00-9-9z" stroke="currentColor" stroke-width="1.5" opacity=".3" stroke-linecap="round"/><path d="M20 30a4 4 0 008 0" stroke="currentColor" stroke-width="1.5" opacity=".3" stroke-linecap="round"/></svg>
            <p>暂无通知</p>
          </div>
          </template>
        </div>
      </div>
    </section>
  </div>
</template>

<script setup>
import { ref, onMounted } from "vue";
import { useNotificationStore } from "@/stores/notification";
import { getNotifications, markNotificationRead, markAllNotificationsRead } from "@/request/axiosForNotifications.js";
import { ElMessage } from "element-plus";

const notifStore = useNotificationStore();
const loading = ref(true);

function markRead(id) {
  notifStore.markRead(id);
  markNotificationRead(id).catch(() => {});
}

async function markAllRead() {
  notifStore.markAllRead();
  try { await markAllNotificationsRead(); ElMessage.success("已全部标为已读"); } catch {}
}

onMounted(async () => {
  try {
    const resp = await getNotifications();
    if (resp?.data) notifStore.setNotifications(resp.data);
  } catch {} finally {
    loading.value = false;
  }
});
</script>

<style scoped>
.notif-page { padding-top: var(--nav-height); }
.pill {
  display: inline-flex; font-size: 12px; font-weight: 600; color: #5856d6;
  background: rgba(88,86,214,.08); padding: 6px 16px; border-radius: 980px; margin-bottom: 16px;
}

.notif-bar {
  display: flex; justify-content: space-between; align-items: center;
  margin-bottom: 20px;
}
.notif-summary { font-size: 14px; color: var(--color-text-secondary); }

.notif-list { max-width: 680px; margin: 0 auto; }
.notif-item {
  display: flex; align-items: center; gap: 14px;
  padding: 16px 18px; background: var(--color-surface);
  border-radius: var(--radius-sm); border: 1px solid var(--color-border);
  margin-bottom: 10px; cursor: pointer; transition: all .15s;
}
.notif-item:hover { background: var(--color-bg); }
.notif-item.unread { border-left: 3px solid var(--color-accent); }

.notif-icon {
  width: 36px; height: 36px; border-radius: 50%;
  display: flex; align-items: center; justify-content: center;
  flex-shrink: 0;
}
.notif-icon.like { background: #fff0f0; color: #ff3b30; }
.notif-icon.comment { background: #f0f4ff; color: var(--color-accent); }
.notif-icon.system { background: #f5f0ff; color: #5856d6; }

.notif-body { flex: 1; }
.notif-text { font-size: 14px; line-height: 1.5; }
.notif-time { font-size: 12px; color: var(--color-text-tertiary); margin-top: 3px; }
.notif-dot {
  width: 8px; height: 8px; border-radius: 50%;
  background: var(--color-accent); flex-shrink: 0;
}

.empty-notif { text-align: center; padding: 80px 20px; color: var(--color-text-tertiary); }
.empty-notif p { margin-top: 12px; }
</style>
