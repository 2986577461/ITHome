<template>
  <div class="ai-page">
    <div class="ai-layout">
      <div v-if="pageLoading" class="ai-loading">
        <div class="ai-loading-spinner"></div>
        <span>加载中...</span>
      </div>
      <template v-else>
        <!-- Sidebar -->
        <aside class="sidebar" :class="{ open: sidebarOpen }">
          <div class="sidebar-header">
            <h1>聊天</h1>
            <button class="btn-new" @click="newConversation()" title="新建对话">
              <svg
                viewBox="0 0 24 24"
                fill="none"
                stroke="currentColor"
                stroke-width="2"
              >
                <line x1="12" y1="5" x2="12" y2="19" />
                <line x1="5" y1="12" x2="19" y2="12" />
              </svg>
            </button>
          </div>
          <div class="conv-list">
            <div
              v-for="conv in conversations"
              :key="conv.thread_id"
              :class="[
                'conv-item',
                { active: conv.thread_id === currentThreadId },
              ]"
              @click="switchConversation(conv.thread_id)"
            >
              <div class="conv-item-title">{{ conv.title }}</div>
              <div class="conv-item-time">
                {{ formatTime(conv.updated_at) }}
              </div>
              <button
                class="btn-del"
                @click.stop="delConversation(conv.thread_id)"
              >
                ×
              </button>
            </div>
            <div
              v-if="conversations.length === 0"
              style="
                padding: 20px;
                text-align: center;
                color: var(--text-muted);
                font-size: 13px;
              "
            >
              暂无对话
            </div>
          </div>
          <div class="kb-section">
            <div class="kb-header">
              📄 知识库
              <button
                class="kb-add-btn"
                @click="triggerUpload()"
                title="添加文档"
              >
                +
              </button>
            </div>
            <div class="kb-list" v-if="kbDocs.length > 0">
              <div class="kb-doc" v-for="d in kbDocs" :key="d.id">
                <span class="kb-doc-name" :title="d.filename">{{
                  d.filename
                }}</span>
                <button class="kb-doc-del" @click="deleteDocument(d.id)">
                  ×
                </button>
              </div>
            </div>
            <div class="kb-empty" v-else>暂无文档</div>
          </div>
        </aside>

        <!-- Main Chat -->
        <main class="chat-area">
          <div
            class="messages"
            ref="msgBox"
            v-if="currentThreadId"
            @scroll="onScroll"
          >
            <div
              v-for="(msg, i) in messages"
              :key="i"
              :class="['msg-wrap', msg.role]"
            >
              <div v-if="msg.role === 'user'" class="user-bubble">
                {{ msg.text }}
              </div>
              <template v-if="msg.role === 'ai'">
                <div
                  class="ai-text"
                  v-html="renderMarkdown(kbBefore(msg))"
                ></div>
                <div
                  v-if="msg.kbResults != null"
                  class="search-link"
                  @click="msg.kbOpen = !msg.kbOpen"
                >
                  Searched the knowledge base
                  <span class="arrow" :class="{ open: msg.kbOpen }">▸</span>
                </div>
                <div
                  v-if="msg.kbOpen && msg.kbResults && msg.kbResults.length > 0"
                  class="search-dropdown"
                >
                  <div
                    v-for="(r, ri) in msg.kbResults"
                    :key="ri"
                    class="search-dropdown-item"
                    style="cursor: default"
                  >
                    <div class="search-dropdown-title">{{ r.title }}</div>
                    <div class="search-dropdown-src">{{ r.snippet }}</div>
                  </div>
                </div>
                <div
                  v-if="
                    msg.kbOpen && (!msg.kbResults || msg.kbResults.length === 0)
                  "
                  class="search-dropdown"
                >
                  <div
                    class="search-dropdown-item"
                    style="
                      cursor: default;
                      color: var(--text-muted);
                      font-size: 13px;
                    "
                  >
                    未找到相关内容
                  </div>
                </div>
                <div
                  v-if="betweenText(msg)"
                  class="ai-text"
                  v-html="renderMarkdown(betweenText(msg))"
                ></div>
                <div
                  v-if="msg.webResults != null && !msg.searching"
                  class="search-link"
                  @click="msg.webOpen = !msg.webOpen"
                >
                  Searched the web
                  <span class="arrow" :class="{ open: msg.webOpen }">▸</span>
                </div>
                <div
                  v-if="
                    msg.webOpen && msg.webResults && msg.webResults.length > 0
                  "
                  class="search-dropdown"
                >
                  <a
                    v-for="(r, ri) in msg.webResults"
                    :key="ri"
                    :href="r.url"
                    target="_blank"
                    class="search-dropdown-item"
                    style="
                      display: block;
                      text-decoration: none;
                      color: inherit;
                    "
                  >
                    <div class="search-dropdown-title">{{ r.title }}</div>
                    <div class="search-dropdown-src">
                      {{ extractDomain(r.url) }}
                    </div>
                  </a>
                </div>
                <div
                  v-if="msg.searching"
                  class="search-link"
                  style="cursor: default"
                >
                  <template v-if="msg.searchType === 'web'"
                    >The web searching...</template
                  >
                  <template v-else-if="msg.searchType === 'kb'"
                    >Searching in the knowledge base...</template
                  >
                  <template v-else>Searching...</template>
                </div>
                <div
                  v-if="afterText(msg)"
                  class="ai-text"
                  v-html="renderMarkdown(afterText(msg))"
                ></div>
                <div class="msg-toolbar" v-if="!msg.streaming">
                  <button class="btn-copy" @click="copyText(msg.text)">
                    <svg
                      viewBox="0 0 24 24"
                      fill="none"
                      stroke="currentColor"
                      stroke-width="2"
                    >
                      <rect x="9" y="9" width="13" height="13" rx="2" />
                      <path
                        d="M5 15H4a2 2 0 01-2-2V4a2 2 0 012-2h9a2 2 0 012 2v1"
                      />
                    </svg>
                  </button>
                </div>
              </template>
            </div>
            <div
              v-if="loadingMessages"
              style="
                text-align: center;
                padding: 20px;
                color: var(--text-muted);
              "
            >
              加载中...
            </div>
          </div>

          <div class="chat-empty" v-if="!currentThreadId">
            <h3>有什么我可以帮忙的？</h3>
            <p>可以问我任何问题，我会搜索知识库或联网查找最新信息。</p>
          </div>

          <div class="input-wrap">
            <div class="input-box">
              <div class="input-inner">
                <button
                  class="btn-upload"
                  title="上传文件"
                  @click="triggerUpload()"
                >
                  <svg
                    viewBox="0 0 24 24"
                    fill="none"
                    stroke="currentColor"
                    stroke-width="2"
                  >
                    <line x1="12" y1="5" x2="12" y2="19" />
                    <line x1="5" y1="12" x2="19" y2="12" />
                  </svg>
                </button>
                <input
                  type="file"
                  ref="fileInput"
                  accept=".txt,.md"
                  @change="onFileSelect"
                  style="display: none"
                  multiple
                />
                <textarea
                  ref="inputBox"
                  v-model="question"
                  @keydown.enter.exact.prevent="send()"
                  placeholder="Write a message..."
                  :disabled="loading"
                  rows="1"
                  @input="autoResize"
                ></textarea>
                <button
                  class="btn-send"
                  @click="send()"
                  :disabled="loading || !question.trim()"
                  title="发送"
                >
                  <span v-if="loading" class="spinner-sm"></span>
                  <svg
                    v-else
                    viewBox="0 0 24 24"
                    fill="none"
                    stroke="currentColor"
                    stroke-width="2"
                  >
                    <line x1="22" y1="2" x2="11" y2="13" />
                    <polygon points="22 2 15 22 11 13 2 9 22 2" />
                  </svg>
                </button>
              </div>
            </div>
          </div>
          <div class="chat-footer">
            Claude is AI and can make mistakes. Please double-check responses.
          </div>
        </main>
      </template>
    </div>

    <div class="toast" :class="{ hide: !toastVisible }">{{ toastMsg }}</div>
    <button
      class="btn-scroll-bottom"
      v-if="showScrollBtn"
      @click="scrollBottom()"
    >
      <svg
        viewBox="0 0 24 24"
        fill="none"
        stroke="currentColor"
        stroke-width="2"
        width="18"
        height="18"
      >
        <polyline points="6 9 12 15 18 9" />
      </svg>
    </button>
  </div>
