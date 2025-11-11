package com.pipeline.framework.core.connector;

import com.pipeline.framework.api.component.Component;
import com.pipeline.framework.api.connector.Connector;
import com.pipeline.framework.api.connector.ConnectorConfig;
import com.pipeline.framework.api.connector.adapter.ConnectorAdapter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Connector适配器抽象基类。
 * <p>
 * 使用模板方法模式，定义适配流程的骨架。
 * 子类实现具体的适配逻辑。
 * </p>
 *
 * @param <CONN> Connector类型
 * @param <COMP> Component类型
 * @param <C>    配置类型
 * @author Pipeline Framework Team
 * @since 1.0.0
 */
public abstract class AbstractConnectorAdapter<CONN extends Connector<C>, COMP extends Component<?>, C extends ConnectorConfig>
    implements ConnectorAdapter<CONN, COMP, C> {

    protected final Logger logger = LoggerFactory.getLogger(getClass());
    protected final CONN connector;

    protected AbstractConnectorAdapter(CONN connector) {
        this.connector = connector;
        validate();
    }

    @Override
    public COMP adapt(CONN connector) {
        logger.debug("Adapting connector: {}", connector.getName());
        
        // 模板方法：前置处理
        preAdapt(connector);
        
        // 模板方法：执行适配
        COMP component = doAdapt(connector);
        
        // 模板方法：后置处理
        postAdapt(connector, component);
        
        logger.debug("Adapter completed for connector: {}", connector.getName());
        return component;
    }

    /**
     * 前置处理（钩子方法）。
     *
     * @param connector Connector实例
     */
    protected void preAdapt(CONN connector) {
        // 默认空实现，子类可覆盖
    }

    /**
     * 执行适配（抽象方法，子类必须实现）。
     *
     * @param connector Connector实例
     * @return Component实例
     */
    protected abstract COMP doAdapt(CONN connector);

    /**
     * 后置处理（钩子方法）。
     *
     * @param connector Connector实例
     * @param component Component实例
     */
    protected void postAdapt(CONN connector, COMP component) {
        // 默认空实现，子类可覆盖
    }

    @Override
    public CONN getConnector() {
        return connector;
    }

    @Override
    public boolean supports(CONN connector) {
        return connector != null && connector.validate();
    }

    /**
     * 验证Connector。
     *
     * @throws IllegalArgumentException 验证失败
     */
    protected void validate() {
        if (connector == null) {
            throw new IllegalArgumentException("Connector cannot be null");
        }
        if (!connector.validate()) {
            throw new IllegalArgumentException("Connector validation failed: " + connector.getName());
        }
    }
}
