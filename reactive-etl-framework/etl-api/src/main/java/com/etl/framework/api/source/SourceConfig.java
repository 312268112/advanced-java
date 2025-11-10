package com.etl.framework.api.source;

import java.util.Map;

/**
 * 数据源配置接口。
 *
 * @author ETL Framework Team
 * @since 1.0.0
 */
public interface SourceConfig {

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
     * 获取缓冲区大小。
     *
     * @return 缓冲区大小
     */
    int getBufferSize();
}