</template>

<script setup>
import { ref, reactive, nextTick, onMounted, onUnmounted } from "vue";
import { marked } from "marked";
import {
  getConversations,
  getMessages,
  deleteConversation,
  getKbDocuments,
  uploadKbDocument,
  deleteKbDocument,
} from "@/request/axiosForAi.js";

const CHAT_STREAM_URL = "/chat-stream";

const question = ref("");
const messages = ref([]);
const loading = ref(false);
const loadingMessages = ref(false);
const currentThreadId = ref("");
const conversations = ref([]);
const pageLoading = ref(true);
const sidebarOpen = ref(false);
const kbDocs = ref([]);
const showScrollBtn = ref(false);
const toastVisible = ref(false);
const toastMsg = ref("");
const msgBox = ref(null);
const inputBox = ref(null);
const fileInput = ref(null);
let es = null;

function _buildMsg(role, content, searchInfo) {
  const m = reactive({
    role,
    text: content || "",
    streaming: false,
    searching: false,
    searchType: null,
    webResults: null,
    kbResults: null,
    webOpen: false,
    kbOpen: false,
    searchSplitPos: null,
    kbSplitPos: null,
    webSplitPos: null,
    kbMarkerPos: null,
    webMarkerPos: null,
  });
  if (searchInfo) {
    try {
      const si =
        typeof searchInfo === "string" ? JSON.parse(searchInfo) : searchInfo;
      if (si.split_pos != null) m.searchSplitPos = si.split_pos;
      if (si.kb_marker_pos != null) m.kbMarkerPos = si.kb_marker_pos;
      if (si.web_marker_pos != null) m.webMarkerPos = si.web_marker_pos;
      if (si.web) {
        m.webResults = si.web;
        if (!m.searchType) m.searchType = "web";
      }
      if (si.kb) {
        m.kbResults = si.kb;
        if (!m.searchType) m.searchType = "kb";
      }
    } catch {}
  }
  return m;
}

