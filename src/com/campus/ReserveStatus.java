package com.campus;

/**
 * 预约状态枚举：固定四种合法状态，禁止自由字符串。
 *
 * @author 课程设计
 * @version V3.0
 */
public enum ReserveStatus {

    /** 待审核（用户刚提交） */
    PENDING("待审核"),

    /** 已通过（管理员审核成功） */
    PASS("已通过"),

    /** 已驳回（管理员拒绝） */
    REJECT("已驳回"),

    /** 已取消（用户主动撤销） */
    CANCEL("已取消");

    /** 中文显示名 */
    private final String label;

    /**
     * 构造枚举项。
     *
     * @param label 中文显示名
     */
    ReserveStatus(String label) {
        this.label = label;
    }

    /**
     * 获取中文显示名。
     *
     * @return 中文名
     */
    public String getLabel() {
        return label;
    }

    /**
     * 把用户输入解析成状态枚举，支持「枚举名」「中文名」「序号」三种写法。
     *
     * @param text 用户输入
     * @return 状态枚举；输入非法返回 null
     */
    public static ReserveStatus parse(String text) {
        if (text == null) {
            return null;
        }
        String value = text.trim();
        for (ReserveStatus status : values()) {
            if (status.name().equalsIgnoreCase(value) || status.label.equals(value)) {
                return status;
            }
        }
        for (int i = 0; i < values().length; i++) {
            if (String.valueOf(i + 1).equals(value)) {
                return values()[i];
            }
        }
        return null;
    }

    /**
     * 输出「待审核(PENDING)」这种便于阅读的形式。
     *
     * @return 展示文本
     */
    @Override
    public String toString() {
        return label + "(" + name() + ")";
    }
}
