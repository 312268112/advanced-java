package com.pipeline.framework.connector.sdk;

import java.util.List;

/**
 * 可写入能力接口。
 * <p>
 * Connector实现此接口以提供数据写入能力。
 * 支持批量写入以提高性能。
 * </p>
 *
 * @param <T> 数据类型
 * @author Pipeline Framework Team
 * @since 1.0.0
 */
public interface Writable<T> {

    /**
     * 批量写入数据。
     *
     * @param records 数据批次
     * @throws Exception 写入失败
     */
    void write(List<T> records) throws Exception;

    /**
     * 刷新缓冲区，确保数据写入。
     *
     * @throws Exception 刷新失败
     */
    void flush() throws Exception;
}