function firstSearchPos(msg) {
  if (msg.kbMarkerPos != null) return msg.kbMarkerPos;
  if (msg.webMarkerPos != null) return msg.webMarkerPos;
  return msg.searchSplitPos;
}
function kbBefore(msg) {
  const p = firstSearchPos(msg);
  return p != null ? msg.text.substring(0, p) : msg.text;
}
function betweenText(msg) {
  const s = msg.kbMarkerPos,
    e = msg.webMarkerPos;
  if (s != null && e != null && e > s) return msg.text.substring(s, e);
  return "";
}
function afterText(msg) {
  let p = null;
  if (msg.webMarkerPos != null) p = msg.webMarkerPos;
  else if (msg.kbMarkerPos != null) p = msg.kbMarkerPos;
  else p = msg.searchSplitPos;
  return p != null ? msg.text.substring(p) : "";
}
function renderMarkdown(t) {
  if (!t) return "";
  return marked.parse(t);
}
function scrollBottom() {
  if (msgBox.value) msgBox.value.scrollTop = msgBox.value.scrollHeight;
}
function onScroll() {
  const b = msgBox.value;
  if (b)
    showScrollBtn.value = b.scrollTop + b.clientHeight < b.scrollHeight - 200;
}
function autoResize() {
  const e = inputBox.value;
  if (!e) return;
  e.style.height = "auto";
  e.style.height = Math.min(e.scrollHeight, 150) + "px";
}
function formatTime(iso) {
  if (!iso) return "";
  const d = new Date(iso),
    n = new Date(),
    dm = Math.floor((n - d) / 6e4);
  if (dm < 1) return "刚刚";
  if (dm < 60) return dm + "m";
  if (dm < 1440) return Math.floor(dm / 60) + "h";
  return `${d.getMonth() + 1}/${d.getDate()}`;
}
function extractDomain(url) {
  try {
    return new URL(url).hostname.replace("www.", "");
  } catch {
    return url;
  }
}
async function copyText(t) {
  try {
    await navigator.clipboard.writeText(t);
    toastMsg.value = "复制成功";
    toastVisible.value = true;
    setTimeout(() => {
      toastVisible.value = false;
    }, 2000);
  } catch {}
}

