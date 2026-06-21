<template>
  <div class="upload-page">
    <div class="page-hero">
      <div class="hero-bg-mesh"></div>
      <div class="section-inner">
        <div class="hero-row">
          <div class="pill">{{ articleStore.getText }}</div>
          <h1>{{ articleStore.id ? "编辑文章" : "发布新文章" }}</h1>
        </div>

        <!-- Type chips -->
        <div class="type-chips">
          <button
            v-for="t in types"
            :key="t.value"
            type="button"
            class="type-chip"
            :class="{ active: article.type === t.value }"
            @click="article.type = t.value"
          >
            {{ t.label }}
          </button>
        </div>
      </div>
    </div>

    <section class="section">
      <div class="section-inner editor-wrap">
        <form @submit.prevent="submitArticle">
          <!-- Title -->
          <div class="title-row">
            <input
              v-model="article.head"
              placeholder="输入文章标题..."
              class="title-input"
              required
            />
          </div>

          <!-- Toolbar -->
          <div class="editor-toolbar" v-if="editor">
            <div class="toolbar-group">
              <button
                type="button"
                @click="editor.chain().focus().toggleBold().run()"
                :class="{ active: editor.isActive('bold') }"
                title="加粗"
              >
                <b>B</b>
              </button>
              <button
                type="button"
                @click="editor.chain().focus().toggleItalic().run()"
                :class="{ active: editor.isActive('italic') }"
                title="斜体"
              >
                <i>I</i>
              </button>
              <button
                type="button"
                @click="
                  editor.chain().focus().toggleHeading({ level: 2 }).run()
                "
                :class="{ active: editor.isActive('heading', { level: 2 }) }"
                title="标题"
              >
                H
              </button>
            </div>

            <span class="toolbar-divider"></span>

            <div class="toolbar-group">
              <button
                type="button"
                @click="editor.chain().focus().toggleBulletList().run()"
                :class="{ active: editor.isActive('bulletList') }"
                title="无序列表"
              >
                <svg width="16" height="16" viewBox="0 0 16 16" fill="none">
                  <path
                    d="M5 5h8M5 8h8M5 11h8M2 5h.01M2 8h.01M2 11h.01"
                    stroke="currentColor"
                    stroke-width="1.5"
                    stroke-linecap="round"
                  />
                </svg>
              </button>
              <button
                type="button"
                @click="editor.chain().focus().toggleOrderedList().run()"
                :class="{ active: editor.isActive('orderedList') }"
                title="有序列表"
              >
                <svg width="16" height="16" viewBox="0 0 16 16" fill="none">
                  <path
                    d="M7 5h7M7 8h7M7 11h7M3 4v2m1 2H2l1-1V6m-1 6h1v.5H2"
                    stroke="currentColor"
                    stroke-width="1.5"
                    stroke-linecap="round"
                  />
                </svg>
              </button>
              <button
                type="button"
                @click="editor.chain().focus().toggleCodeBlock().run()"
                :class="{ active: editor.isActive('codeBlock') }"
                title="代码块"
              >
                <svg width="16" height="16" viewBox="0 0 16 16" fill="none">
                  <path
                    d="M5 5L2 8l3 3m6-6l3 3-3 3"
                    stroke="currentColor"
                    stroke-width="1.5"
                    stroke-linecap="round"
                    stroke-linejoin="round"
                  />
                </svg>
              </button>
            </div>

            <span class="toolbar-divider"></span>

            <div class="toolbar-group">
              <button
                type="button"
                @click="editor.chain().focus().setTextAlign('left').run()"
                :class="{ active: editor.isActive({ textAlign: 'left' }) }"
                title="左对齐"
              >
                <svg width="16" height="16" viewBox="0 0 16 16" fill="none">
                  <path
                    d="M2 3h8M2 7h12M2 11h6"
                    stroke="currentColor"
                    stroke-width="1.5"
                    stroke-linecap="round"
                  />
                </svg>
              </button>
              <button
                type="button"
                @click="editor.chain().focus().setTextAlign('center').run()"
                :class="{ active: editor.isActive({ textAlign: 'center' }) }"
                title="居中"
              >
                <svg width="16" height="16" viewBox="0 0 16 16" fill="none">
                  <path
                    d="M4 3h8M2 7h12M5 11h6"
                    stroke="currentColor"
                    stroke-width="1.5"
                    stroke-linecap="round"
                  />
                </svg>
              </button>
              <button
                type="button"
                @click="editor.chain().focus().setTextAlign('right').run()"
                :class="{ active: editor.isActive({ textAlign: 'right' }) }"
                title="右对齐"
              >
                <svg width="16" height="16" viewBox="0 0 16 16" fill="none">
                  <path
                    d="M6 3h8M2 7h12M8 11h6"
                    stroke="currentColor"
                    stroke-width="1.5"
                    stroke-linecap="round"
                  />
                </svg>
              </button>
            </div>

            <span class="toolbar-divider"></span>

            <div class="toolbar-group">
              <select
                class="font-size-select"
                @change="setFontSize($event.target.value)"
                title="字号"
              >
                <option value="0">正文</option>
                <option value="1">H1</option>
                <option value="2">H2</option>
                <option value="3">H3</option>
                <option value="4">H4</option>
                <option value="5">H5</option>
                <option value="6">H6</option>
                <option value="7">H7</option>
              </select>
            </div>

            <span class="toolbar-divider"></span>

            <div class="toolbar-group">
              <button type="button" @click="insertImage" title="插入图片">
                <svg width="16" height="16" viewBox="0 0 16 16" fill="none">
                  <rect
                    x="2"
                    y="3"
                    width="12"
                    height="10"
                    rx="1.5"
                    stroke="currentColor"
                    stroke-width="1.5"
                  />
                  <circle
                    cx="6"
                    cy="6.5"
                    r="1.5"
                    stroke="currentColor"
                    stroke-width="1.5"
                  />
                  <path
                    d="M2 11.5l3-3 2 2 4-4 2 2"
                    stroke="currentColor"
                    stroke-width="1.5"
                    stroke-linecap="round"
                    stroke-linejoin="round"
                  />
                </svg>
              </button>
              <button
                type="button"
                @click="addLink"
                :class="{ active: editor.isActive('link') }"
                title="链接"
              >
                <svg width="16" height="16" viewBox="0 0 16 16" fill="none">
                  <path
                    d="M6.5 9.5a3 3 0 004.24 0l2-2a3 3 0 00-4.24-4.24L8.5 3M9.5 6.5a3 3 0 00-4.24 0l-2 2a3 3 0 004.24 4.24L7.5 13"
                    stroke="currentColor"
                    stroke-width="1.5"
                    stroke-linecap="round"
                  />
                </svg>
              </button>
            </div>

            <template v-if="editor.isActive('image')">
              <span class="toolbar-divider"></span>
              <div class="toolbar-group">
                <button
                  type="button"
                  class="btn-del-img"
                  @click="deleteSelectedImage"
                  title="删除选中图片"
                >
                  <svg width="16" height="16" viewBox="0 0 16 16" fill="none">
                    <path
                      d="M3 4h10M5.5 4V3a1 1 0 011-1h3a1 1 0 011 1v1M5 4v8a1 1 0 001 1h4a1 1 0 001-1V4"
                      stroke="currentColor"
                      stroke-width="1.4"
                      stroke-linecap="round"
                    />
                  </svg>
                </button>
              </div>
            </template>
          </div>

          <!-- Editor -->
          <div class="editor-area">
            <editor-content
              :editor="editor"
              class="rich-editor"
              @keydown.tab.prevent="insertTab"
            />
          </div>

          <input
            ref="fileInput"
            type="file"
            accept="image/*"
            hidden
            @change="handleImageUpload"
          />

          <!-- Submit -->
          <div class="submit-bar">
            <button type="submit" class="btn-publish" :disabled="submitting">
              <span v-if="submitting" class="btn-spinner"></span>
              <svg
                v-else
                width="18"
                height="18"
                viewBox="0 0 18 18"
                fill="none"
              >
                <path
                  d="M17 1L9 17l-3-6-5-2L17 1z"
                  stroke="currentColor"
                  stroke-width="1.5"
                  stroke-linejoin="round"
                />
              </svg>
              {{ submitting ? "发布中..." : articleStore.getText }}
            </button>
          </div>
        </form>
      </div>
    </section>
  </div>
