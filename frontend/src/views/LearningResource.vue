<template>
  <div class="resource-page">
    <div class="page-hero hero-accent-blue" style="background: linear-gradient(180deg, #f0f4ff 0%, #f5f5f7 100%);">
      <div class="hero-bg-mesh"></div>
      <div class="section-inner">
        <div class="pill">学习资料</div>
        <h1>资源共享库</h1>
        <p>秉承开放共享的原则，学员可自由上传与下载学习资料</p>
      </div>
    </div>

    <section class="section">
      <div class="section-inner">
        <div class="resource-grid" v-if="loading">
          <div class="skeleton-card" v-for="n in 6" :key="n">
            <div style="width:100%;aspect-ratio:16/10;background:var(--skeleton-base);border-radius:var(--radius-sm)"></div>
            <div class="skeleton-title"></div>
            <div class="skeleton-text"></div>
            <div class="skeleton-text"></div>
          </div>
        </div>
        <div class="resource-grid" v-else-if="pagedResources.length > 0">
          <div v-for="r in pagedResources" :key="r.id" class="res-card">
            <div class="res-cover" @click="download(r.objectName)">
              <img v-if="r.coverUrl" :src="r.coverUrl" class="res-cover-img" />
              <div v-else class="res-cover-placeholder">
                <svg viewBox="0 0 48 48" fill="none">
                  <rect x="6" y="10" width="36" height="28" rx="4" stroke="currentColor" stroke-width="1.5" opacity=".25"/>
                  <path d="M12 34l8-12 6 8 4-5 6 9H12z" stroke="currentColor" stroke-width="1.5" stroke-linecap="round" stroke-linejoin="round" opacity=".25"/>
                </svg>
              </div>
              <div class="res-cover-overlay">
                <svg width="24" height="24" viewBox="0 0 24 24" fill="none"><path d="M12 3v12M7 10l5 5 5-5M5 17h14v3H5z" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"/></svg>
              </div>
            </div>
            <div class="res-body">
              <div class="res-title-row">
                <h3>{{ r.head }}</h3>
                <button v-if="r.studentId === userStore.studentId" class="res-del" @click="confirmDelete(r)" title="删除">
                  <svg width="14" height="14" viewBox="0 0 15 15" fill="none"><path d="M3 4h9M6 4V3a1 1 0 011-1h1a1 1 0 011 1v1M5 4v8a1 1 0 001 1h3a1 1 0 001-1V4" stroke="currentColor" stroke-width="1.3" stroke-linecap="round"/></svg>
                </button>
              </div>
              <p class="res-desc">{{ r.introduce }}</p>
              <div class="res-meta">
                <img v-if="r.avatar" :src="r.avatar" class="res-avatar" />
                <span class="res-author">{{ r.studentName }}</span>
                <span class="res-time">{{ fmtDate(r.releaseDateTime) }}</span>
              </div>
              <button class="res-dl" @click="download(r.objectName)">
                <svg width="16" height="16" viewBox="0 0 16 16" fill="none"><path d="M8 2v8M4 6l4 4 4-4M2 12v2h12v-2" stroke="currentColor" stroke-width="1.5" stroke-linecap="round" stroke-linejoin="round"/></svg>
                下载资料
              </button>
            </div>
          </div>
        </div>
        <div v-else-if="!loading && resources.length === 0" class="empty-state">
          <svg width="48" height="48" viewBox="0 0 48 48" fill="none"><rect x="10" y="6" width="28" height="36" rx="3" stroke="currentColor" stroke-width="1.5" opacity=".3"/><path d="M18 18h12M18 24h10M18 30h8" stroke="currentColor" stroke-width="1.5" stroke-linecap="round" opacity=".3"/></svg>
          <p>暂无资料</p>
        </div>
        <div class="pagination" v-if="totalPages > 1">
          <button :disabled="currentPage === 1" @click="currentPage = 1">|<</button>
          <button :disabled="currentPage === 1" @click="currentPage--"><</button>
          <template v-for="(p, idx) in paginationButtons" :key="idx">
            <span v-if="p === '...'" class="pag-ellipsis">...</span>
            <button v-else :class="{ active: p === currentPage, skip: p % 5 === 0 && p !== 1 && p !== totalPages }" @click="currentPage = p">{{ p }}</button>
          </template>
          <button :disabled="currentPage === totalPages" @click="currentPage++">></button>
          <button :disabled="currentPage === totalPages" @click="currentPage = totalPages">>|</button>
        </div>
      </div>
    </section>

    <button v-if="userStore.condition" class="fab" @click="uploadStore.loadFileVisible = true">
      <svg width="18" height="18" viewBox="0 0 18 18" fill="none"><path d="M9 3v12M3 9h12" stroke="currentColor" stroke-width="2" stroke-linecap="round"/></svg>
      上传资料
    </button>

    <UploadResourceDialog />
    <Teleport to="body">
      <div class="modal-overlay" v-if="deleteDialog" @click.self="deleteDialog = false">
        <div class="modal-card">
          <h3>确认删除</h3>
          <p>确定要删除此资源吗？</p>
          <div class="modal-actions">
            <button class="btn-secondary" @click="deleteDialog = false">取消</button>
            <button class="btn-primary" style="background:#ff3b30;border-color:#ff3b30" @click="doDelete">删除</button>
          </div>
        </div>
      </div>
    </Teleport>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from "vue";
