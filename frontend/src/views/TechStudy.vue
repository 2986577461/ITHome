<template>
  <div class="tech-page">
    <div
      class="page-hero hero-accent-blue"
      style="background: linear-gradient(180deg, #f0f4ff 0%, #f5f5f7 100%)"
    >
      <div class="hero-bg-mesh"></div>
      <div class="section-inner">
        <div class="pill">技术教学</div>
        <h1>学习资源库</h1>
        <p>C语言 · HTML · CSS · JavaScript · Java · MySQL</p>
      </div>
    </div>

    <section ref="scrollRoot" class="section scroll-section">
      <div v-if="loadingMore" class="page-loading-overlay">
        <div class="page-loading-spinner"></div>
      </div>
      <div class="section-inner">
        <div class="cat-pills">
          <button
            v-for="g in groups"
            :key="g.key"
            :class="{ active: activeCat === g.key }"
            @click="switchCat(g.key)"
          >
            {{ g.label }}
          </button>
          <button
            :class="{ active: activeCat === 'all' }"
            @click="switchCat('all')"
          >
            全部
          </button>
        </div>

        <div class="article-list">
          <template v-if="loading">
            <div class="skeleton-card" v-for="n in 3" :key="n">
              <div class="skeleton-title"></div>
              <div class="skeleton-text"></div>
              <div class="skeleton-text"></div>
              <div class="skeleton-text"></div>
            </div>
          </template>

          <template v-else>
            <div v-for="a in filteredArticles" :key="a.id" class="article-card">
              <div class="ac-head">
                <span class="ac-badge">{{ typeLabel(a.type) }}</span>
                <div
                  v-if="a.studentId === userStore.studentId"
                  class="ac-actions"
                >
                  <button class="ac-btn" title="编辑" @click="editArticle(a)">
                    <svg width="14" height="14" viewBox="0 0 15 15" fill="none">
                      <path
                        d="M10.5 2.5l2 2L6 11H4V9l6.5-6.5z"
                        stroke="currentColor"
                        stroke-width="1.3"
                        stroke-linejoin="round"
                      />
                    </svg>
                  </button>
                  <button class="ac-btn" title="删除" @click="confirmDelete(a)">
                    <svg width="14" height="14" viewBox="0 0 15 15" fill="none">
                      <path
                        d="M3 4h9M6 4V3a1 1 0 011-1h1a1 1 0 011 1v1M5 4v8a1 1 0 001 1h3a1 1 0 001-1V4"
                        stroke="currentColor"
                        stroke-width="1.3"
                        stroke-linecap="round"
                      />
                    </svg>
                  </button>
                </div>
              </div>
              <h2 class="ac-title">{{ a.head }}</h2>
              <div class="ac-meta">
                <span class="ac-author">
                  <img v-if="a.avatar" :src="a.avatar" class="ac-avatar" />
                  <span v-else class="ac-avatar">{{ (a.name || "?")[0] }}</span>
                  {{ a.name }}
                </span>
                <span>{{ fmtDate(a.updatedDateTime) }}</span>
              </div>
              <div class="ac-body" v-html="a.content"></div>
              <div class="ac-interact">
                <button
                  class="interact-btn"
                  :class="{ liked: liked[a.id] }"
                  @click="toggleLike(a.id)"
                >
                  <svg width="16" height="16" viewBox="0 0 16 16" fill="none">
                    <path
                      d="M8 3.5C6.5 2 4 2 4 5s4 4.5 4 4.5S12 9 12 6s-2.5-3-4-2.5z"
                      stroke="currentColor"
                      stroke-width="1.3"
                      stroke-linecap="round"
                    />
                  </svg>
                  {{ likeCount[a.id] || 0 }}
                </button>
                <button class="interact-btn" @click="toggleComments(a.id)">
                  <svg width="16" height="16" viewBox="0 0 16 16" fill="none">
                    <path
                      d="M3 2h10a1 1 0 011 1v8a1 1 0 01-1 1H7l-3 2v-2H3a1 1 0 01-1-1V3a1 1 0 011-1z"
                      stroke="currentColor"
                      stroke-width="1.3"
                      stroke-linecap="round"
                    />
                  </svg>
                  {{ (comments[a.id] || []).length }}
                </button>
                <button class="interact-btn share-btn" @click="shareArticle(a)">
                  <svg width="16" height="16" viewBox="0 0 16 16" fill="none">
                    <path
                      d="M12 4a2 2 0 100-4 2 2 0 000 4zM4 10a2 2 0 100-4 2 2 0 000 4zM12 16a2 2 0 100-4 2 2 0 000 4zM6.5 7.5l3-2M6.5 8.5l3 2"
                      stroke="currentColor"
                      stroke-width="1.3"
                      stroke-linecap="round"
                    />
                  </svg>
                  分享
                </button>
              </div>
              <div v-if="showComments[a.id]" class="comments-section">
                <div class="comment-input-row">
                  <input
                    v-model="commentInputs[a.id]"
                    placeholder="写下你的评论..."
                    @keydown.enter.prevent="addComment(a.id)"
                  />
                  <button
                    class="comment-send"
                    @click="addComment(a.id)"
                    :disabled="!commentInputs[a.id]?.trim()"
                  >
                    发送
                  </button>
                </div>
                <div
                  v-for="c in comments[a.id] || []"
                  :key="c.id"
                  class="comment-item"
                >
                  <div class="comment-avatar">{{ (c.name || "?")[0] }}</div>
                  <div class="comment-body">
                    <div class="comment-meta">
                      <span class="comment-name">{{ c.name }}</span>
                      <span class="comment-time">{{ c.time || "刚刚" }}</span>
                    </div>
                    <div class="comment-text">{{ c.content }}</div>
                    <button
                      class="comment-reply-btn"
                      @click="toggleReplyInput(a.id, c.id)"
                    >
                      回复
                    </button>
                    <div v-if="c.replies?.length" class="replies-wrap">
                      <div
                        v-for="r in c.replies"
                        :key="r.id"
                        class="reply-item"
                      >
                        <span class="reply-name">{{ r.name }}</span>
                        <span class="reply-text">{{ r.content }}</span>
                      </div>
                    </div>
                    <div
                      v-if="replyInputVisible[a.id] === c.id"
                      class="reply-input-row"
                    >
                      <input
                        v-model="replyInputs[a.id + '_' + c.id]"
                        placeholder="回复..."
                        @keydown.enter.prevent="addReply(a.id, c.id)"
                      />
                      <button
                        class="comment-send"
                        @click="addReply(a.id, c.id)"
                      >
                        回复
                      </button>
                    </div>
                  </div>
                </div>
                <div v-if="!(comments[a.id] || []).length" class="no-comments">
                  暂无评论，快来抢沙发吧~
                </div>
              </div>
            </div>

            <div
              v-if="filteredArticles.length === 0 && !loadingMore"
              class="empty-state"
            >
              <svg width="48" height="48" viewBox="0 0 48 48" fill="none">
                <rect
                  x="10"
                  y="8"
                  width="28"
                  height="34"
                  rx="3"
                  stroke="currentColor"
                  stroke-width="1.5"
                  opacity=".3"
                />
                <path
                  d="M18 20h12M18 26h10M18 32h8"
                  stroke="currentColor"
                  stroke-width="1.5"
                  stroke-linecap="round"
                  opacity=".3"
                />
              </svg>
              <p>暂无文章</p>
            </div>
          </template>
        </div>
      </div>
    </section>

    <div v-if="articles.length > 0" class="page-bar">
      <button
        class="pag-nav"
        :disabled="currentPage === 1 || loadingMore"
        @click="goToPage(1)"
      >
        首页
      </button>
      <div class="pag-numbers">
        <template v-for="(p, idx) in pageNumbers" :key="p">
          <span v-if="idx > 0 && p - pageNumbers[idx - 1] > 1" class="pag-gap"
            >...</span
          >
          <button
            :class="{ active: p === currentPage }"
            :disabled="loadingMore"
            @click="goToPage(p)"
          >
            {{ p }}
          </button>
        </template>
      </div>

      <button
        class="pag-nav"
        :disabled="currentPage === totalPages || loadingMore"
        @click="goToPage(totalPages)"
      >
        末页
      </button>
    </div>

    <button v-if="userStore.condition" class="fab" @click="goUpload">
      <svg width="18" height="18" viewBox="0 0 18 18" fill="none">
        <path
          d="M9 3v12M3 9h12"
          stroke="currentColor"
          stroke-width="2"
          stroke-linecap="round"
        />
      </svg>
      发表文章
    </button>

    <Teleport to="body">
      <div
        class="modal-overlay"
        v-if="deleteDialog"
        @click.self="deleteDialog = false"
      >
        <div class="modal-card">
          <h3>确认删除</h3>
          <p>确定要删除这篇文章吗？此操作不可撤销。</p>
          <div class="modal-actions">
            <button class="btn-secondary" @click="deleteDialog = false">
              取消
            </button>
            <button
              class="btn-primary"
              style="background: #ff3b30; border-color: #ff3b30"
              @click="doDelete"
            >
              删除
            </button>
          </div>
        </div>
      </div>
    </Teleport>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted } from "vue";
