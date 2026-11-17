package com.campus;

/**
 * 资源不存在异常：按 ID 查找 / 修改 / 删除资源时，ID 无效触发。
 *
 * @author 课程设计
 * @version V2.0
 */
public class ResourceNotFoundException extends BusinessException {

    /** 序列化版本号 */
    private static final long serialVersionUID = 1L;

    /**
     * 构造资源不存在异常。
     *
     * @param resourceId 查询的资源编号
     */
    public ResourceNotFoundException(String resourceId) {
        super("资源【" + resourceId + "】不存在，请确认编号后重试。");
    }
}