import { useUserStore } from "@/stores/user";
import { useUploadStore } from "@/stores/upload";
import { getAll, getDownloadUrl, deleteById } from "@/request/axiosForResources.js";
import UploadResourceDialog from "@/components/UploadResourceDialog.vue";
import { ElMessage } from "element-plus";

function fmtDate(d) {
  if (!d) return "";
  var t = new Date(d);
  var pad = function(n){ return String(n).padStart(2,"0"); };
  return t.getFullYear()+"/"+pad(t.getMonth()+1)+"/"+pad(t.getDate())+" "+pad(t.getHours())+":"+pad(t.getMinutes())+":"+pad(t.getSeconds());
}

var userStore = useUserStore();
var uploadStore = useUploadStore();

var loading = ref(true);
var resources = ref([]);
var currentPage = ref(1);
var pageSize = 6;
var deleteDialog = ref(false);
var toDelete = ref(null);

var totalPages = computed(function(){ return Math.max(1, Math.ceil(resources.value.length / pageSize)); });

var pagedResources = computed(function(){
  var s = (currentPage.value - 1) * pageSize;
  return resources.value.slice(s, s + pageSize);
});

var paginationButtons = computed(function(){
  var total = totalPages.value;
  var cur = currentPage.value;
  if (total <= 9) return Array.from({length: total}, function(_, i){ return i + 1; });

  var pages = new Set([1, total]);
  for (var i = cur - 2; i <= cur + 2; i++) {
    if (i >= 1 && i <= total) pages.add(i);
  }
  for (var j = 5; j < total; j += 5) pages.add(j);
  var sorted = Array.from(pages).sort(function(a, b){ return a - b; });

  var result = [];
  for (var k = 0; k < sorted.length; k++) {
    result.push(sorted[k]);
    if (k < sorted.length - 1 && sorted[k + 1] - sorted[k] > 1) {
      result.push("...");
    }
  }
  return result;
});

async function download(objectName) {
  try {
    var resp = await getDownloadUrl(objectName);
    if (!resp || !resp.data) return;
    var a = document.createElement("a");
    a.href = resp.data;
    a.style.display = "none";
    document.body.appendChild(a);
    a.click();
    a.remove();
  } catch(e) {}
}
function confirmDelete(r) { toDelete.value = r; deleteDialog.value = true; }
async function doDelete() {
  if (!toDelete.value) return;
  await deleteById(toDelete.value.id);
  ElMessage.success("删除成功");
  resources.value = resources.value.filter(function(x){ return x.id !== toDelete.value.id; });
  if (currentPage.value > totalPages.value) currentPage.value = totalPages.value;
  deleteDialog.value = false; toDelete.value = null;
}

onMounted(async function(){
  try {
    var res = await getAll();
    if (res && res.data) resources.value = res.data;
  } catch(e) {}
  loading.value = false;
});
</script>

