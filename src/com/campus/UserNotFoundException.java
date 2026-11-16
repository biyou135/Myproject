package com.campus;

/**
 * 用户不存在异常：登录时账号未注册、或按账号查询用户失败触发。
 *
 * @author 课程设计
 * @version V1.4
 */
public class UserNotFoundException extends BusinessException {

    /** 序列化版本号 */
    private static final long serialVersionUID = 1L;

    /**
     * 构造用户不存在异常。
     *
     * @param username 查询的账号
     */
    public UserNotFoundException(String username) {
        super("账号【" + username + "】不存在，请确认后重试或先注册。");
    }
}
