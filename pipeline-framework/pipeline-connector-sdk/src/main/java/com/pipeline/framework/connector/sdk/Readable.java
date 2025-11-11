package com.pipeline.framework.connector.sdk;

import java.util.List;

/**
 * 可读取能力接口。
 * <p>
 * Connector实现此接口以提供数据读取能力。
 * 支持批量读取以提高性能。
 * </p>
 *
 * @param <T> 数据类型
 * @author Pipeline Framework Team
 * @since 1.0.0
 */
public interface Readable<T> {

    /**
     * 批量读取数据。
     *
     * @param batchSize 批次大小
     * @return 数据批次，如果没有更多数据返回null或空列表
     * @throws Exception 读取失败
     */
    List<T> read(int batchSize) throws Exception;

    /**
     * 是否还有更多数据。
     *
     * @return true如果还有数据
     */
    boolean hasMore();
}