</template>

<script setup>
import { reactive, onMounted, ref } from "vue";
import { useRouter } from "vue-router";
import { useEditor, EditorContent } from "@tiptap/vue-3";
import StarterKit from "@tiptap/starter-kit";
import Image from "@tiptap/extension-image";
import Link from "@tiptap/extension-link";
import Placeholder from "@tiptap/extension-placeholder";
import TextAlign from "@tiptap/extension-text-align";
import { useArticleStore } from "@/stores/updateArticle";
import {
  upload,
  update,
  uploadArticleImageBatch,
  deleteArticleImageBatch,
} from "@/request/axiosForArticles";
import { ElMessage, ElMessageBox } from "element-plus";

const router = useRouter();
const articleStore = useArticleStore();
const fileInput = ref(null);
const submitting = ref(false);
const pendingImages = new Map();
const existingImageIds = ref([]);
const fontSize = ref("17px");

function applyInline(tag, attr) {
  var e = editor.value;
  if (!e) return;
  var from = e.state.selection.from,
    to = e.state.selection.to;
  if (from === to) return;
  var txt = e.state.doc.textBetween(from, to);
  if (!txt) return;
  e.chain()
    .focus()
    .deleteSelection()
    .insertContent("<" + tag + " " + attr + ">" + txt + "</" + tag + ">")
    .run();
}
function setFontSize(level) {
  fontSize.value = level;
  var e = editor.value;
  if (!e) return;
  var lv = Number(level);
  if (!lv) {
    e.chain().focus().setParagraph().run();
    return;
  }
  e.chain().focus().toggleHeading({ level: lv }).run();
}

