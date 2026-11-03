package com.xiaoyan.service.impl;

import com.xiaoyan.context.BaseContext;
import com.xiaoyan.dto.ArticleDTO;
import com.xiaoyan.exception.ParameterException;
import com.xiaoyan.mapper.ArticleMapper;
import com.xiaoyan.pojo.Article;
import com.xiaoyan.service.CommentsService;
import com.xiaoyan.service.CommonService;
import com.xiaoyan.service.LikesService;
import com.xiaoyan.service.UsersService;
import com.xiaoyan.utils.RedisUtil;
import org.jsoup.Jsoup;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import static com.xiaoyan.constant.RedisConstant.CACHE_ARTICLE_PAGES;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class ArticlesServiceImplTest {
    private final ArticleMapper mapper = mock(ArticleMapper.class);
    private final UsersService users = mock(UsersService.class);
    private final RedisUtil redis = mock(RedisUtil.class);
    private final CommonService files = mock(CommonService.class);
    private ArticlesServiceImpl service;

    @BeforeEach
    void setUp() {
        service = spy(new ArticlesServiceImpl(users, mock(LikesService.class),
                mock(CommentsService.class), mapper, redis, files));
        BaseContext.setCurrentStudentId("alice");
    }

    @AfterEach
    void clearContext() {
        BaseContext.removeCurrentId();
    }

    private ArticleDTO input(String html) {
        ArticleDTO dto = new ArticleDTO();
        dto.setId(7L);
        dto.setType(5);
        dto.setHead("Java 接口说明");
        dto.setContent(html);
        return dto;
    }

    private void existingArticle() {
        Article existing = new Article();
        existing.setId(7L);
        existing.setStudentId("alice");
        existing.setContent("<p>原文</p><img src='https://bucket.aliyuncs.com/old.png'>");
        doReturn(existing).when(service).getById(7L);
    }

    @Test
    void uploadPersistsOnlySanitizedHtmlAndKeepsServerDeterminedAuthor() {
        ArticleDTO dto = input("<h2>Java 接口</h2><p onclick='alert(1)'>正文</p><script>alert(2)</script>");
        service.upload(dto);
        ArgumentCaptor<Article> saved = ArgumentCaptor.forClass(Article.class);
        verify(mapper).insert(saved.capture());
        Article article = saved.getValue();
        assertEquals("alice", article.getStudentId());
        assertEquals(dto.getHead(), article.getHead());
        assertEquals(dto.getType(), article.getType());
        assertNotNull(article.getReleaseDateTime());
        assertTrue(Jsoup.parseBodyFragment(article.getContent()).select("script,[onclick]").isEmpty());
        assertTrue(article.getContent().contains("正文"));
        assertTrue(dto.getContent().contains("<script>")); // Do not mutate incoming DTO.
        verify(redis).evict(CACHE_ARTICLE_PAGES);
    }

    @Test
    void uploadRejectsEmptyCleanedContentBeforeAnyWriteOrCacheInvalidation() {
        assertThrows(ParameterException.class,
                () -> service.upload(input("<script>alert('hello')</script>")));
        verifyNoInteractions(mapper, redis, files);
    }

    @Test
    void updateChecksOwnershipAndPersistsSanitizedContent() {
        existingArticle();
        when(mapper.updateById(any(Article.class))).thenReturn(1);
        service.update(input("<p style='color:red'>更新正文</p>"
                + "<img src='https://bucket.aliyuncs.com/new.png' onerror='alert(1)'>"));
        verify(users).checkOwnerOrAdmin("alice");
        ArgumentCaptor<Article> saved = ArgumentCaptor.forClass(Article.class);
        verify(mapper).updateById(saved.capture());
        assertTrue(Jsoup.parseBodyFragment(saved.getValue().getContent()).select("[style],[onerror]").isEmpty());
        assertTrue(saved.getValue().getContent().contains("new.png"));
        verify(files).delete("old.png");
        verify(redis).evict(CACHE_ARTICLE_PAGES);
    }

    @Test
    void updateRejectsEmptyCleanedContentWithoutWritingOrDeletingFiles() {
        existingArticle();
        assertThrows(ParameterException.class,
                () -> service.update(input("<script>alert('hello')</script>")));
        verify(users).checkOwnerOrAdmin("alice");
        verifyNoInteractions(mapper, redis, files);
    }

    @Test
    void forbiddenUpdateNeverWritesOrDeletesFiles() {
        existingArticle();
        doThrow(new ParameterException("无权限")).when(users).checkOwnerOrAdmin("alice");
        assertThrows(ParameterException.class, () -> service.update(input("<p>更新正文</p>")));
        verifyNoInteractions(mapper, redis, files);
    }
}
