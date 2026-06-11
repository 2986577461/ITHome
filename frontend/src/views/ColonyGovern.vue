<template>
  <div class="gov-page">
    <div class="gov-hero">
      <div class="section-inner">
        <h1>学员管理系统</h1>
        <p>管理在册学员，审核新成员申请</p>
      </div>
    </div>

    <div class="gov-body">
      <div class="section-inner">
        <template v-if="pageLoading">
          <div class="skeleton-card" v-for="n in 4" :key="n" style="margin-bottom:12px">
            <div class="skeleton-title"></div>
            <div class="skeleton-text"></div>
            <div class="skeleton-text"></div>
          </div>
        </template>
        <template v-else>
        <!-- Controls bar -->
        <div class="gov-bar">
          <div class="gov-actions">
            <button class="gov-btn-approve" @click="governStore.toggleAdd()">
              <svg width="16" height="16" viewBox="0 0 16 16" fill="none">
                <path d="M8 2v12M2 8h12" stroke="currentColor" stroke-width="1.8" stroke-linecap="round"/>
              </svg>
              审核新学员
              <span v-if="governStore.newcomers.length" class="gov-badge-dot">{{ governStore.newcomers.length }}</span>
            </button>
            <button
              class="btn-secondary"
              @click="openUpdate"
              :disabled="selected.length === 0"
            >
              <svg width="16" height="16" viewBox="0 0 16 16" fill="none">
                <path
                  d="M11 2l3 3L7 12H4v-3L11 2z"
                  stroke="currentColor"
                  stroke-width="1.5"
                  stroke-linejoin="round"
                />
              </svg>
              修改信息
            </button>
            <button
              class="gov-btn-danger"
              @click="openRemove"
              :disabled="selected.length === 0"
            >
              <svg width="16" height="16" viewBox="0 0 16 16" fill="none">
                <path
                  d="M3 4h10M6 4V3a1 1 0 011-1h2a1 1 0 011 1v1M5 4v8a1 1 0 001 1h4a1 1 0 001-1V4"
                  stroke="currentColor"
                  stroke-width="1.4"
                  stroke-linecap="round"
                />
              </svg>
              删除学员
            </button>
            <button class="btn-secondary" @click="downloadFile">
              <svg width="16" height="16" viewBox="0 0 16 16" fill="none">
                <path
                  d="M8 2v8M4 7l4 4 4-4M2 13h12"
                  stroke="currentColor"
                  stroke-width="1.5"
                  stroke-linecap="round"
                  stroke-linejoin="round"
                />
              </svg>
              下载名单
            </button>
          </div>
          <div v-if="selected.length > 0" class="gov-selected-hint">
            已选 <strong>{{ selected.length }}</strong> 人
          </div>
        </div>

        <!-- Stats -->
        <div class="gov-stats">
          <div class="gov-stat">
            <div class="gs-icon gs-icon-blue">
              <svg width="22" height="22" viewBox="0 0 24 24" fill="none">
                <path
                  d="M17 21v-2a4 4 0 00-4-4H5a4 4 0 00-4 4v2M9 11a4 4 0 100-8 4 4 0 000 8zM23 21v-2a4 4 0 00-3-3.87M16 3.13a4 4 0 010 7.75"
                  stroke="currentColor"
                  stroke-width="1.6"
                  stroke-linecap="round"
                  stroke-linejoin="round"
                />
              </svg>
            </div>
            <div class="gs-info">
              <span class="gs-num">{{ governStore.member.length }}</span>
              <span class="gs-lbl">在册学员</span>
            </div>
          </div>
          <div class="gov-stat">
            <div class="gs-icon gs-icon-green">
              <svg width="22" height="22" viewBox="0 0 24 24" fill="none">
                <path
                  d="M14 2H6a2 2 0 00-2 2v16a2 2 0 002 2h12a2 2 0 002-2V8zM14 2v6h6M16 13H8M16 17H8M10 9H8"
                  stroke="currentColor"
                  stroke-width="1.6"
                  stroke-linecap="round"
                  stroke-linejoin="round"
                />
              </svg>
            </div>
            <div class="gs-info">
              <span class="gs-num">{{ articleCount }}</span>
              <span class="gs-lbl">文章总数</span>
            </div>
          </div>
          <div class="gov-stat">
            <div class="gs-icon gs-icon-orange">
              <svg width="22" height="22" viewBox="0 0 24 24" fill="none">
                <path
                  d="M22 19a2 2 0 01-2 2H4a2 2 0 01-2-2V5a2 2 0 012-2h5l2 3h9a2 2 0 012 2z"
                  stroke="currentColor"
                  stroke-width="1.6"
                  stroke-linecap="round"
                  stroke-linejoin="round"
                />
              </svg>
            </div>
            <div class="gs-info">
              <span class="gs-num">{{ resourceCount }}</span>
              <span class="gs-lbl">资料总数</span>
            </div>
          </div>
        </div>

        <!-- Student list -->
        <div class="gov-list">
          <div class="gov-list-header">
            <span class="glh-col glh-name">姓名</span>
            <span class="glh-col glh-id">学号</span>
            <span class="glh-col glh-gender">性别</span>
            <span class="glh-col glh-major">专业</span>
            <span class="glh-col glh-class">班级</span>
            <span class="glh-col glh-academy">学院</span>
            <span class="glh-col glh-pos">职位</span>
            <span class="glh-col glh-count">文章</span>
            <span class="glh-col glh-count">资料</span>
          </div>

          <div v-if="governStore.member.length === 0" class="gov-empty">
            <svg width="48" height="48" viewBox="0 0 48 48" fill="none">
              <path
                d="M34 42v-4a8 8 0 00-8-8H10a8 8 0 00-8 8v4M18 22a8 8 0 100-16 8 8 0 000 16zM46 42v-4a8 8 0 00-6-7.74M32 6.26a8 8 0 010 15.5"
                stroke="currentColor"
                stroke-width="1.5"
                opacity=".3"
                stroke-linecap="round"
                stroke-linejoin="round"
              />
            </svg>
            <p>暂无成员数据</p>
          </div>

          <div
            v-for="(s, i) in governStore.member"
            :key="s.id || i"
            class="gov-row"
            :class="{ selected: selected.includes(i) }"
            @click="toggleSelect(i)"
          >
            <span class="gr-name">
              <img v-if="s.avatar" :src="s.avatar" class="gr-avatar" />
              <span v-else class="gr-avatar">{{ (s.name || "?")[0] }}</span>
              {{ s.name }}
            </span>
            <span class="gr-id">{{ s.studentId }}</span>
            <span class="gr-gender">{{ s.sex }}</span>
            <span class="gr-major">{{ s.major }}</span>
            <span class="gr-class">{{ s.className }}</span>
            <span class="gr-academy">{{ s.academy }}</span>
            <span class="gr-pos">
              <span class="gr-pos-tag" :class="posClass(s.position)">{{
                s.position || "学员"
              }}</span>
            </span>
            <span class="gr-count">{{ s.articleCount }}</span>
            <span class="gr-count">{{ s.resourceCount }}</span>
          </div>
        </div>
      </template>
    </div>
  </div>

    <Teleport to="body">
      <div
        class="modal-overlay"
        v-if="removeDialog"
        @click.self="removeDialog = false"
      >
        <div class="modal-card">
          <h3>确认删除</h3>
          <p>{{ removeMsg }}</p>
          <div class="modal-actions">
            <button class="btn-secondary" @click="removeDialog = false">
              取消
            </button>
            <button
              class="btn-primary"
              style="background: #ff3b30; border-color: #ff3b30"
              @click="doRemove"
            >
              确定删除
            </button>
          </div>
        </div>
      </div>
    </Teleport>

    <AddStudentDialog />
    <UpdateStudentDialog />
  </div>
