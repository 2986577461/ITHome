package com.xiaoyan.webConfig;

import cn.hutool.json.JSONUtil;
import com.xiaoyan.mapper.StudentFileMapper;
import com.xiaoyan.pojo.Article;
import com.xiaoyan.pojo.Resources;
import com.xiaoyan.pojo.Student;
import com.xiaoyan.service.ArticlesService;
import com.xiaoyan.service.NewcomersService;
import com.xiaoyan.service.ResourcesService;
import com.xiaoyan.service.UsersService;
import com.xiaoyan.utils.RedisUtil;
import lombok.AllArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.xiaoyan.constant.RedisConstant.CACHE_ARTICLES;
import static com.xiaoyan.constant.RedisConstant.CACHE_COUNT_ARTICLES;
import static com.xiaoyan.constant.RedisConstant.CACHE_COUNT_RESOURCES;
import static com.xiaoyan.constant.RedisConstant.CACHE_RESOURCES;
import static com.xiaoyan.constant.RedisConstant.CACHE_STUDENTS;


@Component
@AllArgsConstructor
public class CacheWarmUpRunner implements CommandLineRunner {

    private ArticlesService articlesService;
    private RedisUtil redisUtil;
    private ResourcesService resourcesService;
    private StringRedisTemplate stringRedisTemplate;
    private UsersService usersService;
    private StudentFileMapper studentFileMapper;
    private NewcomersService newcomersService;

    @Override
    public void run(String... args) {
        System.out.println("🚀 缓存预热任务开始...");
        loadArticleCount();
        loadResourcesCount();
        loadStudents();
        loadResources();
        loadArticles();
        System.out.println("✅ 缓存预热任务完成");
    }

    private void loadArticles() {
        List<Article> list = articlesService.query().list();
        Map<String, String> map = new HashMap<>();
        for (Article article : list) {
            map.put(String.valueOf(article.getId()), JSONUtil.toJsonStr(article));
        }
        stringRedisTemplate.opsForHash().putAll(CACHE_ARTICLES, map);
    }

    private void loadResources() {
        List<Resources> resources = resourcesService.list();
        HashMap<String, String> map = new HashMap<>();
        resources.forEach(vo -> map.put(String.valueOf(vo.getId()), JSONUtil.toJsonStr(vo)));
        stringRedisTemplate.opsForHash().putAll(CACHE_RESOURCES, map);
    }

    private void loadStudents() {
        Map<String, String> map = new HashMap<>();
        List<Student> students = usersService.list();
        Map<String, String> temporaryMap;
        if (students == null) {
            temporaryMap = Map.of("", "");
        } else {
            for (Student student : students) {
                map.put(String.valueOf(student.getStudentId()), JSONUtil.toJsonStr(student));
            }
            temporaryMap = map;
        }
        stringRedisTemplate.opsForHash().putAll(CACHE_STUDENTS, temporaryMap);
    }

    private void loadResourcesCount() {
        redisUtil.saveWithLogicalExpire(CACHE_COUNT_RESOURCES, resourcesService.count());
    }

    private void loadArticleCount() {
        redisUtil.saveWithLogicalExpire(CACHE_COUNT_ARTICLES, articlesService.count());
    }

}