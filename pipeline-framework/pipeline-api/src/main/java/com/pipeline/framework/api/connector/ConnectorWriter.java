package com.pipeline.framework.api.connector;

import java.util.List;

/**
 * Connector数据写入器。
 * <p>
 * 提供批量数据写入能力，不依赖Reactor。
 * </p>
 *
 * @param <T> 数据类型
 * @author Pipeline Framework Team
 * @since 1.0.0
 */
public interface ConnectorWriter<T> {

    /**
     * 打开写入器。
     *
     * @throws Exception 打开失败
     */
    void open() throws Exception;

    /**
     * 写入单条数据。
     *
     * @param record 数据记录
     * @throws Exception 写入失败
     */
    void write(T record) throws Exception;

    /**
     * 批量写入数据。
     *
     * @param records 数据列表
     * @throws Exception 写入失败
     */
    void writeBatch(List<T> records) throws Exception;

    /**
     * 刷新缓冲区，确保数据写入。
     *
     * @throws Exception 刷新失败
     */
    void flush() throws Exception;

    /**
     * 关闭写入器，释放资源。
     *
     * @throws Exception 关闭失败
     */
    void close() throws Exception;

    /**
     * 保存检查点（用于容错恢复）。
     *
     * @return 检查点信息
     * @throws Exception 保存失败
     */
    default Object saveCheckpoint() throws Exception {
        return null;
    }

    /**
     * 从检查点恢复。
     *
     * @param checkpoint 检查点信息
     * @throws Exception 恢复失败
     */
    default void restoreCheckpoint(Object checkpoint) throws Exception {
        // 默认不支持
    }

    /**
     * 是否支持事务。
     *
     * @return true表示支持
     */
    default boolean supportsTransaction() {
        return false;
    }

    /**
     * 开始事务。
     *
     * @throws Exception 开始失败
     */
    default void beginTransaction() throws Exception {
        // 默认不支持
    }

    /**
     * 提交事务。
     *
     * @throws Exception 提交失败
     */
    default void commit() throws Exception {
        // 默认不支持
    }

    /**
     * 回滚事务。
     *
     * @throws Exception 回滚失败
     */
    default void rollback() throws Exception {
        // 默认不支持
    }

    /**
     * 获取已写入的记录数。
     *
     * @return 记录数
     */
    default long getWriteCount() {
        return 0;
    }
}
