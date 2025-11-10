package com.pipeline.framework.api.component;

import reactor.core.publisher.Mono;

/**
 * 组件基础接口。
 * <p>
 * 所有 Pipeline 组件（Source、Operator、Sink）的顶层抽象。
 * 提供通用的生命周期管理和元数据访问。
 * </p>
 *
 * @param <C> 组件配置类型
 * @author Pipeline Framework Team
 * @since 1.0.0
 */
public interface Component<C> {

    /**
     * 获取组件名称。
     *
     * @return 组件名称
     */
    String getName();

    /**
     * 获取组件类型。
     *
     * @return 组件类型
     */
    ComponentType getComponentType();

    /**
     * 获取组件配置。
     *
     * @return 组件配置
     */
    C getConfig();

    /**
     * 健康检查。
     *
     * @return 是否健康
     */
    default Mono<Boolean> healthCheck() {
        return Mono.just(true);
    }

    /**
     * 获取组件元数据。
     *
     * @return 元数据
     */
    default ComponentMetadata getMetadata() {
        return ComponentMetadata.builder()
            .name(getName())
            .type(getComponentType())
            .build();
    }
}
