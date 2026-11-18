package com.campus;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * V3.0 预约记录实体：一条预约申请的全部数据，不含任何业务逻辑。
 *
 * <p>设计原则（单一职责 / 迪米特法则）：预约实体通过 {@code resourceId} 关联资源，
 * 只保存资源编号，不持有 {@link CampusResource} 对象，也不操作资源内部细节。</p>
 *
 * @author 课程设计
 * @version V3.0
 */
public class ReserveRecord {

    /** 时间展示格式化器 */
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    /** 预约记录唯一编号（全局唯一，形如 A001） */
    private String recordId;

    /** 关联的校园资源编号 */
    private String resourceId;

    /** 申请人用户名 */
    private String applicantUsername;

    /** 申请时间 */
    private LocalDateTime applyTime;

    /** 预约使用开始时间 */
    private LocalDateTime reserveStartTime;

    /** 预约使用结束时间 */
    private LocalDateTime reserveEndTime;

    /** 预约状态 */
    private ReserveStatus status;

    /** 预约备注（可选） */
    private String remark;

    /** 无参构造方法。 */
    public ReserveRecord() {
    }

    /**
     * 全参构造方法。
     *
     * @param recordId          预约记录编号
     * @param resourceId        资源编号
     * @param applicantUsername 申请人
     * @param applyTime         申请时间
     * @param reserveStartTime  使用开始时间
     * @param reserveEndTime    使用结束时间
     * @param status            预约状态
     * @param remark            备注
     */
    public ReserveRecord(String recordId, String resourceId, String applicantUsername,
                         LocalDateTime applyTime, LocalDateTime reserveStartTime,
                         LocalDateTime reserveEndTime, ReserveStatus status, String remark) {
        this.recordId = recordId;
        this.resourceId = resourceId;
        this.applicantUsername = applicantUsername;
        this.applyTime = applyTime;
        this.reserveStartTime = reserveStartTime;
        this.reserveEndTime = reserveEndTime;
        this.status = status;
        this.remark = remark;
    }

    public String getRecordId() {
        return recordId;
    }

    public void setRecordId(String recordId) {
        this.recordId = recordId;
    }

    public String getResourceId() {
        return resourceId;
    }

    public void setResourceId(String resourceId) {
        this.resourceId = resourceId;
    }

    public String getApplicantUsername() {
        return applicantUsername;
    }

    public void setApplicantUsername(String applicantUsername) {
        this.applicantUsername = applicantUsername;
    }

    public LocalDateTime getApplyTime() {
        return applyTime;
    }

    public void setApplyTime(LocalDateTime applyTime) {
        this.applyTime = applyTime;
    }

    public LocalDateTime getReserveStartTime() {
        return reserveStartTime;
    }

    public void setReserveStartTime(LocalDateTime reserveStartTime) {
        this.reserveStartTime = reserveStartTime;
    }

    public LocalDateTime getReserveEndTime() {
        return reserveEndTime;
    }

    public void setReserveEndTime(LocalDateTime reserveEndTime) {
        this.reserveEndTime = reserveEndTime;
    }

    public ReserveStatus getStatus() {
        return status;
    }

    public void setStatus(ReserveStatus status) {
        this.status = status;
    }

    public String getRemark() {
        return remark;
    }

    public void setRemark(String remark) {
        this.remark = remark;
    }

    /**
     * 判断本预约是否与另一条预约时间段重叠。
     *
     * <p>重叠判定：{@code start < otherEnd && end > otherStart}；首尾相接不算重叠。</p>
     *
     * @param other 另一条预约记录
     * @return 重叠返回 true
     */
    public boolean overlaps(ReserveRecord other) {
        return reserveStartTime.isBefore(other.reserveEndTime)
                && reserveEndTime.isAfter(other.reserveStartTime);
    }

    /**
     * 输出预约信息。
     *
     * @return 预约信息字符串
     */
    @Override
    public String toString() {
        String start = reserveStartTime == null ? "未知" : reserveStartTime.format(FORMATTER);
        String end = reserveEndTime == null ? "未知" : reserveEndTime.format(FORMATTER);
        String applied = applyTime == null ? "未知" : applyTime.format(FORMATTER);
        return "ReserveRecord{编号='" + recordId + "', 资源='" + resourceId + "', 申请人='"
                + applicantUsername + "', 状态=" + status + ", 使用时间=" + start + " ~ " + end
                + ", 申请时间=" + applied + ", 备注='" + remark + "'}";
    }
}
