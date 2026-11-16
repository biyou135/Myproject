package com.campus;

/**
 * 参数异常：必填参数缺失、空白或格式不合法时触发。
 *
 * <p>通过静态工厂方法区分「参数为空」与「参数格式非法」两种提示，
 * 保持异常类单一（符合开闭原则，无需为每种格式错误各建一个类）。</p>
 *
 * @author 课程设计
 * @version V1.4
 */
public class ParamEmptyException extends BusinessException {

    /** 序列化版本号 */
    private static final long serialVersionUID = 1L;

    /**
     * 构造参数异常。
     *
     * @param message 面向用户的中文提示
     */
    public ParamEmptyException(String message) {
        super(message);
    }

    /**
     * 参数为空场景的工厂方法。
     *
     * @param fieldName 参数中文名
     * @return 参数异常实例
     */
    public static ParamEmptyException empty(String fieldName) {
        return new ParamEmptyException(fieldName + "不能为空，请重新输入。");
    }

    /**
     * 参数格式非法场景的工厂方法。
     *
     * @param message 具体的格式错误提示
     * @return 参数异常实例
     */
    public static ParamEmptyException illegal(String message) {
        return new ParamEmptyException(message);
    }
}
