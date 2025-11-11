package com.pipeline.framework.api.connector;

import java.util.List;

/**
 * Connector数据读取器。
 * <p>
 * 提供批量数据读取能力，不依赖Reactor。
 * </p>
 *
 * @param <T> 数据类型
 * @author Pipeline Framework Team
 * @since 1.0.0
 */
public interface ConnectorReader<T> {

    /**
     * 打开读取器。
     *
     * @throws Exception 打开失败
     */
    void open() throws Exception;

    /**
     * 批量读取数据。
     *
     * @param batchSize 批次大小
     * @return 数据列表，如果没有更多数据返回null或空列表
     * @throws Exception 读取失败
     */
    List<T> readBatch(int batchSize) throws Exception;

    /**
     * 是否还有更多数据。
     *
     * @return true表示还有数据
     */
    boolean hasNext();

    /**
     * 关闭读取器，释放资源。
     *
     * @throws Exception 关闭失败
     */
    void close() throws Exception;

    /**
     * 获取当前读取位置（用于断点续传）。
     *
     * @return 位置信息
     */
    default Object getCheckpoint() {
        return null;
    }

    /**
     * 从指定位置开始读取（断点续传）。
     *
     * @param checkpoint 检查点位置
     * @throws Exception 定位失败
     */
    default void seekToCheckpoint(Object checkpoint) throws Exception {
        // 默认不支持
    }

    /**
     * 是否支持断点续传。
     *
     * @return true表示支持
     */
    default boolean supportsCheckpoint() {
        return false;
    }

    /**
     * 获取读取进度（0.0-1.0）。
     *
     * @return 进度百分比
     */
    default double getProgress() {
        return -1.0;
    }

    /**
     * 获取已读取的记录数。
     *
     * @return 记录数
     */
    default long getReadCount() {
        return 0;
    }
}
