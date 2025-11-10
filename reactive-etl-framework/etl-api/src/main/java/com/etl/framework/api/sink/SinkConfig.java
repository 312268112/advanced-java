package com.etl.framework.api.sink;

import java.util.Map;

/**
 * Sink配置接口。
 *
 * @author ETL Framework Team
 * @since 1.0.0
 */
public interface SinkConfig {

    /**
     * 获取数据源ID。
     *
     * @return 数据源ID
     */
    String getDataSourceId();

    /**
     * 获取连接器类型。
     *
     * @return 连接器类型（如：jdbc, kafka, http）
     */
    String getConnectorType();

    /**
     * 获取配置参数。
     *
     * @return 配置参数Map
     */
    Map<String, Object> getConfig();

    /**
     * 获取批量大小。
     *
     * @return 批量大小
     */
    int getBatchSize();

    /**
     * 获取刷新间隔（毫秒）。
     *
     * @return 刷新间隔
     */
    long getFlushIntervalMs();
}
