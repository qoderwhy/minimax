package com.qkit.common.api;

/**
 * 错误码枚举。集中维护所有业务错误码，避免散落。
 */
public enum ErrorCode {

    SUCCESS(200, "成功"),

    BAD_REQUEST(400, "请求参数错误"),
    UNAUTHORIZED(401, "未登录或登录已过期"),
    FORBIDDEN(403, "无权限访问"),
    NOT_FOUND(404, "资源不存在"),
    METHOD_NOT_ALLOWED(405, "请求方法不允许"),
    INTERNAL_ERROR(500, "系统异常，请联系管理员"),

    USER_NOT_FOUND(10001, "用户不存在"),
    USER_PASSWORD_ERROR(10002, "用户名或密码错误"),
    USER_DISABLED(10003, "用户已被停用"),
    USER_LOCKED(10004, "账号已锁定，请稍后再试"),
    USER_EXISTS(10005, "用户名已存在"),
    USERNAME_OR_PASSWORD_ERROR(10006, "用户名或密码错误"),
    USER_CANNOT_DELETE_SELF(10007, "不能删除当前登录用户"),
    USER_CANNOT_DISABLE_SELF(10008, "不能停用当前登录用户"),
    USER_PROTECTED(10009, "系统内置管理员账号，不允许操作"),

    CAPTCHA_INVALID(10101, "验证码错误或已过期"),
    CAPTCHA_REQUIRED(10102, "请输入验证码"),

    ROLE_NOT_FOUND(11001, "角色不存在"),
    ROLE_IN_USE(11002, "角色已分配用户，无法删除"),
    ROLE_SYSTEM_PROTECTED(11003, "系统内置角色，不允许操作"),
    ROLE_EXISTS(11004, "角色编码已存在"),

    MENU_HAS_CHILDREN(12001, "存在子菜单，无法删除"),
    DEPT_HAS_CHILDREN(13001, "存在子部门，无法删除"),
    DEPT_HAS_USER(13002, "部门下存在用户，无法删除"),

    DICT_HAS_ITEMS(14001, "字典下存在字典项，无法删除"),

    POST_IN_USE(15001, "岗位已分配用户，无法删除"),

    EXPORT_ERROR(16001, "导出失败，请稍后重试"),

    CONFIG_KEY_EXISTS(17001, "参数键名已存在"),
    CONFIG_BUILTIN(17002, "系统内置参数，不允许删除或修改键名"),

    VALIDATION_FAILED(422, "参数校验失败"),
    TOO_MANY_REQUESTS(429, "请求过于频繁，请稍后再试"),
    REPEAT_SUBMIT(429, "请勿重复提交，请稍后再试");

    private final Integer code;
    private final String message;

    ErrorCode(Integer code, String message) {
        this.code = code;
        this.message = message;
    }

    public Integer getCode() {
        return code;
    }

    public String getMessage() {
        return message;
    }
}
