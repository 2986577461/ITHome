package com.xiaoyan.utils;

import com.xiaoyan.properties.LocalFileProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class LocalFileStorageTest {

    @TempDir
    Path tempDir;

    private LocalFileStorage storage;

    /** 根目录故意指向一个还不存在的子目录，顺便验证自动建目录 */
    @BeforeEach
    void setUp() {
        LocalFileProperties properties = new LocalFileProperties();
        properties.setLocalDir(tempDir.resolve("files").toString());
        storage = new LocalFileStorage(properties);
        storage.afterPropertiesSet();
    }

    private byte[] bytes(String content) {
        return content.getBytes(StandardCharsets.UTF_8);
    }

    /* ====================================================
     * 正常读写
     * ==================================================== */

    @Nested
    class RoundTrip {

        @Test
        void saveThenReadReturnsSameContent() {
            storage.save("abc-123.txt", bytes("hello"));

            assertArrayEquals(bytes("hello"), storage.read("abc-123.txt"));
        }

        @Test
        void saveCreatesBaseDirOnFirstWrite() throws IOException {
            assertFalse(Files.exists(tempDir.resolve("files").resolve("sub")));

            storage.save("sub/abc.txt", bytes("x"));

            assertTrue(Files.isRegularFile(tempDir.resolve("files").resolve("sub").resolve("abc.txt")));
        }

        @Test
        void saveOverwritesExistingFile() {
            storage.save("a.txt", bytes("first"));
            storage.save("a.txt", bytes("second"));

            assertArrayEquals(bytes("second"), storage.read("a.txt"));
        }

        @Test
        void deleteRemovesFile() {
            storage.save("a.txt", bytes("x"));

            assertTrue(storage.delete("a.txt"));
            assertFalse(Files.exists(tempDir.resolve("files").resolve("a.txt")));
        }
    }

    /* ====================================================
     * 缺失的文件：一律安静处理，不抛异常
     * ==================================================== */

    @Nested
    class MissingFile {

        @Test
        void readReturnsNullWhenAbsent() {
            assertNull(storage.read("nope.txt"));
        }

        /** 补传任务每轮都会扫一遍，删一个已经被删掉的文件不该让整轮挂掉 */
        @Test
        void deleteReturnsFalseWhenAbsent() {
            assertFalse(storage.delete("nope.txt"));
        }
    }

    /* ====================================================
     * 路径穿越
     *
     * objectName 上传时来自用户文件名、下载时直接来自请求参数，都不能信。
     * ==================================================== */

    @Nested
    class PathTraversal {

        @Test
        void resolveRejectsParentTraversal() {
            assertNull(storage.resolve("../../etc/passwd"));
        }

        /** 规范化之后还在根目录内的相对路径是合法的，不该一刀切拒掉 */
        @Test
        void resolveKeepsPathInsideBaseDir() {
            Path resolved = storage.resolve("sub/../a.txt");

            assertNotNull(resolved);
            assertEquals(tempDir.resolve("files").resolve("a.txt"), resolved);
        }

        @Test
        void resolveRejectsAbsolutePathOutsideBaseDir() {
            assertNull(storage.resolve("/etc/passwd"));
        }

        @Test
        void resolveRejectsBlankName() {
            assertNull(storage.resolve(null));
            assertNull(storage.resolve("  "));
        }

        @Test
        void saveThrowsOnTraversal() {
            assertThrows(IllegalArgumentException.class,
                    () -> storage.save("../escaped.txt", bytes("x")));
            assertFalse(Files.exists(tempDir.resolve("escaped.txt")));
        }

        @Test
        void deleteIgnoresTraversal() {
            assertFalse(storage.delete("../../etc/passwd"));
        }
    }

    /* ====================================================
     * 配置缺失
     * ==================================================== */

    @Test
    void failsFastWhenLocalDirNotConfigured() {
        LocalFileProperties blank = new LocalFileProperties();

        // 启动时就炸掉，好过等到第一次 OSS 挂了才发现降级无处可写
        assertThrows(IllegalStateException.class,
                () -> new LocalFileStorage(blank).afterPropertiesSet());
    }
}
