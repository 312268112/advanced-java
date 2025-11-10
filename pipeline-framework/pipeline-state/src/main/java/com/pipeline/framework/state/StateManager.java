package com.pipeline.framework.state;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.Map;

/**
 * 状态管理器接口。
 * <p>
 * 管理所有算子的状态。
 * 支持响应式API。
 * </p>
 *
 * @author Pipeline Framework Team
 * @since 1.0.0
 */
public interface StateManager {

    /**
     * 注册状态。
     *
     * @param name  状态名称
     * @param state 状态实例
     * @param <T>   状态值类型
     * @return 注册完成信号
     */
    <T> Mono<Void> registerState(String name, State<T> state);

    /**
     * 获取状态。
     *
     * @param name 状态名称
     * @param <T>  状态值类型
     * @return 状态实例的Mono
     */
    <T> Mono<State<T>> getState(String name);

    /**
     * 创建并注册状态。
     *
     * @param name         状态名称
     * @param initialValue 初始值
     * @param <T>          状态值类型
     * @return 状态实例的Mono
     */
    <T> Mono<State<T>> createState(String name, T initialValue);

    /**
     * 创建状态快照。
     * <p>
     * 异步创建所有状态的快照。
     * </p>
     *
     * @return 状态快照Map的Mono
     */
    Mono<Map<String, Object>> snapshot();

    /**
     * 从快照恢复状态。
     * <p>
     * 异步从快照恢复所有状态。
     * </p>
     *
     * @param snapshot 状态快照
     * @return 恢复完成信号
     */
    Mono<Void> restore(Map<String, Object> snapshot);

    /**
     * 清空所有状态。
     *
     * @return 清空完成信号
     */
    Mono<Void> clearAll();

    /**
     * 判断状态是否存在。
     *
     * @param name 状态名称
     * @return true如果存在
     */
    Mono<Boolean> exists(String name);

    /**
     * 获取所有状态名称。
     *
     * @return 状态名称流
     */
    Flux<String> getAllStateNames();

    /**
     * 删除状态。
     *
     * @param name 状态名称
     * @return 删除完成信号
     */
    Mono<Void> removeState(String name);
}
