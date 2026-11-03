package com.xiaoyan.security;

import com.xiaoyan.exception.ParameterException;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.*;

class ArticleHtmlSanitizerTest {
    @Test
    void preservesRichTextTablesImagesLinksAndLiteralCode() {
        String html = "<h2>Java 接口</h2><p><strong>重点</strong><em>说明</em></p>"
                + "<ol start='2'><li>第一步</li></ol><blockquote>引用</blockquote>"
                + "<pre class='code-block'><code class='language-java'>if (a &lt; b) {\n  run();\n}</code></pre>"
                + "<pre><code>&lt;script&gt;alert(1)&lt;/script&gt;</code></pre>"
                + "<p><a href='https://example.com/docs'>资料</a>"
                + "<a href='mailto:help@example.com'>邮件</a><a href='#section'>目录</a></p>"
                + "<img src='https://bucket.aliyuncs.com/a.png' alt='图' width='100'>"
                + "<table><tbody><tr><th>标题</th><td colspan='2'>内容</td></tr></tbody></table>";
        String clean = ArticleHtmlSanitizer.sanitize(html);
        Document doc = Jsoup.parseBodyFragment(clean);
        assertEquals("Java 接口", doc.selectFirst("h2").text());
        assertEquals("2", doc.selectFirst("ol").attr("start"));
        assertEquals("language-java", doc.selectFirst("code").className());
        assertEquals("if (a < b) {\n  run();\n}", doc.selectFirst("code").wholeText());
        assertEquals("<script>alert(1)</script>", doc.select("code").get(1).wholeText());
        assertTrue(doc.select("script").isEmpty());
        assertEquals(3, doc.select("a[href]").size());
        assertEquals("https://bucket.aliyuncs.com/a.png", doc.selectFirst("img").attr("src"));
        assertEquals("2", doc.selectFirst("td").attr("colspan"));
        assertEquals(clean, ArticleHtmlSanitizer.sanitize(clean));
    }

    @Test
    void removesActiveTagsEventsStylesAndUnexpectedAttributes() {
        String html = "<p style='color:red' onclick='alert(1)' data-x='1'>安全正文</p>"
                + "<script>alert(1)</script><iframe src='https://evil.example'></iframe>"
                + "<object data='x'></object><embed src='x'>"
                + "<svg onload='alert(1)'><circle/></svg><math><mi>x</mi></math>"
                + "<form action='https://evil.example'><input name='token'></form>"
                + "<img src='https://example.com/a.png' onerror='alert(1)' srcset='x 2x'>";
        Document doc = Jsoup.parseBodyFragment(ArticleHtmlSanitizer.sanitize(html));
        assertTrue(doc.select("script,iframe,object,embed,svg,math,form,input").isEmpty());
        doc.getAllElements().forEach(element -> element.attributes().forEach(attribute -> {
            assertFalse(attribute.getKey().startsWith("on"));
            assertFalse(attribute.getKey().startsWith("data-"));
            assertNotEquals("style", attribute.getKey());
            assertNotEquals("srcset", attribute.getKey());
        }));
        assertTrue(doc.text().contains("安全正文"));
        assertEquals(1, doc.select("img[src]").size());
    }

    @ParameterizedTest
    @ValueSource(strings = {"javascript:alert(1)", "JaVaScRiPt:alert(1)",
            "java&#x73;cript:alert(1)", "java&#x09;script:alert(1)",
            "java&#x0a;script:alert(1)", "data:text/html;base64,PHNjcmlwdD4=",
            "vbscript:msgbox(1)", "//evil.example/a", "/relative/path"})
    void removesUnsafeOrUnresolvedUrls(String url) {
        Document doc = Jsoup.parseBodyFragment(ArticleHtmlSanitizer.sanitize(
                "<p>安全正文</p><a href='" + url + "'>链接</a><img src='" + url + "'>"));
        assertTrue(doc.select("a[href],img[src]").isEmpty());
    }

    @Test
    void malformedAndNestedPayloadsCannotIntroduceExecutableMarkup() {
        String html = "<p>保留正文</p><IMG SRC=javascript:alert(1)>"
                + "<svg><g/onload=alert(2)//<p>"
                + "<math><mtext><table><mglyph><style><!--</style>"
                + "<img title='--><img src=x onerror=alert(3)>'>";
        String clean = ArticleHtmlSanitizer.sanitize(html);
        Document doc = Jsoup.parseBodyFragment(clean);
        assertTrue(doc.select("script,style,svg,math,[onload],[onerror],img[src]").isEmpty());
        assertTrue(doc.text().contains("保留正文"));
        assertEquals(clean, ArticleHtmlSanitizer.sanitize(clean));
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "   ", "<p><br></p>", "<p>&nbsp;</p>",
            "<script>alert('hello')</script>", "<img src='javascript:alert(1)'>"})
    void rejectsEmptyContentAfterCleaning(String html) {
        assertThrows(ParameterException.class, () -> ArticleHtmlSanitizer.sanitize(html));
    }

    @Test
    void acceptsImageOnlyArticleButRejectsMissingOrOversizedContent() {
        assertTrue(ArticleHtmlSanitizer.sanitize("<img src='https://example.com/a.png'>").contains("src="));
        assertThrows(ParameterException.class, () -> ArticleHtmlSanitizer.sanitize(null));
        assertThrows(ParameterException.class, () -> ArticleHtmlSanitizer.sanitize("x".repeat(5001)));
        assertThrows(ParameterException.class, () -> ArticleHtmlSanitizer.sanitize("&".repeat(1100)));
    }
}
