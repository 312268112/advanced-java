package com.pipeline.framework.core.builder;

import com.pipeline.framework.api.operator.Operator;
import com.pipeline.framework.api.sink.DataSink;
import com.pipeline.framework.api.source.DataSource;
import com.pipeline.framework.core.pipeline.Pipeline;
import com.pipeline.framework.core.pipeline.OperatorChain;
import com.pipeline.framework.core.pipeline.DefaultPipeline;
import com.pipeline.framework.core.pipeline.DefaultOperatorChain;

import java.util.ArrayList;
import java.util.List;

/**
 * Pipeline构建器。
 * <p>
 * 使用Builder模式构建Pipeline，支持链式调用。
 * </p>
 *
 * @param <IN>  初始输入类型
 * @param <OUT> 最终输出类型
 * @author Pipeline Framework Team
 * @since 1.0.0
 */
public class PipelineBuilder<IN, OUT> {
    
    private String name;
    private DataSource<IN> source;
    private final List<Operator<?, ?>> operators = new ArrayList<>();
    private DataSink<OUT> sink;
    
    private PipelineBuilder() {
    }
    
    public static <T> PipelineBuilder<T, T> create() {
        return new PipelineBuilder<>();
    }
    
    /**
     * 设置Pipeline名称。
     */
    public PipelineBuilder<IN, OUT> name(String name) {
        this.name = name;
        return this;
    }
    
    /**
     * 设置数据源。
     */
    public PipelineBuilder<IN, OUT> source(DataSource<IN> source) {
        this.source = source;
        return this;
    }
    
    /**
     * 添加算子。
     * <p>
     * 注意：这里使用了类型转换技巧，实际使用时需要确保类型匹配。
     * </p>
     */
    @SuppressWarnings("unchecked")
    public <NEXT> PipelineBuilder<IN, NEXT> addOperator(Operator<OUT, NEXT> operator) {
        operators.add(operator);
        return (PipelineBuilder<IN, NEXT>) this;
    }
    
    /**
     * 设置数据输出。
     */
    public PipelineBuilder<IN, OUT> sink(DataSink<OUT> sink) {
        this.sink = sink;
        return this;
    }
    
    /**
     * 构建Pipeline。
     */
    @SuppressWarnings("unchecked")
    public Pipeline<IN, OUT> build() {
        if (source == null) {
            throw new IllegalStateException("Source is required");
        }
        if (sink == null) {
            throw new IllegalStateException("Sink is required");
        }
        
        // 构建算子链
        OperatorChain<IN, OUT> operatorChain = buildOperatorChain();
        
        // 创建Pipeline
        return new DefaultPipeline<>(
            name != null ? name : "pipeline-" + System.currentTimeMillis(),
            source,
            operatorChain,
            sink
        );
    }
    
    /**
     * 构建算子链。
     */
    @SuppressWarnings("unchecked")
    private OperatorChain<IN, OUT> buildOperatorChain() {
        if (operators.isEmpty()) {
            // 没有算子，创建空链
            return new DefaultOperatorChain<>(new ArrayList<>());
        }
        
        // 有算子，创建链
        return new DefaultOperatorChain<>((List<Operator<?, ?>>) (List<?>) operators);
    }
}