const types = [
  { label: "C/C++", value: "1" },
  { label: "前端", value: "2" },
  { label: "数据结构与算法", value: "3" },
  { label: "MySQL数据库", value: "4" },
  { label: "Java", value: "5" },
  { label: "Python/AI", value: "6" },
];

const article = reactive({ id: "", type: "1", head: "", content: "" });

const editor = useEditor({
  content: "",
  extensions: [
    StarterKit.configure({
      heading: { levels: [1, 2, 3, 4, 5, 6, 7] },
      codeBlock: { HTMLAttributes: { class: "code-block" } },
    }),
    Image.configure({ inline: false }),
    Link.configure({ openOnClick: false }),
    TextAlign.configure({ types: ["heading", "paragraph"] }),
    Placeholder.configure({
      placeholder: "开始写作...支持 Markdown 语法和代码块",
    }),
  ],
});

async function addLink() {
  var e = editor.value;
  if (!e) return;

  var existing = e.getAttributes("link").href || "";
  var hasSelection = !e.state.selection.empty;

  try {
    var result = await ElMessageBox.prompt("请输入链接地址", "添加链接", {
      confirmButtonText: "确定",
      cancelButtonText: "取消",
      inputValue: existing,
      inputPlaceholder: "https://example.com",
    });
    var url = result.value || result;
    e.commands.focus();
    if (!url) {
      e.chain().focus().unsetLink().run();
      return;
    }
    var href = /^https?:\/\//.test(url) ? url : "https://" + url;
    if (hasSelection) {
      e.chain().focus().toggleLink({ href: href }).run();
    } else {
      e.chain()
        .focus()
        .insertContent('<a href="' + href + '" target="_blank">' + url + "</a>")
        .run();
    }
  } catch (err) {}
}

function deleteSelectedImage() {
  if (!editor.value?.isActive("image")) return;
  const sel = editor.value.state.selection;
  const node = sel.$anchor.node(sel.$anchor.depth);
  const src = node?.attrs?.src;
  if (src && pendingImages.has(src)) {
    URL.revokeObjectURL(src);
    pendingImages.delete(src);
  }
  editor.value.chain().focus().deleteSelection().run();
}

function insertTab() {
  editor.value?.chain().focus().insertContent("        ").run();
}

function insertImage() {
  fileInput.value?.click();
}

function handleImageUpload(e) {
  const file = e.target.files?.[0];
  if (!file) return;
  const objectUrl = URL.createObjectURL(file);
  pendingImages.set(objectUrl, file);
  editor.value?.chain().focus().setImage({ src: objectUrl }).run();
  e.target.value = "";
}