</template>

<script setup>
import { ref, onMounted } from "vue";
import { useGovernStore } from "@/stores/govern";
import { getAll, removeBatch, downloadExcel } from "@/request/axiosForUser";
import { getArticleCount } from "@/request/axiosForArticles";
import { getResourcesCount } from "@/request/axiosForResources";
import { getAllNewcomers } from "@/request/axiosForNewcomers";
import AddStudentDialog from "@/components/AddStudentDialog.vue";
import UpdateStudentDialog from "@/components/UpdateStudentDialog.vue";
import { ElMessage } from "element-plus";

var governStore = useGovernStore();
var pageLoading = ref(true);
var selected = ref([]);
var articleCount = ref(0);
var resourceCount = ref(0);
var removeDialog = ref(false);
var removeMsg = ref("");

function toggleSelect(i) {
  var idx = selected.value.indexOf(i);
  if (idx === -1) {
    selected.value.push(i);
  } else {
    selected.value.splice(idx, 1);
  }
}

function posClass(p) {
  if (p === "会长" || p === "副会长") return "pos-leader";
  if (p === "部长") return "pos-manager";
  return "pos-member";
}

function openUpdate() {
  if (selected.value.length === 0) return ElMessage.error("请先选择学生");
  if (selected.value.length > 1) return ElMessage.error("只能选择一个学生");
  var i = selected.value[0];
  var s = governStore.member[i];
  governStore.oldStudentId = s.studentId;
  governStore.updateStudent = Object.assign({}, s);
  governStore.toggleUpdate();
}

