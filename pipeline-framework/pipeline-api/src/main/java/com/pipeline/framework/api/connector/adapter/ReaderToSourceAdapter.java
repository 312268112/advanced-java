package com.pipeline.framework.api.connector.adapter;

import com.pipeline.framework.api.connector.ConnectorConfig;
import com.pipeline.framework.api.connector.ConnectorReader;
import com.pipeline.framework.api.source.DataSource;

/**
 * ConnectorReader到DataSource的适配器接口。
 * <p>
 * 将ConnectorReader（不依赖Reactor）适配为DataSource（依赖Reactor）。
 * </p>
 *
 * @param <T> 数据类型
 * @param <C> 配置类型
 * @author Pipeline Framework Team
 * @since 1.0.0
 */
public interface ReaderToSourceAdapter<T, C extends ConnectorConfig>
    extends ConnectorAdapter<ConnectorReader<T, C>, DataSource<T>, C> {

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
     * 是否启用背压。
     *
     * @return true表示启用
     */
    default boolean isBackpressureEnabled() {
        return true;
    }
}
