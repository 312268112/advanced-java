package com.pipeline.framework.connector.sdk;

import java.io.Closeable;
import java.util.List;

/**
 * 批量数据读取器接口。
 * <p>
 * 用于批量读取数据，适合大数据量场景，性能优于单条读取。
 * </p>
 *
 * @param <T> 记录类型
 * @author Pipeline Framework Team
 * @since 1.0.0
 */
public interface BatchReader<T> extends Closeable {

    /**
     * 打开读取器。
     *
     * @throws Exception 如果打开失败
     */
    void open() throws Exception;

    /**
     * 批量读取数据。
     * <p>
     * 每次调用返回一批数据，当没有更多数据时返回 null 或空列表。
     * </p>
     *
     * @param batchSize 期望的批次大小
     * @return 数据批次，如果没有更多数据则返回 null
     * @throws Exception 如果读取失败
     */
    List<T> readBatch(int batchSize) throws Exception;

    /**
     * 检查是否还有更多数据。
     *
     * @return true 如果还有数据，false 否则
     */
    boolean hasMore();

    /**
     * 关闭读取器并释放资源。
     */
    @Override
    void close();

    /**
     * 获取读取器元数据。
     *
     * @return 元数据
     */
    default ReaderMetadata getMetadata() {
        return ReaderMetadata.builder()
                .readerName(this.getClass().getSimpleName())
                .supportsBatchRead(true)
                .build();
    }
}