function extractPendingImages(html) {
  const re = /<img[^>]+src="(blob:[^"]+)"/g;
  const result = [];
  let m;
  while ((m = re.exec(html)) !== null) {
    const file = pendingImages.get(m[1]);
    if (file) result.push({ full: m[0], src: m[1], file });
  }
  return result;
}

async function submitArticle() {
  const content = editor.value?.getHTML() || "";
  const plainText = editor.value?.getText() || "";
  if (!content || plainText.length < 5) {
    ElMessage.error("文章内容太少");
    return;
  }
  if (plainText.length > 8000) {
    ElMessage.error("内容太多了");
    return;
  }

  submitting.value = true;

  const images = extractPendingImages(content);
  let finalContent = content;
  if (images.length > 0) {
    const fd = new FormData();
    images.forEach((img) => fd.append("files", img.file));
    const resp = await uploadArticleImageBatch(fd);
    if (resp?.code === "200" && resp.data) {
      images.forEach((img, idx) => {
        const item = resp.data[idx];
        if (item && item.fileUrl) {
          finalContent = finalContent.replace(img.src, item.fileUrl);
        }
      });
    }
    images.forEach((img) => {
      URL.revokeObjectURL(img.src);
      pendingImages.delete(img.src);
    });
  }

  if (article.id && existingImageIds.value.length > 0) {
    const re = /objectName=(\d+)/g;
    const newIds = new Set();
    let m;
    while ((m = re.exec(finalContent)) !== null) newIds.add(Number(m[1]));
    const removedIds = existingImageIds.value.filter((id) => !newIds.has(id));
    if (removedIds.length > 0) {
      deleteArticleImageBatch(removedIds).catch(() => {});
    }
  }

  article.content = finalContent;

  const resp = article.id ? await update(article) : await upload(article);
  if (resp?.code === "200") {
    ElMessage.success("文章发布成功");
    setTimeout(() => router.push("/tech-study"), 700);
  }
  submitting.value = false;
}

onMounted(() => {
  if (articleStore.id != null) {
    article.id = articleStore.id;
    article.head = articleStore.head;
    article.type = articleStore.type;
    if (articleStore.content) {
      editor.value?.commands.setContent(articleStore.content);
      const re = /objectName=(\d+)/g;
      let m;
      while ((m = re.exec(articleStore.content)) !== null) {
        existingImageIds.value.push(Number(m[1]));
      }
    }
  }
});
</script>

<style scoped>
/* ── Page ── */
.upload-page {
  padding-top: var(--nav-height);
  height: 100vh;
  display: flex;
  flex-direction: column;
  overflow: hidden;
  background: var(--color-bg);
}