import { useRouter } from "vue-router";
import { useUserStore } from "@/stores/user";
import { useArticleStore } from "@/stores/updateArticle";
import {
  getPage,
  getArticleCount,
  deleteById,
} from "@/request/axiosForArticles.js";
import { toggleLike as apiToggleLike } from "@/request/axiosForLikes.js";
import {
  addComment as apiAddComment,
  replyComment as apiReplyComment,
} from "@/request/axiosForComments.js";
import { ElMessage } from "element-plus";

const router = useRouter();
const userStore = useUserStore();
const articleStore = useArticleStore();

const groups = [
  { key: "1", label: "C语言" },
  { key: "2", label: "HTML" },
  { key: "3", label: "CSS" },
  { key: "4", label: "JavaScript" },
  { key: "5", label: "Java" },
  { key: "6", label: "MySQL" },
];
const articles = ref([]);
const activeCat = ref("all");
const deleteDialog = ref(false);
const toDelete = ref(null);
const pageSize = 5;
const currentPage = ref(1);
const totalCount = ref(0);
const loading = ref(true);
const loadingMore = ref(false);
const totalPages = computed(() =>
  Math.max(1, Math.ceil(totalCount.value / pageSize)),
);
const hasMore = computed(() => articles.value.length === pageSize);
const pageNumbers = computed(() => {
  const total = totalPages.value;
  const cur = currentPage.value;
  if (total <= 7) return Array.from({ length: total }, (_, i) => i + 1);
  const pages = new Set([1, total]);
  for (let i = cur - 2; i <= cur + 2; i++)
    if (i >= 1 && i <= total) pages.add(i);
  return [...pages].sort((a, b) => a - b);
});
const filteredArticles = computed(() => {
  if (activeCat.value === "all") return articles.value;
  return articles.value.filter((a) => a.type == activeCat.value);
});
const scrollRoot = ref(null);