async function loadConversations() {
  try {
    conversations.value = await getConversations();
  } catch {}
}
async function loadKbDocuments() {
  try {
    kbDocs.value = await getKbDocuments();
  } catch {}
}
function triggerUpload() {
  const inp = fileInput.value;
  if (inp) {
    inp.value = "";
    inp.click();
  }
}
async function onFileSelect(e) {
  for (const f of e.target.files || []) {
    try {
      const buf = await f.arrayBuffer();
      const b64 = btoa(String.fromCharCode(...new Uint8Array(buf)));
      await uploadKbDocument(f.name, b64);
    } catch {}
  }
  e.target.value = "";
  await loadKbDocuments();
}
async function deleteDocument(docId) {
  await deleteKbDocument(docId);
  kbDocs.value = kbDocs.value.filter((d) => d.id !== docId);
}

async function switchConversation(threadId) {
  if (loading.value) stopStream();
  currentThreadId.value = threadId;
  messages.value = [];
  loadingMessages.value = true;
  try {
    const res = await getMessages(threadId);
    messages.value = res.map((m) =>
      _buildMsg(m.role, m.content, m.search_info),
    );
  } catch {
    messages.value = [];
  }
  loadingMessages.value = false;
  nextTick(() => scrollBottom());
  sidebarOpen.value = false;
}
function newConversation() {
  if (loading.value) stopStream();
  currentThreadId.value = "";
  messages.value = [];
  question.value = "";
  nextTick(() => inputBox.value?.focus());
}
async function delConversation(threadId) {
  if (!confirm("删除？")) return;
  try {
    await deleteConversation(threadId);
  } catch {}
  if (currentThreadId.value === threadId) {
    currentThreadId.value = "";
    messages.value = [];
  }
  await loadConversations();
}
function stopStream() {
  if (es) {
    es.close();
    es = null;
  }
  loading.value = false;
}

async function send() {
  const text = question.value.trim();
  if (!text || loading.value) return;

  if (!currentThreadId.value) {
    currentThreadId.value = "session-" + Date.now();
  }
  messages.value.push({ role: "user", text });
  question.value = "";
  autoResize();
  loading.value = true;
  nextTick(() => scrollBottom());

  const aiIdx = messages.value.length;
  messages.value.push(
    reactive({
      role: "ai",
      text: "",
      streaming: true,
      searching: false,
      searchType: null,
      webResults: null,
      kbResults: null,
      webOpen: false,
      kbOpen: false,
      searchSplitPos: null,
    }),
  );

  const token = localStorage.getItem("authorization") || "";
  const url = `${CHAT_STREAM_URL}?question=${encodeURIComponent(text)}&thread_id=${currentThreadId.value}&token=${encodeURIComponent(token)}`;
  const evtSource = new EventSource(url);
  es = evtSource;
  evtSource.onmessage = async (e) => {
    const msg = messages.value[aiIdx];
    if (e.data === "[DONE]") {
      evtSource.close();
      es = null;
      msg.streaming = false;
      msg.searching = false;
      loading.value = false;
      await loadConversations();
      nextTick(() => inputBox.value?.focus());
    } else if (e.data === "[SEARCHING]") {
      msg.searching = true;
      if (msg.searchSplitPos == null) msg.searchSplitPos = msg.text.length;
    } else if (e.data.startsWith("[SEARCH_RESULT]")) {
      msg.searching = false;
      msg.searchType = "web";
      try {
        msg.webResults =
          JSON.parse(e.data.replace("[SEARCH_RESULT]", "")).results || [];
      } catch {}
    } else if (e.data.startsWith("[KB_RESULT]")) {
      msg.searching = false;
      if (msg.searchSplitPos == null) msg.searchSplitPos = msg.text.length;
      try {
        msg.kbResults =
          JSON.parse(e.data.replace("[KB_RESULT]", "")).results || [];
      } catch {}
    } else if (e.data.startsWith("[ERROR]")) {
      evtSource.close();
      es = null;
      msg.text = "出错：" + e.data.replace("[ERROR] ", "");
      msg.streaming = false;
      loading.value = false;
    } else {
      msg.searching = false;
      const t = e.data.replace(/\\n/g, "\n");
      if (t === "[[KB_MARK]]") msg.kbMarkerPos = msg.text.length;
      else if (t === "[[WEB_MARK]]") msg.webMarkerPos = msg.text.length;
      else msg.text += t;
      nextTick(() => scrollBottom());
    }
  };
  evtSource.onerror = () => {
    evtSource.close();
    es = null;
    const msg = messages.value[aiIdx];
    if (msg) {
      msg.streaming = false;
      msg.searching = false;
    }
    loading.value = false;
  };
}

