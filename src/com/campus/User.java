package com.campus;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * V1.2 用户实体类：把 V1.1 的 HashMap 账号密码升级为面向对象数据模型。
 *
 * <p>封装原则 / 单一职责原则：实体仅用于承载数据，不包含任何业务逻辑。</p>
 *
 * @author 课程设计
 * @version V1.2
 */
public class User {

    /** 管理员角色标识 */
    public static final String ROLE_ADMIN = "admin";

    /** 学生角色标识 */
    public static final String ROLE_STUDENT = "student";

    /** 注册时间格式化器 */
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    /** 账号 */
    private String username;

    /** 密码 */
    private String password;

    /** 角色（admin / student） */
    private String role;

    /** 注册时间 */
    private LocalDateTime registerTime;

    /** 无参构造方法。 */
    public User() {
    }

    /**
     * 全参构造方法。
     *
     * @param username     账号
     * @param password     密码
     * @param role         角色
     * @param registerTime 注册时间
     */
    public User(String username, String password, String role, LocalDateTime registerTime) {
        this.username = username;
        this.password = password;
        this.role = role;
        this.registerTime = registerTime;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }

    public LocalDateTime getRegisterTime() {
        return registerTime;
    }

    public void setRegisterTime(LocalDateTime registerTime) {
        this.registerTime = registerTime;
    }

    /**
     * 判断当前用户是否为管理员。
     *
     * @return 管理员返回 true
     */
    public boolean isAdmin() {
        return ROLE_ADMIN.equals(role);
    }

    /**
     * 角色中文名称，用于控制台友好展示。
     *
     * @return 角色中文名
     */
    public String getRoleName() {
        return isAdmin() ? "管理员" : "学生";
    }

    /**
     * 输出用户信息；密码做脱敏处理，避免控制台明文泄露。
     *
     * @return 用户信息字符串
     */
    @Override
    public String toString() {
        String time = registerTime == null ? "未知" : registerTime.format(FORMATTER);
        return "User{账号='" + username + "', 密码='******', 角色=" + getRoleName()
                + "(role=" + role + "), 注册时间=" + time + "}";
    }
}