const liked = reactive({});
const likeCount = reactive({});
const showComments = reactive({});
const comments = reactive({});
const commentInputs = reactive({});
const replyInputs = reactive({});
const replyInputVisible = reactive({});

function fmtDate(d) {
  if (!d) return "";
  var t = new Date(d),
    p = function (n) {
      return String(n).padStart(2, "0");
    };
  return (
    t.getFullYear() +
    "/" +
    p(t.getMonth() + 1) +
    "/" +
    p(t.getDate()) +
    " " +
    p(t.getHours()) +
    ":" +
    p(t.getMinutes()) +
    ":" +
    p(t.getSeconds())
  );
}
function typeLabel(t) {
  return (
    { 1: "C语言", 2: "HTML", 3: "CSS", 4: "JavaScript", 5: "Java", 6: "MySQL" }[
      String(t)
    ] || "其他"
  );
}

async function fetchPage(p) {
  const type = activeCat.value === "all" ? null : activeCat.value;
  loadingMore.value = true;
  try {
    const resp = await getPage(p, pageSize, type);
    if (resp?.data) {
      const list = resp.data.records ?? resp.data;
      articles.value = list;
      list.forEach((a) => {
        if (!(a.id in likeCount)) {
          likeCount[a.id] = 0;
          liked[a.id] = false;
        }
      });
    }
  } catch {
    articles.value = [];
  }
  loadingMore.value = false;
}

async function goToPage(p) {
  if (p === currentPage.value || p < 1 || p > totalPages.value) return;
  currentPage.value = p;
  loading.value = true;
  await fetchPage(p);
  loading.value = false;
  if (scrollRoot.value) scrollRoot.value.scrollTop = 0;
}

async function switchCat(key) {
  activeCat.value = key;
  articles.value = [];
  currentPage.value = 1;
  totalCount.value = 0;
  loading.value = true;
  await fetchPage(1);
  await fetchCount();
  loading.value = false;
  if (scrollRoot.value) scrollRoot.value.scrollTop = 0;
}

