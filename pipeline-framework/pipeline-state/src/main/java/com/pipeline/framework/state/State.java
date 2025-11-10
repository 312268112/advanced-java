package com.pipeline.framework.state;

/**
 * 状态接口。
 * <p>
 * 用于有状态算子存储和管理状态。
 * </p>
 *
 * @param <T> 状态值类型
 * @author Pipeline Framework Team
 * @since 1.0.0
 */
public interface State<T> {

    /**
     * 获取状态值。
     *
     * @return 状态值
     */
    T get();

    /**
     * 更新状态值。
     *
     * @param value 新的状态值
     */
    void update(T value);

    /**
     * 清空状态。
     */
    void clear();

    /**
     * 判断状态是否为空。
     *
     * @return true如果为空
     */
    boolean isEmpty();

    /**
     * 获取状态名称。
     *
     * @return 状态名称
     */
    String getName();
}
