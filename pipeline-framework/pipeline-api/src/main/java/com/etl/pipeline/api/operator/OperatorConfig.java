package com.pipeline.framework.api.operator;

import java.util.Map;

/**
 * 算子配置接口。
 *
 * @author ETL Framework Team
 * @since 1.0.0
 */
public interface OperatorConfig {

    /**
     * 获取算子ID。
     *
     * @return 算子ID
     */
    String getOperatorId();

    /**
     * 获取算子名称。
     *
     * @return 算子名称
     */
    String getOperatorName();

    /**
     * 获取配置参数。
     *
     * @return 配置参数Map
     */
    Map<String, Object> getConfig();
}