<style scoped>
.resource-page { padding-top: var(--nav-height); }
.page-hero .pill {
  display: inline-flex; font-size: 12px; font-weight: 600; color: var(--color-accent);
  background: rgba(0,113,227,.08); padding: 6px 16px; border-radius: 980px; margin-bottom: 16px;
}
.resource-grid {
  display: grid; grid-template-columns: repeat(2, 1fr); gap: 28px; padding: 40px 0;
}
.res-card {
  background: var(--color-surface); border-radius: var(--radius-lg); overflow: hidden;
  border: 1px solid var(--color-border);
  transition: transform 0.35s var(--easing-spring), box-shadow 0.35s var(--easing-spring);
}
.res-card:hover { transform: translateY(-4px); box-shadow: var(--shadow-lg); }
.res-cover {
  position: relative; width: 100%; aspect-ratio: 16 / 10; overflow: hidden;
  cursor: pointer; background: linear-gradient(135deg, #e8ecf4, #d5dde8);
}
.res-cover-img {
  width: 100%; height: 100%; object-fit: cover; display: block;
  transition: transform 0.5s var(--easing-emphasized);
}
.res-cover:hover .res-cover-img { transform: scale(1.05); }
.res-cover-placeholder {
  width: 100%; height: 100%; display: flex; align-items: center; justify-content: center;
  color: var(--color-text-tertiary);
}
.res-cover-overlay {
  position: absolute; inset: 0; background: rgba(0,0,0,0.35);
  display: flex; align-items: center; justify-content: center;
  opacity: 0; transition: opacity 0.3s; color: #fff;
}
.res-cover:hover .res-cover-overlay { opacity: 1; }
.res-body { padding: 24px; display: flex; flex-direction: column; gap: 12px; }
.res-title-row { display: flex; justify-content: space-between; align-items: flex-start; gap: 12px; }
.res-title-row h3 { font-family: var(--font-heading); font-size: 18px; font-weight: 600; line-height: 1.3; }
.res-del {
  width: 28px; height: 28px; border-radius: 50%; border: 1px solid var(--color-border);
  background: none; cursor: pointer; display: flex; align-items: center; justify-content: center;
  color: var(--color-text-tertiary); transition: all .2s; flex-shrink: 0;
}
.res-del:hover { border-color: #ff3b30; color: #ff3b30; }
.res-desc {
  color: var(--color-text-secondary); font-size: 14px; line-height: 1.7;
  display: -webkit-box; -webkit-line-clamp: 3; -webkit-box-orient: vertical; overflow: hidden;
}
.res-meta {
  display: flex; gap: 16px; align-items: center; font-size: 13px;
  color: var(--color-text-tertiary); padding-top: 12px; border-top: 1px solid var(--color-border);
}
.res-avatar { width: 22px; height: 22px; border-radius: 50%; object-fit: cover; flex-shrink: 0; }
.res-author { font-weight: 500; color: var(--color-text-secondary); }
.res-dl {
  display: inline-flex; align-items: center; gap: 8px;
  background: var(--color-accent); color: #fff; border: none;
  padding: 10px 24px; border-radius: 980px; font-size: 14px; font-weight: 600;
  cursor: pointer; font-family: var(--font-heading); align-self: flex-start;
  transition: all 0.25s var(--easing-spring);
}
.res-dl:hover { background: var(--color-accent-light); transform: translateY(-1px); }

.pagination { display: flex; justify-content: center; align-items: center; gap: 6px; padding: 24px 0 48px; }
.pagination button {
  width: 36px; height: 36px; border-radius: 50%;
  border: 1.5px solid var(--color-border); background: var(--color-surface);
  cursor: pointer; font-size: 13px; font-weight: 500; color: var(--color-text-secondary);
  display: flex; align-items: center; justify-content: center; transition: all 0.2s;
}
.pagination button:hover:not(:disabled) { border-color: var(--color-accent); color: var(--color-accent); }
.pagination button.active { background: var(--color-accent); color: #fff; border-color: var(--color-accent); }
.pagination button.skip { border-color: var(--color-accent); background: rgba(0,113,227,0.06); color: var(--color-accent); }
.pagination button:disabled { opacity: 0.3; cursor: not-allowed; }
.pag-ellipsis { color: var(--color-text-tertiary); padding: 0 4px; font-size: 14px; }

@media (max-width: 768px) { .resource-grid { grid-template-columns: 1fr; } }
</style>
