package com.campus;

/**
 * 预约时间冲突异常：同一资源已有通过审核的预约，且时间段重叠时触发。
 *
 * @author 课程设计
 * @version V3.0
 */
public class ReserveTimeConflictException extends BusinessException {

    /** 序列化版本号 */
    private static final long serialVersionUID = 1L;

    /**
     * 构造时间冲突异常。
     *
     * @param resourceId       资源编号
     * @param conflictRecordId 冲突的预约记录编号
     */
    public ReserveTimeConflictException(String resourceId, String conflictRecordId) {
        super("资源【" + resourceId + "】在该时间段已被预约（记录 " + conflictRecordId + "），请更换时间。");
    }
}
