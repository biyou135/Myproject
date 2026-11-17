package com.campus;

/**
 * 资源状态枚举：固定三种合法状态，禁止自由字符串。
 *
 * <p>设计原则（开闭原则）：后续若新增状态，只需在此扩展枚举项，
 * 不需要修改使用方的判断代码。</p>
 *
 * @author 课程设计
 * @version V2.0
 */
public enum ResourceStatus {

    /** 可预约 */
    AVAILABLE("可预约"),

    /** 占用（已有通过的预约） */
    OCCUPIED("占用"),

    /** 停用（管理员禁用，不可预约） */
    DISABLED("停用");

    /** 中文显示名 */
    private final String label;

    /**
     * 构造枚举项。
     *
     * @param label 中文显示名
     */
    ResourceStatus(String label) {
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
     * 把用户输入解析成状态枚举，支持「枚举名」和「序号」两种写法。
     *
     * @param text 用户输入
     * @return 状态枚举；输入非法时返回 null（由调用方决定如何提示）
     */
    public static ResourceStatus parse(String text) {
        if (text == null) {
            return null;
        }
        String value = text.trim();
        for (ResourceStatus status : values()) {
            if (status.name().equalsIgnoreCase(value) || status.label.equals(value)) {
                return status;
            }
        }
        // 支持输入序号：1 / 2 / 3
        for (int i = 0; i < values().length; i++) {
            if (String.valueOf(i + 1).equals(value)) {
                return values()[i];
            }
        }
        return null;
    }

    /**
     * 输出「可预约(AVAILABLE)」这种便于阅读的形式。
     *
     * @return 展示文本
     */
    @Override
    public String toString() {
        return label + "(" + name() + ")";
    }
}