/* ── Hero ── */
.page-hero {
  position: relative;
  padding: 28px 0 20px;
  background: linear-gradient(180deg, #f5f0ff 0%, #f5f5f7 100%);
  flex-shrink: 0;
}
.hero-bg-mesh {
  position: absolute;
  inset: 0;
  background: radial-gradient(
    ellipse 60% 50% at 50% 100%,
    rgba(88, 86, 214, 0.04) 0%,
    transparent 70%
  );
  pointer-events: none;
}
.hero-row {
  display: flex;
  align-items: center;
  gap: 16px;
}
.pill {
  display: inline-flex;
  font-size: 12px;
  font-weight: 600;
  color: #5856d6;
  background: rgba(88, 86, 214, 0.08);
  padding: 5px 14px;
  border-radius: 980px;
}
.page-hero h1 {
  font-family: var(--font-heading);
  font-size: 22px;
  font-weight: 700;
  letter-spacing: -0.5px;
  margin: 0;
}

/* ── Type chips ── */
.type-chips {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  margin-top: 18px;
  padding-top: 16px;
  border-top: 1px solid rgba(0, 0, 0, 0.06);
}
.type-chip {
  padding: 7px 18px;
  border-radius: 980px;
  border: 1.5px solid var(--color-border);
  background: var(--color-surface);
  font-size: 13px;
  font-weight: 500;
  cursor: pointer;
  transition: all 0.2s;
  color: var(--color-text-secondary);
}
.type-chip:hover {
  border-color: var(--color-accent);
  color: var(--color-accent);
}
.type-chip.active {
  background: var(--color-accent);
  color: #fff;
  border-color: var(--color-accent);
  box-shadow: 0 2px 10px rgba(0, 113, 227, 0.25);
}

/* ── Section / layout ── */
.section {
  flex: 1;
  display: flex;
  flex-direction: column;
  overflow: hidden;
}
.editor-wrap {
  max-width: 1080px;
  width: 100%;
  display: flex;
  flex-direction: column;
  flex: 1;
  overflow: hidden;
  padding: 0 0 20px;
}
form {
  display: flex;
  flex-direction: column;
  flex: 1;
  overflow: hidden;
}

/* ── Title ── */
.title-row {
  padding: 20px 0 16px;
  flex-shrink: 0;
}
.title-input {
  width: 100%;
  padding: 12px 0;
  border: none;
  border-bottom: 2px solid var(--color-border);
  font-size: 28px;
  font-weight: 700;
  outline: none;
  transition: border-color 0.2s;
  font-family: var(--font-heading);
  letter-spacing: -0.5px;
  background: transparent;
  border-radius: 0;
}
.title-input::placeholder {
  color: var(--color-text-tertiary);
}
.title-input:focus {
  border-color: var(--color-accent);
}

/* ── Toolbar ── */
.editor-toolbar {
  display: flex;
  align-items: center;
  gap: 4px;
  padding: 8px 16px;
  border: 1px solid var(--color-border);
  border-radius: var(--radius-sm);
  background: var(--color-surface);
  box-shadow: var(--shadow-sm);
  flex-shrink: 0;
  flex-wrap: wrap;
}
.toolbar-group {
  display: flex;
  align-items: center;
  gap: 2px;
}
.toolbar-divider {
  width: 1px;
  height: 22px;
  background: var(--color-border);
  margin: 0 6px;
}
.editor-toolbar button {
  width: 34px;
  height: 34px;
  border-radius: 8px;
  border: none;
  background: transparent;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  color: var(--color-text-secondary);
  font-size: 15px;
  font-weight: 600;
  transition: all 0.15s;
}
.editor-toolbar button:hover {
  background: var(--color-bg);
  color: var(--color-text);
}
.editor-toolbar button.active {
  background: var(--color-accent);
  color: #fff;
}
.btn-del-img {
  color: #ff3b30 !important;
}
.btn-del-img:hover {
  background: rgba(255, 59, 48, 0.1) !important;
  color: #ff3b30 !important;
}
.font-size-select {
  height: 28px;
  padding: 0 6px;
  border: 1.5px solid var(--color-border);
  border-radius: 6px;
  font-size: 13px;
  cursor: pointer;
  background: var(--color-surface);
  color: var(--color-text-secondary);
  outline: none;
}
.font-size-select:focus {
  border-color: var(--color-accent);
}

/* ── Editor ── */
.editor-area {
  flex: 1;
  display: flex;
  flex-direction: column;
  overflow: hidden;
  margin-top: 10px;
}
.rich-editor {
  flex: 1;
  min-height: 0;
  padding: 20px 28px;
  display: flex;
  flex-direction: column;
  border: 1px solid var(--color-border);
  border-radius: var(--radius-sm);
  background: var(--color-surface);
  font-size: 20px;
  line-height: 1.9;
  cursor: text;
  font-family: var(--font-body);
  overflow-y: auto;
  color: var(--color-text);
  box-shadow: var(--shadow-sm);
}
.rich-editor :deep(.ProseMirror) {
  flex: 1;
  min-height: 100%;
  outline: none;
}
.rich-editor::-webkit-scrollbar {
  width: 5px;
}
.rich-editor::-webkit-scrollbar-thumb {
  background: var(--color-border);
  border-radius: 3px;
}

.rich-editor :deep(h1) {
  font-family: var(--font-heading);
  font-size: 50px;
  font-weight: 400;
  margin: 36px 0 20px;
  color: var(--color-text);
  letter-spacing: -0.5px;
}
.rich-editor :deep(h2) {
  font-family: var(--font-heading);
  font-size: 45px;
  font-weight: 400;
  margin: 28px 0 12px;
  color: var(--color-text);
}
.rich-editor :deep(h3) {
  font-family: var(--font-heading);
  font-size: 40px;
  font-weight: 400;
  margin: 22px 0 10px;
}
.rich-editor :deep(h4) {
  font-family: var(--font-heading);
  font-size: 35px;
  font-weight: 400;
  margin: 18px 0 8px;
}
.rich-editor :deep(h5) {
  font-family: var(--font-heading);
  font-size: 30px;
  font-weight: 400;
  margin: 14px 0 6px;
}
.rich-editor :deep(h6) {
  font-family: var(--font-heading);
  font-size: 25px;
  font-weight: 400;
  color: var(--color-text-secondary);
  margin: 12px 0 6px;
}
.rich-editor :deep(h7) {
  font-family: var(--font-heading);
  font-size: 20px;
  font-weight: 400;
  color: var(--color-text-secondary);
  margin: 10px 0 4px;
}
.rich-editor :deep(p) {
  margin-bottom: 14px;
}
.rich-editor :deep(strong) {
  font-weight: 700;
  color: var(--color-text);
}
.rich-editor :deep(ul),
.rich-editor :deep(ol) {
  padding-left: 28px;
  margin-bottom: 14px;
}
.rich-editor :deep(li) {
  margin-bottom: 6px;
}
.rich-editor :deep(a) {
  color: var(--color-accent);
  text-decoration: underline;
}

.rich-editor :deep(img) {
  max-width: 100%;
  max-height: 400px;
  width: auto;
  height: auto;
  object-fit: contain;
  border-radius: 10px;
  margin: 16px 0;
  cursor: pointer;
  transition: box-shadow 0.15s;
}
.rich-editor :deep(img.ProseMirror-selectednode) {
  box-shadow:
    0 0 0 3px var(--color-accent),
    0 0 20px rgba(0, 113, 227, 0.15);
}

.rich-editor :deep(pre) {
  background: #1a1a2e;
  color: #e0e0e0;
  padding: 18px 22px;
  border-radius: var(--radius-sm);
  overflow-x: auto;
  font-size: 14px;
  line-height: 1.7;
  margin: 18px 0;
  font-family: "SF Mono", "Fira Code", "JetBrains Mono", monospace;
}
.rich-editor :deep(code) {
  background: rgba(0, 0, 0, 0.05);
  padding: 2px 7px;
  border-radius: 5px;
  font-size: 0.88em;
  font-family: "SF Mono", "Fira Code", "JetBrains Mono", monospace;
}
.rich-editor :deep(pre code) {
  background: none;
  padding: 0;
  font-size: 20px;
}
.rich-editor :deep(blockquote) {
  border-left: 3px solid var(--color-accent);
  padding: 8px 0 8px 20px;
  margin: 16px 0;
  color: var(--color-text-secondary);
  font-style: italic;
}

.rich-editor :deep(p.is-editor-empty:first-child::before) {
  content: attr(data-placeholder);
  float: left;
  color: var(--color-text-tertiary);
  pointer-events: none;
  height: 0;
  font-style: italic;
}

/* ── Submit ── */
.submit-bar {
  display: flex;
  justify-content: center;
  padding: 20px 0 0;
  flex-shrink: 0;
}
.btn-publish {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  background: var(--color-accent);
  color: #fff;
  border: none;
  padding: 12px 36px;
  border-radius: 980px;
  font-size: 16px;
  font-weight: 600;
  cursor: pointer;
  font-family: var(--font-heading);
  transition: all 0.25s var(--easing-spring);
  box-shadow: 0 4px 16px rgba(0, 113, 227, 0.3);
}
.btn-publish:hover:not(:disabled) {
  background: var(--color-accent-light);
  transform: translateY(-1px);
  box-shadow: 0 6px 24px rgba(0, 113, 227, 0.4);
}
.btn-publish:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}
.btn-spinner {
  width: 18px;
  height: 18px;
  border: 2px solid rgba(255, 255, 255, 0.3);
  border-top-color: #fff;
  border-radius: 50%;
  animation: spin 0.6s linear infinite;
  display: inline-block;
}
@keyframes spin {
  to {
    transform: rotate(360deg);
  }
}

@media (max-width: 640px) {
  .editor-toolbar {
    padding: 6px 10px;
    gap: 1px;
  }
  .editor-toolbar button {
    width: 30px;
    height: 30px;
  }
  .rich-editor {
    padding: 16px;
    font-size: 16px;
  }
  .title-input {
    font-size: 22px;
  }
  .type-chips {
    gap: 6px;
  }
  .type-chip {
    font-size: 12px;
    padding: 6px 14px;
  }
}
</style>
