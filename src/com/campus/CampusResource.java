package com.campus;

/**
 * V2.0 校园资源实体：承载资源数据，不含任何业务逻辑。
 *
 * <p>设计原则（单一职责）：用户实体与资源实体职责完全隔离，互不耦合。
 * 资源状态的合法流转由业务层控制，实体只负责存。</p>
 *
 * @author 课程设计
 * @version V2.0
 */
public class CampusResource {

    /** 资源唯一编号（全局唯一，由系统生成，形如 R001） */
    private String id;

    /** 资源名称（一号会议室、302 实验室、投影仪 A ……） */
    private String name;

    /** 资源类型（教室 / 实验室 / 设备 / 场地） */
    private String type;

    /** 资源状态 */
    private ResourceStatus status;

    /** 位置描述 */
    private String location;

    /** 资源简要描述（可选） */
    private String description;

    /** 无参构造方法。 */
    public CampusResource() {
    }

    /**
     * 全参构造方法。
     *
     * @param id          资源编号
     * @param name        资源名称
     * @param type        资源类型
     * @param status      资源状态
     * @param location    位置
     * @param description 描述
     */
    public CampusResource(String id, String name, String type, ResourceStatus status,
                          String location, String description) {
        this.id = id;
        this.name = name;
        this.type = type;
        this.status = status;
        this.location = location;
        this.description = description;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public ResourceStatus getStatus() {
        return status;
    }

    public void setStatus(ResourceStatus status) {
        this.status = status;
    }

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    /**
     * 是否为可预约状态。
     *
     * <p>这是一个取值判断，不是业务规则；状态能不能变由业务层决定。</p>
     *
     * @return 可预约返回 true
     */
    public boolean isAvailable() {
        return ResourceStatus.AVAILABLE.equals(status);
    }

    /**
     * 输出资源信息，控制台列表直接使用。
     *
     * @return 资源信息字符串
     */
    @Override
    public String toString() {
        return "CampusResource{编号='" + id + "', 名称='" + name + "', 类型='" + type
                + "', 状态=" + status + ", 位置='" + location + "', 描述='" + description + "'}";
    }
}
