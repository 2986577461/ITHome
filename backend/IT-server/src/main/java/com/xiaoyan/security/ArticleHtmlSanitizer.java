package com.xiaoyan.security;

import com.xiaoyan.exception.ParameterException;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.safety.Safelist;

/**
 * Article HTML is untrusted, whether supplied by an editor or an Agent.
 * Keep this policy aligned with frontend/src/utils/sanitizeHtml.js.
 * No CSS, event handlers, embedded documents, SVG/MathML or executable URLs.
 * External HTTP(S) links/images remain allowed; this is not content moderation.
 */
public final class ArticleHtmlSanitizer {
    private static final int MAX_HTML_LENGTH = 5000;
    private static final Safelist POLICY = Safelist.none()
            .addTags("p", "br", "h1", "h2", "h3", "h4", "h5", "h6",
                    "strong", "b", "em", "i", "s", "del", "u", "ul", "ol", "li",
                    "pre", "code", "blockquote", "a", "img", "hr", "span",
                    "table", "thead", "tbody", "tr", "th", "td")
            .addAttributes(":all", "class", "title")
            .addAttributes("a", "href")
            .addAttributes("img", "src", "alt", "width", "height")
            .addAttributes("th", "colspan", "rowspan")
            .addAttributes("td", "colspan", "rowspan")
            .addAttributes("ol", "start")
            .addProtocols("a", "href", "http", "https", "mailto", "#")
            .addProtocols("img", "src", "http", "https");

    private ArticleHtmlSanitizer() {
    }

    public static String sanitize(String html) {
        if (html == null || html.length() > MAX_HTML_LENGTH) {
            throw new ParameterException("文章正文不能为空，且不能超过5000字符");
        }
        // Disable pretty printing: preserve whitespace in pre/code, avoid adding layout whitespace.
        String clean = Jsoup.clean(html, "", POLICY,
                new Document.OutputSettings().prettyPrint(false));
        Document parsed = Jsoup.parseBodyFragment(clean);
        String text = parsed.body().text().replace('\u00a0', ' ').strip();
        if (text.isBlank() && parsed.select("img[src]").isEmpty()) {
            throw new ParameterException("文章正文清洗后为空，请提供有效内容");
        }
        // Escaping/normalization can expand the serialized HTML beyond the input's size.
        if (clean.length() > MAX_HTML_LENGTH) {
            throw new ParameterException("清洗后的文章正文不能超过5000字符");
        }
        return clean;
    }
}
