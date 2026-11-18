package com.campus;

/**
 * 资源不可预约异常：资源不存在、已占用或已停用时提交预约触发。
 *
 * @author 课程设计
 * @version V3.0
 */
public class ResourceUnavailableException extends BusinessException {

    /** 序列化版本号 */
    private static final long serialVersionUID = 1L;

    /**
     * 构造资源不可用异常（资源不存在）。
     *
     * @param resourceId 资源编号
     */
    public ResourceUnavailableException(String resourceId) {
        super("资源【" + resourceId + "】不存在，请先确认资源编号。");
    }

    /**
     * 构造资源不可用异常（状态不符）。
     *
     * @param resourceId 资源编号
     * @param statusName 当前状态中文名
     */
    public ResourceUnavailableException(String resourceId, String statusName) {
        super("资源【" + resourceId + "】当前状态为「" + statusName + "」，不可预约。");
    }
}