async function fetchCount() {
  const type = activeCat.value === "all" ? null : activeCat.value;
  try {
    const resp = await getArticleCount(type);
    if (resp?.data != null) totalCount.value = resp.data;
  } catch {}
}

function editArticle(a) {
  articleStore.setArticle(a.id, a.head, a.content, a.type);
  router.push("/upload-article");
}
function goUpload() {
  articleStore.setArticle(null, "", "", "");
  router.push("/upload-article");
}
function confirmDelete(a) {
  toDelete.value = a;
  deleteDialog.value = true;
}

async function doDelete() {
  if (!toDelete.value) return;
  await deleteById(toDelete.value.id);
  ElMessage.success("删除成功");
  articles.value = articles.value.filter((a) => a.id !== toDelete.value.id);
  totalCount.value = Math.max(0, totalCount.value - 1);
  deleteDialog.value = false;
  toDelete.value = null;
}

async function toggleLike(articleId) {
  liked[articleId] = !liked[articleId];
  likeCount[articleId] =
    (likeCount[articleId] || 0) + (liked[articleId] ? 1 : -1);
  try {
    await apiToggleLike(articleId);
  } catch {}
}

function toggleComments(articleId) {
  showComments[articleId] = !showComments[articleId];
  if (showComments[articleId] && !comments[articleId]) comments[articleId] = [];
}

async function addComment(articleId) {
  const text = commentInputs[articleId]?.trim();
  if (!text) return;
  if (!comments[articleId]) comments[articleId] = [];
  comments[articleId].push({
    id: Date.now(),
    name: userStore.name || "我",
    content: text,
    time: "刚刚",
    replies: [],
  });
  commentInputs[articleId] = "";
  try {
    await apiAddComment({ articleId, content: text });
  } catch {}
}

function toggleReplyInput(articleId, commentId) {
  replyInputVisible[articleId] =
    replyInputVisible[articleId] === commentId ? null : commentId;
}

async function addReply(articleId, commentId) {
  const key = articleId + "_" + commentId;
  const text = replyInputs[key]?.trim();
  if (!text) return;
  const c = (comments[articleId] || []).find((x) => x.id === commentId);
  if (c) {
    if (!c.replies) c.replies = [];
    c.replies.push({
      id: Date.now(),
      name: userStore.name || "我",
      content: text,
    });
  }
  replyInputs[key] = "";
  replyInputVisible[articleId] = null;
  try {
    await apiReplyComment({ commentId, content: text });
  } catch {}
}

function shareArticle() {
  navigator.clipboard?.writeText(window.location.origin + "/#/tech-study");
  ElMessage.success("链接已复制");
}

onMounted(async () => {
  loading.value = true;
  await fetchPage(1);
  await fetchCount();
  loading.value = false;
});
</script>

<style scoped>
.tech-page {
  padding-top: var(--nav-height);
  height: 100vh;
  display: flex;
  flex-direction: column;
  overflow: hidden;
}
.scroll-section {
  flex: 1;
  overflow-y: auto;
  position: relative;
  padding-bottom: 32px;
  scrollbar-width: none;
  -ms-overflow-style: none;
  max-width: var(--max-width);
  margin: 0 auto;
  width: 100%;
}
.scroll-section::-webkit-scrollbar {
  display: none;
}
.scroll-section .section-inner {
  padding-top: 24px;
}
.page-hero .pill {
  display: inline-flex;
  font-size: 12px;
  font-weight: 600;
  color: var(--color-accent);
  background: rgba(0, 113, 227, 0.08);
  padding: 6px 16px;
  border-radius: 980px;
  margin-bottom: 16px;
}

.cat-pills {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  padding: 14px 0;
  position: sticky;
  top: 0;
  z-index: 10;
  background: var(--color-bg);
  border-bottom: 1px solid var(--color-border);
  margin-bottom: 20px;
}
.cat-pills button {
  padding: 8px 18px;
  border-radius: 980px;
  border: 1.5px solid var(--color-border);
  background: var(--color-surface);
  cursor: pointer;
  font-size: 13px;
  font-weight: 500;
  display: flex;
  align-items: center;
  gap: 6px;
  transition: all 0.2s;
}
.cat-pills button:hover {
  border-color: var(--color-accent);
  color: var(--color-accent);
}
.cat-pills button.active {
  background: var(--color-accent);
  color: #fff;
  border-color: var(--color-accent);
}
.cat-count {
  font-size: 11px;
  background: rgba(0, 0, 0, 0.06);
  padding: 1px 7px;
  border-radius: 980px;
  font-weight: 600;
}
.active .cat-count {
  background: rgba(255, 255, 255, 0.2);
}

