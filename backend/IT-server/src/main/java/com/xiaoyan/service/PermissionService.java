package com.xiaoyan.service;

public interface PermissionService {

    void checkOwnerOrAdminPermission(Integer studentId);
}