onMounted(async () => {
  await loadConversations();
  await loadKbDocuments();
  pageLoading.value = false;
  if (conversations.value.length > 0)
    switchConversation(conversations.value[0].thread_id);
  msgBox.value?.addEventListener("scroll", onScroll);
});
onUnmounted(() => {
  if (es) {
    es.close();
    es = null;
  }
});
</script>

<style scoped>
.ai-page {
  --primary: #0891b2;
  --primary-hover: #076e86;
  --bg: #ffffff;
  --chat-bg: #fdfdfc;
  --sidebar-bg: #f9fafb;
  --sidebar-border: #e5e7eb;
  --text: #1a1a2e;
  --text-muted: #9ca3af;
  --text-link: #9ca3af;
  --user-bubble: #f3f4f6;
  --border: #e5e7eb;
  --input-shadow: 0 2px 12px rgba(0, 0, 0, 0.06);
  --radius: 24px;
  --radius-sm: 12px;
  padding-top: var(--nav-height);
  height: 100vh;
  overflow: hidden;
  font-family:
    "Inter",
    -apple-system,
    sans-serif;
  background: var(--bg);
  color: var(--text);
  -webkit-font-smoothing: antialiased;
}

.ai-layout {
  display: flex;
  height: 100%;
}
.ai-loading {
  flex: 1;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 16px;
  color: var(--text-muted);
  font-size: 14px;
}
.ai-loading-spinner {
  width: 36px;
  height: 36px;
  border: 3px solid var(--border);
  border-top-color: var(--primary);
  border-radius: 50%;
  animation: ai-spin 0.7s linear infinite;
}
@keyframes ai-spin {
  to {
    transform: rotate(360deg);
  }
}

/* Sidebar */
.sidebar {
  width: 24%;
  min-width: 200px;
  height: 100%;
  background: var(--sidebar-bg);
  border-right: 1px solid var(--sidebar-border);
  display: flex;
  flex-direction: column;
}
.sidebar-header {
  padding: 16px 16px 12px;
  border-bottom: 1px solid var(--border);
  display: flex;
  align-items: center;
  gap: 8px;
}
.sidebar-header h1 {
  font-size: 16px;
  font-weight: 600;
  flex: 1;
}
.btn-new {
  width: 32px;
  height: 32px;
  border: none;
  background: var(--border);
  border-radius: 8px;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: background 0.2s;
}
.btn-new:hover {
  background: #d1d5db;
}
.btn-new svg {
  width: 16px;
  height: 16px;
}

.conv-list {
  flex: 2;
  overflow-y: auto;
  padding: 8px;
}
.conv-item {
  padding: 10px 12px;
  border-radius: 8px;
  cursor: pointer;
  transition: background 0.15s;
  display: flex;
  align-items: center;
  gap: 8px;
}
.conv-item:hover {
  background: #f3f4f6;
}
.conv-item.active {
  background: #e5e7eb;
}
.conv-item-title {
  font-size: 15px;
  font-weight: 500;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
  flex: 1;
}
.conv-item-time {
  font-size: 11px;
  color: var(--text-muted);
}
.btn-del {
  opacity: 0;
  background: none;
  border: none;
  cursor: pointer;
  color: var(--text-muted);
  font-size: 14px;
  padding: 2px 4px;
  border-radius: 4px;
}
.conv-item:hover .btn-del {
  opacity: 1;
}
.btn-del:hover {
  background: #fee2e2;
  color: #ef4444;
}