.article-card {
  background: var(--color-surface);
  border-radius: var(--radius-md);
  padding: 28px;
  margin-bottom: 20px;
  border: 1px solid var(--color-border);
  transition: box-shadow 0.2s;
}
.article-card:hover {
  box-shadow: var(--shadow-sm);
}
.ac-head {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 10px;
}
.ac-badge {
  font-size: 11px;
  font-weight: 600;
  color: #fff;
  background: var(--color-accent);
  padding: 3px 12px;
  border-radius: 980px;
}
.ac-actions {
  display: flex;
  gap: 6px;
}
.ac-btn {
  width: 42px;
  height: 42px;
  border-radius: 50%;
  border: 1px solid var(--color-border);
  background: var(--color-surface);
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  color: var(--color-text-secondary);
  transition: all 0.2s;
}
.ac-btn:hover {
  border-color: var(--color-accent);
  color: var(--color-accent);
}
.ac-title {
  font-family: var(--font-heading);
  font-size: 38px;
  font-weight: 700;
  margin-bottom: 12px;
}
.ac-meta {
  display: flex;
  gap: 16px;
  align-items: center;
  font-size: 18px;
  color: var(--color-text-secondary);
  margin-bottom: 18px;
}
.ac-author {
  display: flex;
  align-items: center;
  gap: 6px;
}
.ac-avatar {
  width: 33px;
  height: 33px;
  border-radius: 50%;
  object-fit: cover;
  background: var(--color-accent);
  color: #fff;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 11px;
  font-weight: 600;
}
.ac-body {
  line-height: 1.9;
  font-size: 24px;
  word-wrap: break-word;
  color: var(--color-text-secondary);
}
.ac-body :deep(img) {
  max-width: 100%;
  border-radius: 8px;
  margin: 8px 0;
}
.ac-body :deep(pre) {
  overflow-x: auto;
  background: #f4f4f6;
  padding: 14px;
  border-radius: 8px;
  font-size: 13px;
}

.ac-interact {
  display: flex;
  gap: 6px;
  padding-top: 14px;
  margin-top: 14px;
  border-top: 1px solid var(--color-border);
}
.interact-btn {
  display: flex;
  align-items: center;
  gap: 5px;
  padding: 5px 12px;
  border-radius: 980px;
  border: 1px solid var(--color-border);
  background: var(--color-surface);
  font-size: 12px;
  color: var(--color-text-secondary);
  cursor: pointer;
  transition: all 0.15s;
}
.interact-btn:hover {
  border-color: var(--color-accent);
  color: var(--color-accent);
}
.interact-btn.liked {
  background: #fff0f0;
  border-color: #ff3b30;
  color: #ff3b30;
}
.interact-btn.liked svg {
  animation: like-pop 0.35s var(--easing-spring);
}
@keyframes like-pop {
  0% {
    transform: scale(1);
  }
  30% {
    transform: scale(1.3);
  }
  60% {
    transform: scale(0.9);
  }
  100% {
    transform: scale(1);
  }
}
.share-btn {
  margin-left: auto;
}

.comments-section {
  margin-top: 12px;
  padding-top: 12px;
  border-top: 1px solid var(--color-border);
}
.comment-input-row {
  display: flex;
  gap: 8px;
  margin-bottom: 14px;
}
.comment-input-row input {
  flex: 1;
  padding: 8px 14px;
  border: 1.5px solid var(--color-border);
  border-radius: 980px;
  font-size: 13px;
  outline: none;
  font-family: var(--font-body);
  transition: border-color 0.2s;
}
.comment-input-row input:focus {
  border-color: var(--color-accent);
}
.comment-send {
  padding: 8px 16px;
  border-radius: 980px;
  border: none;
  background: var(--color-accent);
  color: #fff;
  font-size: 12px;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.2s;
}
.comment-send:hover {
  background: var(--color-accent-light);
}
.comment-send:disabled {
  opacity: 0.4;
  cursor: not-allowed;
}

