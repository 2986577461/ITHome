import DOMPurify from "dompurify";

// AI 输出、Markdown 和数据库中的文章都不是可信 HTML。
// 不允许脚本、事件属性、内联样式、表单、iframe、SVG 或 MathML。
export function sanitizeHtml(html) {
  return DOMPurify.sanitize(html || "", {
    ALLOWED_TAGS: [
      "p", "br", "h1", "h2", "h3", "h4", "h5", "h6",
      "strong", "b", "em", "i", "s", "del", "u", "ul", "ol", "li",
      "pre", "code", "blockquote", "a", "img", "hr", "span",
      "table", "thead", "tbody", "tr", "th", "td",
    ],
    ALLOWED_ATTR: [
      "href", "src", "alt", "title", "class", "width", "height",
      "colspan", "rowspan", "start",
    ],
    ALLOW_DATA_ATTR: false,
    ALLOW_ARIA_ATTR: false,
  });
}
