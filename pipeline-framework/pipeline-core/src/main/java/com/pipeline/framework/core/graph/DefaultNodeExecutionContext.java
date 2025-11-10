package com.pipeline.framework.core.graph;

import com.pipeline.framework.api.graph.NodeExecutionContext;
import com.pipeline.framework.api.graph.StreamGraph;
import com.pipeline.framework.api.operator.Operator;
import com.pipeline.framework.api.sink.DataSink;
import com.pipeline.framework.api.source.DataSource;
import reactor.core.publisher.Flux;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 默认的节点执行上下文实现。
 *
 * @author Pipeline Framework Team
 * @since 1.0.0
 */
public class DefaultNodeExecutionContext implements NodeExecutionContext {
    
    private final StreamGraph graph;
    private final Map<String, DataSource<?>> sources;
    private final Map<String, Operator<?, ?>> operators;
    private final Map<String, DataSink<?>> sinks;
    private final Map<String, Flux<?>> fluxCache;
    private final Map<String, Object> attributes;

    public DefaultNodeExecutionContext(StreamGraph graph,
                                      Map<String, DataSource<?>> sources,
                                      Map<String, Operator<?, ?>> operators,
                                      Map<String, DataSink<?>> sinks) {
        this.graph = graph;
        this.sources = sources;
        this.operators = operators;
        this.sinks = sinks;
        this.fluxCache = new ConcurrentHashMap<>();
        this.attributes = new ConcurrentHashMap<>();
    }

    @Override
    public StreamGraph getGraph() {
        return graph;
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> Optional<DataSource<T>> getSource(String nodeId) {
        return Optional.ofNullable((DataSource<T>) sources.get(nodeId));
    }

    @Override
    @SuppressWarnings("unchecked")
    public <IN, OUT> Optional<Operator<IN, OUT>> getOperator(String nodeId) {
        return Optional.ofNullable((Operator<IN, OUT>) operators.get(nodeId));
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> Optional<DataSink<T>> getSink(String nodeId) {
        return Optional.ofNullable((DataSink<T>) sinks.get(nodeId));
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> Optional<Flux<T>> getCachedFlux(String nodeId) {
        return Optional.ofNullable((Flux<T>) fluxCache.get(nodeId));
    }

    @Override
    public <T> void cacheFlux(String nodeId, Flux<T> flux) {
        fluxCache.put(nodeId, flux);
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> Optional<T> getAttribute(String key) {
        return Optional.ofNullable((T) attributes.get(key));
    }

    @Override
    public void setAttribute(String key, Object value) {
        attributes.put(key, value);
    }
}
