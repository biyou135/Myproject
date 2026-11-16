package com.campus;

/**
 * 用户已存在异常：注册时账号重复触发。
 *
 * @author 课程设计
 * @version V1.4
 */
public class UserAlreadyExistsException extends BusinessException {

    /** 序列化版本号 */
    private static final long serialVersionUID = 1L;

    /**
     * 构造用户已存在异常。
     *
     * @param username 重复的账号
     */
    public UserAlreadyExistsException(String username) {
        super("账号【" + username + "】已存在，请更换账号后再注册。");
    }
}
