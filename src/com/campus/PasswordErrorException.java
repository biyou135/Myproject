package com.campus;

/**
 * 密码错误异常：登录时账号存在但密码不匹配触发。
 *
 * @author 课程设计
 * @version V1.4
 */
public class PasswordErrorException extends BusinessException {

    /** 序列化版本号 */
    private static final long serialVersionUID = 1L;

    /** 构造密码错误异常。 */
    public PasswordErrorException() {
        super("密码错误，请重新输入。");
    }
}
