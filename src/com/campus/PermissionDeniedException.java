package com.campus;

/**
 * 权限不足异常：越权操作（学生增删改资源、未成年人审核等）触发。
 *
 * @author 课程设计
 * @version V2.0
 */
public class PermissionDeniedException extends BusinessException {

    /** 序列化版本号 */
    private static final long serialVersionUID = 1L;

    /**
     * 构造权限不足异常。
     *
     * @param action 被拒绝的操作描述
     */
    public PermissionDeniedException(String action) {
        super("权限不足：当前角色无权「" + action + "」。");
    }

    /**
     * 构造权限不足异常（自定义提示）。
     *
     * @param action  被拒绝的操作描述
     * @param roleName 当前角色名
     */
    public PermissionDeniedException(String action, String roleName) {
        super("权限不足：" + roleName + "无权「" + action + "」。");
    }
}
