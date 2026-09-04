package com.xiaoyan.service.impl;


import com.xiaoyan.constant.JwtClaimsConstant;
import com.xiaoyan.constant.MessageConstant;
import com.xiaoyan.context.BaseContext;
import com.xiaoyan.exception.ParameterException;
import com.xiaoyan.service.PermissionService;
import com.xiaoyan.service.UsersService;
import com.xiaoyan.vo.StudentVO;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@AllArgsConstructor
public class PermissionServiceImpl implements PermissionService {

    private UsersService usersService;

    @Override
    public void checkOwnerOrAdminPermission(Integer studentId) {
        // 权限校验：仅作者或管理员可修改
        Integer currentStudentId = BaseContext.getCurrentStudentId();
        StudentVO user = usersService.getUser(currentStudentId);
        if (!JwtClaimsConstant.ADMIN_ID.equals(user.getPosition()) && !currentStudentId.equals(studentId)) {
            throw new ParameterException(MessageConstant.PERMISSION_DENIED);
        }
    }
}