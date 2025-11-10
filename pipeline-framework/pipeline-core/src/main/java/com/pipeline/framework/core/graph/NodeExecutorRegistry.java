package com.pipeline.framework.core.graph;

import com.pipeline.framework.api.graph.NodeExecutor;
import com.pipeline.framework.api.graph.NodeType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 节点执行器注册表。
 * <p>
 * 使用策略模式，管理所有节点执行器。
 * Spring 自动注入所有 NodeExecutor 实现。
 * </p>
 *
 * @author Pipeline Framework Team
 * @since 1.0.0
 */
@Component
public class NodeExecutorRegistry {
    
    private static final Logger log = LoggerFactory.getLogger(NodeExecutorRegistry.class);
    
    private final Map<NodeType, NodeExecutor<?>> executorMap = new ConcurrentHashMap<>();

    /**
     * 构造函数注入所有 NodeExecutor。
     *
     * @param executors 所有 NodeExecutor 实现
     */
    public NodeExecutorRegistry(List<NodeExecutor<?>> executors) {
        for (NodeExecutor<?> executor : executors) {
            NodeType type = executor.getSupportedNodeType();
            executorMap.put(type, executor);
            log.info("Registered NodeExecutor: type={}, class={}", 
                type, executor.getClass().getSimpleName());
        }
        log.info("Total {} NodeExecutors registered", executorMap.size());
    }

    /**
     * 获取指定类型的节点执行器。
     *
     * @param nodeType 节点类型
     * @param <T>      数据类型
     * @return 节点执行器
     */
    @SuppressWarnings("unchecked")
    public <T> NodeExecutor<T> getExecutor(NodeType nodeType) {
        NodeExecutor<T> executor = (NodeExecutor<T>) executorMap.get(nodeType);
        
        if (executor == null) {
            throw new IllegalArgumentException(
                "No executor found for node type: " + nodeType + 
                ". Available types: " + executorMap.keySet());
        }
        
        return executor;
    }

    /**
     * 注册自定义执行器。
     *
     * @param executor 执行器
     */
    public void registerExecutor(NodeExecutor<?> executor) {
        NodeType type = executor.getSupportedNodeType();
        executorMap.put(type, executor);
        log.info("Custom NodeExecutor registered: type={}", type);
    }

    /**
     * 获取所有支持的节点类型。
     *
     * @return 节点类型列表
     */
    public List<NodeType> getSupportedTypes() {
        return List.copyOf(executorMap.keySet());
    }
}
