import assert from "node:assert/strict";
import { readFileSync } from "node:fs";
import { test } from "node:test";
import { JSDOM } from "jsdom";
import { marked } from "marked";
import { parse, compileScript, compileTemplate } from "@vue/compiler-sfc";

const dom = new JSDOM("");
globalThis.window = dom.window;
const { sanitizeHtml } = await import("../src/utils/sanitizeHtml.js");

function fragment(html) {
  const template = dom.window.document.createElement("template");
  template.innerHTML = sanitizeHtml(html);
  return template.content;
}

test("removes scripts, handlers, dangerous URLs and embedded active content", () => {
  const result = fragment(`
    <script>alert(1)</script><img src="https://example.com/a.png" onerror="alert(1)">
    <a href="javascript:alert(1)">bad</a><a href="jav&#x61;script:alert(1)">encoded</a>
    <iframe srcdoc="<script>alert(1)</script>"></iframe><object data="x"></object>
    <svg onload="alert(1)"><a xlink:href="javascript:alert(1)">svg</a></svg>
    <math><mtext>math</mtext></math><form><input autofocus onfocus="alert(1)"></form>
    <p style="position:fixed" onclick="alert(1)" data-evil="x">text</p>
  `);
  assert.equal(result.querySelector("script, iframe, object, svg, math, form, input"), null);
  for (const element of result.querySelectorAll("*")) {
    for (const attr of element.attributes) {
      assert.ok(!/^on|^style$|^data-/i.test(attr.name));
      assert.ok(!/javascript:/i.test(attr.value));
    }
  }
});

test("preserves normal articles, highlighted code, links and OSS images", () => {
  const result = fragment(`
    <h2>标题</h2><p><strong>正文</strong></p><ul><li>列表</li></ul>
    <pre class="code-block"><code class="hljs"><span class="hljs-keyword">int</span> x;</code></pre>
    <a href="https://example.com" target="_blank">来源</a>
    <img src="https://bucket.oss-cn-hangzhou.aliyuncs.com/a.png" alt="图片" width="300">
  `);
  assert.equal(result.querySelector("h2").textContent, "标题");
  assert.equal(result.querySelector("code .hljs-keyword").textContent, "int");
  assert.equal(result.querySelector("a").getAttribute("href"), "https://example.com");
  assert.equal(result.querySelector("a").getAttribute("target"), null);
  assert.equal(result.querySelector("img").getAttribute("alt"), "图片");
});

test("sanitizes raw HTML and dangerous links produced by Markdown", () => {
  const result = fragment(marked.parse('[恶意链接](javascript:alert%281%29)\n\n<img src=x onerror=alert(1)>\n\n```html\n<script>alert(1)</script>\n```'));
  assert.equal(result.querySelector("a").getAttribute("href"), null);
  assert.equal(result.querySelector("img").getAttribute("onerror"), null);
  assert.match(result.querySelector("code").textContent, /<script>/);
  assert.equal(result.querySelector("script"), null);
});

test("document preview uses text interpolation and HTML sinks are sanitized", () => {
  const chat = readFileSync(new URL("../src/views/AiDialog.vue", import.meta.url), "utf8");
  const articles = readFileSync(new URL("../src/views/TechStudy.vue", import.meta.url), "utf8");
  assert.match(chat, /class="doc-modal-body">\{\{ docContent \}\}/);
  assert.doesNotMatch(chat, /v-html="docContent"/);
  assert.match(chat, /sanitizeHtml\(marked\.parse\(t\)\)/);
  assert.match(articles, /v-html="sanitizeHtml\(a\.content\)"/);
});

for (const view of ["AiDialog", "TechStudy"]) {
  test(`${view}.vue script and template compile without a production build`, () => {
    const filename = `${view}.vue`;
    const source = readFileSync(new URL(`../src/views/${filename}`, import.meta.url), "utf8");
    const { descriptor, errors } = parse(source, { filename });
    assert.deepEqual(errors, []);
    const script = compileScript(descriptor, { id: view });
    const template = compileTemplate({
      id: view,
      filename,
      source: descriptor.template.content,
      compilerOptions: { bindingMetadata: script.bindings },
    });
    assert.deepEqual(template.errors, []);
  });
}
