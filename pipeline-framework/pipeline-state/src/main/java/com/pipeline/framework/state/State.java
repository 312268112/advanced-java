package com.pipeline.framework.state;

import reactor.core.publisher.Mono;

/**
 * 状态接口。
 * <p>
 * 用于有状态算子存储和管理状态。
 * 支持响应式访问。
 * </p>
 *
 * @param <T> 状态值类型
 * @author Pipeline Framework Team
 * @since 1.0.0
 */
public interface State<T> {

    /**
     * 获取状态值。
     * <p>
     * 异步获取当前状态值。
     * </p>
     *
     * @return 状态值的Mono
     */
    Mono<T> get();

    /**
     * 更新状态值。
     * <p>
     * 异步更新状态值。
     * </p>
     *
     * @param value 新的状态值
     * @return 更新完成信号
     */
    Mono<Void> update(T value);

    /**
     * 清空状态。
     * <p>
     * 异步清空状态值。
     * </p>
     *
     * @return 清空完成信号
     */
    Mono<Void> clear();

    /**
     * 判断状态是否为空。
     *
     * @return true如果为空
     */
    Mono<Boolean> isEmpty();

    /**
     * 获取状态名称。
     *
     * @return 状态名称
     */
    String getName();

    /**
     * 比较并更新（CAS操作）。
     * <p>
     * 原子性地比较当前值并更新。
     * </p>
     *
     * @param expect 期望的当前值
     * @param update 新的值
     * @return true如果更新成功
     */
    Mono<Boolean> compareAndSet(T expect, T update);
}
