package com.xiaoyan.constant;

/**
 * 信息提示常量类
 */
public class MessageConstant {

    public static final String PASSWORD_ERROR = "密码错误";
    public static final String ACCOUNT_NOT_FOUND = "账号不存在";
    public static final String ACCOUNT_LOCKED = "账号被锁定";
    public static final String PERMISSION_DENIED ="没有权限";
    public static final String USER_NOT_LOGIN = "用户未登录";
    public static final String LOGIN_FAILED = "登录失败";
    public static final String FILE_DELETE_FAILED = "文件删除失败";
    public static final String PASSWORD_EDIT_FAILED = "密码修改失败";
    public static final String ILLEGAL_OPERATION="非法操作";
    public static final String REPEATREQUEST="重复申请";
    public static final String VERIFICATION_CODE_MISMATCH="验证码不匹配";
    public static final String PARAMETER_ERROR ="参数错误";
    public static final String SESSION_NO_FOUND="会话没找到！";
    public static final String ALIOSS_NETWORK_ERROR="oss网络异常";

    // 全局异常处理器用到的提示。对外不暴露异常细节，具体原因只写日志。
    public static final String REQUEST_FORMAT_ERROR = "请求格式错误";
    public static final String PARAMETER_VALIDATE_FAILED = "参数校验失败";
    public static final String METHOD_NOT_ALLOWED = "请求方法不支持";
    public static final String CONTENT_TYPE_NOT_SUPPORTED = "不支持的内容类型";
    public static final String FILE_TOO_LARGE = "上传文件过大";
    public static final String RESOURCE_NOT_FOUND = "请求的资源不存在";
    public static final String DATA_DUPLICATE = "数据已存在，请勿重复提交";
    public static final String SERVER_ERROR = "服务器开小差了，请稍后重试";

    public static final String LAST_ADMIN_CANNOT_LEAVE = "你是最后一位会长，请先设置另一位成员为会长，再注销账号";



}