function openRemove() {
  if (selected.value.length === 0) return ElMessage.error("请先选择学生");
  removeMsg.value =
    "确定要删除选中的 " + selected.value.length + " 名学生吗？此操作不可撤销。";
  removeDialog.value = true;
}

async function doRemove() {
  var ids = selected.value.map(function (i) {
    return governStore.member[i].studentId;
  });
  await removeBatch(ids);
  ElMessage.success("已删除 " + ids.length + " 名学员");
  removeDialog.value = false;
  selected.value = [];
  var resp = await getAll();
  if (resp && resp.data) governStore.member = resp.data;
}

async function downloadFile() {
  try {
    var resp = await downloadExcel();
    if (!resp || !resp.data) return;
    var a = document.createElement("a");
    a.href = resp.data;
    a.download = "学员名单.xlsx";
    a.click();
  } catch (e) {}
}

onMounted(async function () {
  try {
    var resp = await getAll();
    if (resp && resp.data) governStore.member = resp.data;
    var ac = await getArticleCount();
    if (ac && ac.data != null) articleCount.value = ac.data;
    var rc = await getResourcesCount();
    if (rc && rc.data != null) resourceCount.value = rc.data;
    await getAllNewcomers().then(function(r){ if (r && r.data) governStore.newcomers = r.data; });
  } catch(e) {}
  pageLoading.value = false;
});
</script>

<style scoped>
.gov-page {
  padding-top: var(--nav-height);
}

