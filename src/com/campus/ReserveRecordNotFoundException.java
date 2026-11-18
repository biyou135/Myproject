package com.campus;

/**
 * 预约记录不存在异常：按 recordId 查找 / 审核 / 取消预约时编号无效触发。
 *
 * @author 课程设计
 * @version V3.0
 */
public class ReserveRecordNotFoundException extends BusinessException {

    /** 序列化版本号 */
    private static final long serialVersionUID = 1L;

    /**
     * 构造预约记录不存在异常。
     *
     * @param recordId 预约记录编号
     */
    public ReserveRecordNotFoundException(String recordId) {
        super("预约记录【" + recordId + "】不存在，请确认编号后重试。");
    }
}
