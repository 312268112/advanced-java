package com.pipeline.framework.api.connector.adapter;

import com.pipeline.framework.api.component.Component;
import com.pipeline.framework.api.connector.Connector;
import com.pipeline.framework.api.connector.ConnectorConfig;

/**
 * Connector到Component的适配器接口。
 * <p>
 * 使用适配器模式，将不依赖Reactor的Connector
 * 转换为依赖Reactor的Component。
 * </p>
 *
 * @param <CONN> Connector类型
 * @param <COMP> Component类型
 * @param <C>    配置类型
 * @author Pipeline Framework Team
 * @since 1.0.0
 */
public interface ConnectorAdapter<CONN extends Connector<C>, COMP extends Component<?>, C extends ConnectorConfig> {

    /**
     * 适配Connector为Component。
     *
     * @param connector Connector实例
     * @return Component实例
     */
    COMP adapt(CONN connector);

    /**
     * 获取源Connector。
     *
     * @return Connector实例
     */
    CONN getConnector();

    /**
     * 是否支持适配。
     *
     * @param connector Connector实例
     * @return true表示支持
     */
    boolean supports(CONN connector);
}