.gov-hero {
  background: linear-gradient(135deg, var(--color-accent), #5856d6);
  padding: 56px 0;
  text-align: center;
  color: #fff;
}
.gov-hero h1 {
  font-family: var(--font-heading);
  font-size: clamp(28px, 4vw, 40px);
  font-weight: 700;
  letter-spacing: -1px;
  margin-bottom: 8px;
}
.gov-hero p {
  color: rgba(255, 255, 255, 0.7);
  font-size: 15px;
}

.gov-body {
  background: var(--color-bg);
  min-height: calc(100vh - var(--nav-height));
  padding: 32px 0;
}

/* Controls */
.gov-bar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  flex-wrap: wrap;
  gap: 12px;
  margin-bottom: 24px;
}
.gov-actions {
  display: flex;
  gap: 10px;
  flex-wrap: wrap;
}
.gov-badge-dot {
  position: absolute; top: -8px; right: -8px;
  min-width: 22px; height: 22px; border-radius: 50%;
  background: #ff3b30; color: #fff;
  font-size: 11px; font-weight: 700;
  display: flex; align-items: center; justify-content: center;
  padding: 0 5px;
  box-shadow: 0 2px 6px rgba(255,59,48,0.4);
}
.gov-btn-approve {
  position: relative;
  display: inline-flex; align-items: center; gap: 6px;
  background: linear-gradient(135deg, #34c759, #30b350);
  color: #fff; border: none; border-radius: 980px;
  padding: 10px 22px; font-size: 14px; font-weight: 600;
  cursor: pointer; font-family: var(--font-heading);
  transition: all 0.25s var(--easing-spring);
  box-shadow: 0 4px 14px rgba(52,199,89,0.3);
}
.gov-btn-approve:hover {
  background: linear-gradient(135deg, #30b350, #28a745);
  transform: translateY(-1px);
  box-shadow: 0 6px 20px rgba(52,199,89,0.4);
}
.gov-btn-danger {
  align-items: center;
  gap: 6px;
  background: transparent;
  color: #ff3b30;
  border: 1.5px solid #ff3b30;
  border-radius: 980px;
  padding: 10px 20px;
  font-size: 14px;
  font-weight: 600;
  cursor: pointer;
  font-family: var(--font-heading);
  transition: all 0.25s;
}
.gov-btn-danger:hover:not(:disabled) {
  background: #ff3b30;
  color: #fff;
}
.gov-btn-danger:disabled {
  opacity: 0.35;
  cursor: not-allowed;
}
.gov-selected-hint {
  font-size: 14px;
  color: var(--color-accent);
  padding: 8px 16px;
  background: rgba(0, 113, 227, 0.06);
  border-radius: 980px;
}

/* Stats */
.gov-stats {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 20px;
  margin-bottom: 28px;
}
.gov-stat {
  background: var(--color-surface);
  border-radius: var(--radius-md);
  padding: 24px;
  display: flex;
  align-items: center;
  gap: 16px;
  border: 1px solid var(--color-border);
}
.gs-icon {
  width: 48px;
  height: 48px;
  border-radius: var(--radius-sm);
  display: flex;
  align-items: center;
  justify-content: center;
}
.gs-icon-blue {
  background: rgba(0, 113, 227, 0.1);
  color: var(--color-accent);
}
.gs-icon-green {
  background: rgba(52, 199, 89, 0.1);
  color: #34c759;
}
.gs-icon-orange {
  background: rgba(255, 149, 0, 0.1);
  color: #ff9500;
}
.gs-info {
  display: flex;
  flex-direction: column;
}
.gs-num {
  font-family: var(--font-heading);
  font-size: 28px;
  font-weight: 700;
}
.gs-lbl {
  font-size: 13px;
  color: var(--color-text-tertiary);
  margin-top: 2px;
}

/* List */
.gov-list {
  background: var(--color-surface);
  border-radius: var(--radius-md);
  border: 1px solid var(--color-border);
  overflow: hidden;
}
.gov-list-header {
  display: flex;
  align-items: center;
  padding: 14px 20px;
  background: var(--color-bg);
  border-bottom: 1px solid var(--color-border);
  font-size: 12px;
  font-weight: 600;
  color: var(--color-text-tertiary);
  text-transform: uppercase;
  letter-spacing: 0.5px;
}
.gov-row {
  display: flex;
  align-items: center;
  padding: 16px 20px;
  border-bottom: 1px solid var(--color-border);
  cursor: pointer;
  transition: background 0.15s;
  font-size: 15px;
}
.gov-row:last-child {
  border-bottom: none;
}
.gov-row:hover {
  background: var(--color-bg);
}
.gov-row.selected {
  background: #e6f0ff;
  border-left: 4px solid var(--color-accent);
  padding-left: 16px;
}
.gov-row.selected .gr-name {
  color: var(--color-accent);
  font-weight: 600;
}
.gov-row.selected .gr-avatar {
  box-shadow: 0 0 0 3px rgba(0, 113, 227, 0.3);
}

.gr-avatar {
  width: 32px;
  height: 32px;
  border-radius: 50%;
  object-fit: cover;
  background: linear-gradient(
    135deg,
    var(--color-accent),
    var(--color-gradient-mid)
  );
  color: #fff;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  font-size: 14px;
  font-weight: 600;
  margin-right: 8px;
  flex-shrink: 0;
}

/* Column widths */
.glh-name,
.gr-name {
  flex: 2;
  min-width: 120px;
  display: flex;
  align-items: center;
}
.glh-id,
.gr-id {
  flex: 1.2;
  min-width: 90px;
}
.glh-gender,
.gr-gender {
  flex: 0.8;
  min-width: 50px;
}
.glh-major,
.gr-major {
  flex: 1.5;
  min-width: 100px;
}
.glh-class,
.gr-class {
  flex: 1;
  min-width: 70px;
}
.glh-academy,
.gr-academy {
  flex: 1.5;
  min-width: 100px;
}
.glh-pos,
.gr-pos {
  flex: 1.2;
  min-width: 80px;
}
.glh-count,
.gr-count {
  flex: 0.7;
  min-width: 50px;
  text-align: center;
}

.gr-pos-tag {
  display: inline-block;
  padding: 3px 12px;
  border-radius: 980px;
  font-size: 12px;
  font-weight: 600;
}
.pos-leader {
  background: rgba(0, 113, 227, 0.1);
  color: var(--color-accent);
}
.pos-manager {
  background: rgba(88, 86, 214, 0.1);
  color: #5856d6;
}
.pos-member {
  background: var(--color-bg);
  color: var(--color-text-secondary);
}

.gov-empty {
  text-align: center;
  padding: 80px 20px;
  color: var(--color-text-tertiary);
}
.gov-empty p {
  margin-top: 12px;
  font-size: 15px;
}

@media (max-width: 860px) {
  .gov-stats {
    grid-template-columns: 1fr;
  }
  .gov-actions {
    width: 100%;
  }
  .gov-actions button {
    flex: 1;
    justify-content: center;
  }
  .gov-list-header {
    display: none;
  }
  .gov-row {
    flex-wrap: wrap;
    gap: 4px 12px;
  }
}
</style>