.kb-section {
  flex: 1;
  margin: 8px 16px;
  border: 1px solid var(--border);
  border-radius: 8px;
  overflow: hidden;
  display: flex;
  flex-direction: column;
}
.kb-header {
  font-size: 13px;
  font-weight: 600;
  color: var(--text-muted);
  padding: 8px 10px;
  background: #fafbfc;
  border-bottom: 1px solid var(--border);
  display: flex;
  align-items: center;
  gap: 4px;
}
.kb-add-btn {
  margin-left: auto;
  background: none;
  border: none;
  cursor: pointer;
  color: var(--text-muted);
  font-size: 16px;
  padding: 0 4px;
  border-radius: 4px;
  line-height: 1;
}
.kb-add-btn:hover {
  background: #e5e7eb;
  color: var(--text);
}
.kb-list {
  flex: 1;
  overflow-y: auto;
  font-size: 20px;
}
.kb-doc {
  padding: 8px 10px;
  display: flex;
  align-items: center;
  gap: 10px;
}
.kb-doc-name {
  flex: 1;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.kb-doc-del {
  background: none;
  border: none;
  cursor: pointer;
  color: var(--text-muted);
  font-size: 15px;
}
.kb-doc-del:hover {
  color: #ef4444;
}
.kb-empty {
  padding: 8px 10px;
  font-size: 11px;
  color: var(--text-muted);
}

/* Chat */
.chat-area {
  flex: 1;
  display: flex;
  flex-direction: column;
  background: #fdfdfc;
  position: relative;
}
.messages {
  flex: 1;
  overflow-y: auto;
  padding: 24px 20px 40px;
  width: 85%;
  margin: 0 auto;
  scrollbar-width: none;
  -ms-overflow-style: none;
}
.messages::-webkit-scrollbar {
  display: none;
}

.msg-wrap {
  margin-bottom: 24px;
}
.msg-wrap.user {
  display: flex;
  justify-content: flex-end;
}
.user-bubble {
  background: var(--user-bubble);
  border-radius: 18px;
  padding: 10px 16px;
  max-width: 75%;
  font-size: 20px;
  line-height: 1.6;
}
.ai-text {
  font-size: 20px;
  line-height: 1.75;
  color: var(--text);
}
.ai-text hr {
  border: none;
  border-top: 1px solid #d1d5db;
  margin: 1em 0;
}

.search-link {
  display: block;
  clear: both;
  width: 100%;
  color: var(--text-link);
  font-size: 14px;
  cursor: pointer;
  user-select: none;
  margin: 12px 0 6px;
  transition: color 0.15s;
}
.search-link:hover {
  color: #1a1a2e;
}
.search-link .arrow {
  font-size: 12px;
  margin-left: 2px;
  transition: transform 0.2s;
  display: inline-block;
}
.search-link .arrow.open {
  transform: rotate(90deg);
}

.search-dropdown {
  margin: 8px 0;
  border: 1px solid var(--border);
  border-radius: 10px;
  overflow: hidden;
  background: #fafbfc;
}
.search-dropdown-item {
  display: block;
  padding: 10px 14px;
  text-decoration: none;
  color: var(--text);
  border-bottom: 1px solid var(--border);
  transition: background 0.15s;
}
.search-dropdown-item:last-child {
  border-bottom: none;
}
.search-dropdown-item:hover {
  background: #f3f4f6;
}
.search-dropdown-title {
  font-size: 13px;
  font-weight: 500;
  line-height: 1.4;
}
.search-dropdown-src {
  font-size: 11px;
  color: var(--text-muted);
  margin-top: 2px;
}

.msg-toolbar {
  display: flex;
  gap: 4px;
  margin-top: 6px;
}
.btn-copy {
  background: none;
  border: none;
  cursor: pointer;
  color: var(--text-muted);
  padding: 10px 12px;
  border-radius: 8px;
  transition: all 0.15s;
  display: flex;
  align-items: center;
}
.btn-copy:hover {
  background: #f3f4f6;
  color: var(--text);
}
.btn-copy svg {
  width: 26px;
  height: 26px;
}

.chat-empty {
  flex: 1;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 40px;
  text-align: center;
}
.chat-empty h3 {
  font-size: 22px;
  font-weight: 600;
  margin-bottom: 8px;
}
.chat-empty p {
  color: var(--text-muted);
  max-width: 400px;
  line-height: 1.6;
}

/* Input */
.input-wrap {
  padding: 0 20px 20px;
  width: 85%;
  margin: 0 auto;
}
.input-box {
  border: 1px solid var(--border);
  border-radius: var(--radius);
  box-shadow: var(--input-shadow);
  background: #fff;
  overflow: hidden;
}
.input-inner {
  display: flex;
  align-items: flex-end;
  gap: 8px;
  padding: 12px 16px;
}
.input-inner textarea {
  flex: 1;
  border: none;
  outline: none;
  resize: none;
  font-size: 15px;
  font-family: inherit;
  line-height: 1.5;
  min-height: 24px;
  max-height: 150px;
  padding: 4px 0;
}
.input-inner textarea::placeholder {
  color: var(--text-muted);
}

.btn-upload {
  width: 36px;
  height: 36px;
  border: none;
  background: none;
  cursor: pointer;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  color: var(--text-muted);
  transition: all 0.15s;
  flex-shrink: 0;
}
.btn-upload:hover {
  background: #f3f4f6;
  color: var(--text);
}
.btn-upload svg {
  width: 20px;
  height: 20px;
}

.btn-send {
  width: 36px;
  height: 36px;
  border: none;
  background: var(--text);
  color: #fff;
  cursor: pointer;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: background 0.15s;
  flex-shrink: 0;
}
.btn-send:hover {
  background: #374151;
}
.btn-send:disabled {
  background: #d1d5db;
  cursor: not-allowed;
}
.btn-send svg {
  width: 16px;
  height: 16px;
}

.spinner-sm {
  width: 16px;
  height: 16px;
  border: 2px solid rgba(255, 255, 255, 0.3);
  border-top-color: #fff;
  border-radius: 50%;
  animation: spin 0.6s linear infinite;
}
@keyframes spin {
  to {
    transform: rotate(360deg);
  }
}

.chat-footer {
  text-align: center;
  padding: 12px;
  font-size: 11px;
  color: var(--text-muted);
  flex-shrink: 0;
}

/* Scroll button */
.btn-scroll-bottom {
  position: fixed;
  bottom: 160px;
  left: 50%;
  transform: translateX(-50%);
  width: 36px;
  height: 36px;
  border-radius: 50%;
  background: rgba(255, 255, 255, 0.9);
  border: 1px solid var(--border);
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.08);
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  color: var(--text-muted);
  z-index: 5;
  transition: all 0.15s;
}
.btn-scroll-bottom:hover {
  background: #fff;
  color: var(--text);
}

/* Toast */
.toast {
  position: fixed;
  top: calc(var(--nav-height) + 20px);
  left: 50%;
  transform: translateX(-50%);
  background: rgba(0, 0, 0, 0.75);
  color: #fff;
  padding: 12px 28px;
  border-radius: 10px;
  font-size: 14px;
  z-index: 100;
  pointer-events: none;
  transition: opacity 0.3s;
  opacity: 1;
}
.toast.hide {
  opacity: 0;
}

@media (max-width: 768px) {
  .sidebar {
    position: fixed;
    left: 0;
    top: var(--nav-height);
    z-index: 10;
    transform: translateX(-100%);
    transition: transform 0.2s;
    box-shadow: 0 0 30px rgba(0, 0, 0, 0.1);
  }
  .sidebar.open {
    transform: translateX(0);
  }
  .messages {
    padding: 16px;
    width: 100%;
  }
  .input-wrap {
    padding: 0 12px 12px;
    width: 100%;
  }
}
</style>
