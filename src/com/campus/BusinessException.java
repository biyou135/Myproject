package com.campus;

/**
 * V1.4 自定义业务异常父类。
 *
 * <p>继承 {@link RuntimeException}（非受检异常），让业务方法无需层层声明 throws，
 * 由程序入口统一捕获，只输出中文友好提示，屏蔽原生异常堆栈。</p>
 *
 * <p>设计原则（开闭原则）：后续新增错误场景，只需新增子异常类，
 * 无需修改原有业务代码与统一捕获逻辑。</p>
 *
 * @author 课程设计
 * @version V1.4
 */
public class BusinessException extends RuntimeException {

    /** 序列化版本号 */
    private static final long serialVersionUID = 1L;

    /**
     * 构造业务异常。
     *
     * @param message 面向用户的中文友好提示
     */
    public BusinessException(String message) {
        super(message);
    }

    /**
     * 构造业务异常，保留底层原因便于日志排查。
     *
     * @param message 面向用户的中文友好提示
     * @param cause   底层原因
     */
    public BusinessException(String message, Throwable cause) {
        super(message, cause);
    }
}
