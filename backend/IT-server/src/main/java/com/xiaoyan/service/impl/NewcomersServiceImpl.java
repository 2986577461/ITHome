package com.xiaoyan.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.xiaoyan.constant.MessageConstant;
import com.xiaoyan.constant.PasswordConstant;
import com.xiaoyan.constant.PositionConstant;
import com.xiaoyan.exception.ParameterException;

import com.xiaoyan.mapper.NewcomerMapper;
import com.xiaoyan.mapper.UserMapper;
import com.xiaoyan.pojo.Student;
import com.xiaoyan.service.NewcomersService;
import com.xiaoyan.service.UsersService;
import com.xiaoyan.utils.RedisUtil;
import com.xiaoyan.vo.NewcomerVO;
import com.xiaoyan.vo.StudentVO;
import lombok.AllArgsConstructor;
import lombok.NonNull;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.validation.annotation.Validated;
import com.xiaoyan.pojo.Newcomer;

import java.time.LocalDateTime;
import java.util.List;

import static com.xiaoyan.constant.RedisConstant.CACHE_NEWCOMERS;

/**
 * @author yuchao
 */
@Service
@AllArgsConstructor
@Validated
public class NewcomersServiceImpl extends ServiceImpl<NewcomerMapper, Newcomer>
        implements NewcomersService {

    private static final BCryptPasswordEncoder ENCODER = new BCryptPasswordEncoder();
    private final UsersService usersService;
    private final NewcomerMapper newcomerMapper;

    private StringRedisTemplate stringRedisTemplate;
    private TransactionTemplate transactionTemplate;
    private RedisUtil redisUtil;
    private UserMapper userMapper;

    @Override
    public void agreeNewcomer(Long id) {
        transactionTemplate.execute(status -> {
            Newcomer newcomer = redisUtil.queryHashWithMutex(CACHE_NEWCOMERS, String.valueOf(id),
                    Newcomer.class, this::getById);

            if (newcomer == null) {
                throw new ParameterException(MessageConstant.ACCOUNT_NOT_FOUND);
            }

            Integer studentId = newcomer.getStudentId();

            StudentVO oldStudent = usersService.getUser(studentId);
            if (oldStudent != null) {
                throw new ParameterException(MessageConstant.REPEATREQUEST);
            }

            this.removeById(id);
            stringRedisTemplate.opsForHash().delete(CACHE_NEWCOMERS, String.valueOf(id));

            Student student = BeanUtil.toBean(newcomer, Student.class);
            student.setPassword(ENCODER.encode(PasswordConstant.STUDENT_PASSWORD));
            student.setPosition(PositionConstant.STUDENT);
            student.setAvatarId(1L);

            userMapper.insert(student);
            return null;
        });
    }

    @Override
    public void applyJoin(@NonNull Newcomer newComer) {
        Integer studentId = newComer.getStudentId();

        Newcomer dbNewComer = newcomerMapper.selectByStudentId(studentId);

        if (dbNewComer != null) {
            throw new RuntimeException(MessageConstant.REPEATREQUEST);
        }

        newComer.setApplicationDateTime(LocalDateTime.now());
        if (!this.save(newComer)) {
            throw new RuntimeException(MessageConstant.PARAMETER_ERROR);
        }
        stringRedisTemplate.opsForHash().put(CACHE_NEWCOMERS, String.valueOf(newComer.getId()),
                JSONUtil.toJsonStr(newComer));

    }

    @Override
    public List<Newcomer> getAll() {
        return redisUtil.getAllWithHashCache(CACHE_NEWCOMERS, this::count,
                this.query()::list, Newcomer.class);
    }

    @Override
    public void refuseNewcomer(Long id) {
        Newcomer newcomer = redisUtil.queryHashWithMutex(CACHE_NEWCOMERS, String.valueOf(id),
                Newcomer.class, this::getById);

        if (newcomer == null) {
            throw new RuntimeException(MessageConstant.ACCOUNT_NOT_FOUND);
        }
        this.removeById(id);
        stringRedisTemplate.opsForHash().delete(CACHE_NEWCOMERS, String.valueOf(id));
    }
}