.comment-item {
  display: flex;
  gap: 10px;
  margin-bottom: 12px;
}
.comment-avatar {
  width: 26px;
  height: 26px;
  border-radius: 50%;
  background: var(--color-border);
  color: var(--color-text-secondary);
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 11px;
  font-weight: 600;
  flex-shrink: 0;
}
.comment-body {
  flex: 1;
}
.comment-meta {
  display: flex;
  gap: 8px;
  align-items: center;
  margin-bottom: 3px;
}
.comment-name {
  font-size: 12px;
  font-weight: 600;
}
.comment-time {
  font-size: 11px;
  color: var(--color-text-tertiary);
}
.comment-text {
  font-size: 13px;
  line-height: 1.5;
  color: var(--color-text-secondary);
}
.comment-reply-btn {
  font-size: 11px;
  color: var(--color-accent);
  background: none;
  border: none;
  cursor: pointer;
  padding: 2px 0;
  margin-top: 2px;
}
.replies-wrap {
  margin-top: 8px;
  padding-left: 12px;
  border-left: 2px solid var(--color-border);
}
.reply-item {
  font-size: 13px;
  margin-bottom: 6px;
  line-height: 1.5;
}
.reply-name {
  font-weight: 600;
  color: var(--color-accent);
  margin-right: 6px;
}
.reply-text {
  color: var(--color-text-secondary);
}
.reply-input-row {
  display: flex;
  gap: 6px;
  margin-top: 6px;
}
.reply-input-row input {
  flex: 1;
  padding: 6px 12px;
  border: 1.5px solid var(--color-border);
  border-radius: 980px;
  font-size: 12px;
  outline: none;
  font-family: var(--font-body);
  transition: border-color 0.2s;
}
.reply-input-row input:focus {
  border-color: var(--color-accent);
}
.no-comments {
  font-size: 13px;
  color: var(--color-text-tertiary);
  padding: 12px 0;
  text-align: center;
}

.page-bar {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 6px;
  padding: 16px 0 24px;
  flex-shrink: 0;
  user-select: none;
}
.pag-nav {
  height: 38px;
  padding: 0 18px;
  border-radius: 8px;
  font-size: 14px;
  border: 1px solid var(--color-border);
  background: var(--color-surface);
  cursor: pointer;
  font-size: 13px;
  color: var(--color-text-secondary);
  display: flex;
  align-items: center;
  gap: 4px;
  transition: all 0.15s;
}
.pag-nav:hover:not(:disabled) {
  border-color: var(--color-accent);
  color: var(--color-accent);
}
.pag-nav:disabled {
  opacity: 0.35;
  cursor: not-allowed;
}
.pag-numbers {
  display: flex;
  align-items: center;
  gap: 4px;
}
.pag-numbers button {
  width: 38px;
  height: 38px;
  border-radius: 50%;
  border: 1px solid var(--color-border);
  background: var(--color-surface);
  cursor: pointer;
  font-size: 15px;
  font-weight: 500;
  color: var(--color-text-secondary);
  display: flex;
  align-items: center;
  justify-content: center;
  transition: all 0.15s;
}
.pag-numbers button:hover {
  border-color: var(--color-accent);
  color: var(--color-accent);
  background: rgba(0, 113, 227, 0.04);
}
.pag-numbers button.active {
  background: var(--color-accent);
  color: #fff;
  border-color: var(--color-accent);
  font-weight: 600;
}
.pag-gap {
  color: var(--color-text-tertiary);
  padding: 0 6px;
  font-size: 13px;
}
.page-loading-overlay {
  position: absolute;
  inset: 0;
  z-index: 50;
  background: rgba(245, 245, 247, 0.6);
  backdrop-filter: blur(2px);
  display: flex;
  align-items: center;
  justify-content: center;
}
.page-loading-spinner {
  width: 40px;
  height: 40px;
  border: 3px solid var(--color-border);
  border-top-color: var(--color-accent);
  border-radius: 50%;
  animation: pag-spin 0.7s linear infinite;
}
@keyframes pag-spin {
  to {
    transform: rotate(360deg);
  }
}

@media (max-width: 640px) {
  .cat-pills {
    gap: 6px;
  }
  .cat-pills button {
    font-size: 12px;
    padding: 6px 14px;
  }
  .article-card {
    padding: 20px;
  }
}
</style>
