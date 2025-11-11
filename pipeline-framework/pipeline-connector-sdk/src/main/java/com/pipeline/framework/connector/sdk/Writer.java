package com.pipeline.framework.connector.sdk;

import java.io.Closeable;
import java.util.List;

/**
 * 数据写入器接口。
 * <p>
 * Connector 开发者实现此接口以提供数据写入能力。
 * 支持单条写入和批量写入两种模式。
 * </p>
 *
 * @param <T> 记录类型
 * @author Pipeline Framework Team
 * @since 1.0.0
 */
public interface Writer<T> extends Closeable {

    /**
     * 打开写入器。
     * <p>
     * 在开始写入数据之前调用，用于初始化资源。
     * </p>
     *
     * @throws Exception 如果打开失败
     */
    void open() throws Exception;

    /**
     * 写入单条记录。
     *
     * @param record 要写入的记录
     * @throws Exception 如果写入失败
     */
    void write(T record) throws Exception;

    /**
     * 批量写入记录。
     * <p>
     * 默认实现是循环调用 write()，子类可以重写以提供更高效的批量写入。
     * </p>
     *
     * @param records 要写入的记录列表
     * @throws Exception 如果写入失败
     */
    default void writeBatch(List<T> records) throws Exception {
        for (T record : records) {
            write(record);
        }
    }

    /**
     * 刷新缓冲区。
     * <p>
     * 将缓冲的数据强制写入目标系统。
     * </p>
     *
     * @throws Exception 如果刷新失败
     */
    void flush() throws Exception;

    /**
     * 关闭写入器并释放资源。
     * <p>
     * 应该在关闭前自动调用 flush()。
     * </p>
     */
    @Override
    void close();

    /**
     * 获取写入器元数据。
     *
     * @return 元数据
     */
    default WriterMetadata getMetadata() {
        return WriterMetadata.builder()
                .writerName(this.getClass().getSimpleName())
                .supportsBatchWrite(true)
                .build();
    }
}
