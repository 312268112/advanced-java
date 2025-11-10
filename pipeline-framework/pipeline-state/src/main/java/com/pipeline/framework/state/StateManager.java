package com.pipeline.framework.state;

import java.util.Map;

/**
 * 状态管理器接口。
 * <p>
 * 管理所有算子的状态。
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
     */
    <T> void registerState(String name, State<T> state);

    /**
     * 获取状态。
     *
     * @param name 状态名称
     * @param <T>  状态值类型
     * @return 状态实例
     */
    <T> State<T> getState(String name);

    /**
     * 创建并注册状态。
     *
     * @param name         状态名称
     * @param initialValue 初始值
     * @param <T>          状态值类型
     * @return 状态实例
     */
    <T> State<T> createState(String name, T initialValue);

    /**
     * 创建状态快照。
     *
     * @return 状态快照
     */
    Map<String, Object> snapshot();

    /**
     * 从快照恢复状态。
     *
     * @param snapshot 状态快照
     */
    void restore(Map<String, Object> snapshot);

    /**
     * 清空所有状态。
     */
    void clearAll();

    /**
     * 判断状态是否存在。
     *
     * @param name 状态名称
     * @return true如果存在
     */
    boolean exists(String name);
}
