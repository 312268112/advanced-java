package com.pipeline.framework.connector.sdk;

/**
 * Connector标记接口。
 * <p>
 * 所有Connector都应该实现此接口，并根据需要组合其他能力接口：
 * <ul>
 *   <li>{@link Readable} - 数据读取能力</li>
 *   <li>{@link Writable} - 数据写入能力</li>
 *   <li>{@link Seekable} - 断点续传能力（可选）</li>
 *   <li>{@link Lifecycle} - 生命周期管理</li>
 * </ul>
 * </p>
 *
 * @author Pipeline Framework Team
 * @since 1.0.0
 */
public interface Connector {

    /**
     * 获取Connector名称。
     *
     * @return 名称
     */
    String name();

    /**
     * 获取Connector版本。
     *
     * @return 版本
     */
    default String version() {
        return "1.0.0";
    }
}
