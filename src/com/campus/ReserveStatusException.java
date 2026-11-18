package com.campus;

/**
 * 预约状态异常：当前状态不允许执行该操作时触发（例如已驳回的预约再次取消）。
 *
 * @author 课程设计
 * @version V3.0
 */
public class ReserveStatusException extends BusinessException {

    /** 序列化版本号 */
    private static final long serialVersionUID = 1L;

    /**
     * 构造预约状态异常。
     *
     * @param action   被拒绝的操作
     * @param statusName 记录当前状态中文名
     */
    public ReserveStatusException(String action, String statusName) {
        super("该预约当前状态为「" + statusName + "」，不能" + action + "。");
    }
}
