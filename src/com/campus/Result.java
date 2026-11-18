package com.campus;

/**
 * V2.1 统一业务返回结果：封装「成功与否 + 提示信息 + 返回数据」。
 *
 * <p>设计原则（单一职责）：Result 只负责封装结果，不含业务判断、不处理 IO。
 * 设计原则（迪米特法则）：控制台只读 success / msg / data，不需要了解业务实现细节。</p>
 *
 * <p><b>注意一个签名陷阱</b>：{@code success(String msg)} 与 {@code success(T data)}
 * 在 T 恰好是 String 时会命中前者，导致数据被当成提示语。
 * 需要返回字符串数据时，请显式使用 {@code success(msg, data)} 三参形式。</p>
 *
 * @param <T> 业务数据类型
 * @author 课程设计
 * @version V2.1
 */
public class Result<T> {

    /** 本次操作是否成功 */
    private boolean success;

    /** 业务提示信息（给用户看的中文） */
    private String msg;

    /** 业务返回数据；失败时为 null */
    private T data;

    /** 无参构造方法（供框架 / 反射使用）。 */
    public Result() {
    }

    /**
     * 全参构造方法。
     *
     * @param success 是否成功
     * @param msg     提示信息
     * @param data    业务数据
     */
    public Result(boolean success, String msg, T data) {
        this.success = success;
        this.msg = msg;
        this.data = data;
    }

    /**
     * 成功，携带返回数据。
     *
     * @param data 业务数据
     * @param <T>  数据类型
     * @return 成功结果
     */
    public static <T> Result<T> success(T data) {
        return new Result<T>(true, "操作成功", data);
    }

    /**
     * 成功，无返回数据，仅提示信息。
     *
     * @param msg 成功提示
     * @param <T> 数据类型
     * @return 成功结果
     */
    public static <T> Result<T> success(String msg) {
        return new Result<T>(true, msg, null);
    }

    /**
     * 成功，自定义消息 + 数据。
     *
     * @param msg  成功提示
     * @param data 业务数据
     * @param <T>  数据类型
     * @return 成功结果
     */
    public static <T> Result<T> success(String msg, T data) {
        return new Result<T>(true, msg, data);
    }

    /**
     * 失败，携带错误信息。
     *
     * @param msg  失败提示
     * @param <T>  数据类型
     * @return 失败结果
     */
    public static <T> Result<T> fail(String msg) {
        return new Result<T>(false, msg, null);
    }

    public boolean isSuccess() {
        return success;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }

    public String getMsg() {
        return msg;
    }

    public void setMsg(String msg) {
        this.msg = msg;
    }

    public T getData() {
        return data;
    }

    public void setData(T data) {
        this.data = data;
    }

    /**
     * 输出结果信息（不打印业务数据，数据由调用方按类型展示）。
     *
     * @return 结果字符串
     */
    @Override
    public String toString() {
        return "Result{success=" + success + ", msg='" + msg + "', data=" + data + "}";
    }
}
