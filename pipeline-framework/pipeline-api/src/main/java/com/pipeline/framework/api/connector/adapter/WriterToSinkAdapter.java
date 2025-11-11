package com.pipeline.framework.api.connector.adapter;

import com.pipeline.framework.api.connector.ConnectorConfig;
import com.pipeline.framework.api.connector.ConnectorWriter;
import com.pipeline.framework.api.sink.DataSink;

/**
 * ConnectorWriter到DataSink的适配器接口。
 * <p>
 * 将ConnectorWriter（不依赖Reactor）适配为DataSink（依赖Reactor）。
 * </p>
 *
 * @param <T> 数据类型
 * @param <C> 配置类型
 * @author Pipeline Framework Team
 * @since 1.0.0
 */
public interface WriterToSinkAdapter<T, C extends ConnectorConfig>
    extends ConnectorAdapter<ConnectorWriter<T, C>, DataSink<T>, C> {

    /**
     * 获取批次大小。
     *
     * @return 批次大小
     */
    int getBatchSize();

    /**
     * 设置批次大小。
     *
     * @param batchSize 批次大小
     */
    void setBatchSize(int batchSize);

    /**
     * 是否启用自动刷新。
     *
     * @return true表示启用
     */
    default boolean isAutoFlushEnabled() {
        return true;
    }

    /**
     * 获取刷新间隔（毫秒）。
     *
     * @return 刷新间隔
     */
    default long getFlushInterval() {
        return 1000L;
    }
}
