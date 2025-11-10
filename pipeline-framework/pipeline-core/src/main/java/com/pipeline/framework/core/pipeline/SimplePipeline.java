package com.pipeline.framework.core.pipeline;

import com.pipeline.framework.api.component.Component;
import com.pipeline.framework.api.operator.Operator;
import com.pipeline.framework.api.sink.DataSink;
import com.pipeline.framework.api.source.DataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

/**
 * 简化的 Pipeline 实现。
 * <p>
 * 核心逻辑：直接串联 Source.read() → Operators → Sink.write()
 * 使用泛型增强类型安全。
 * </p>
 *
 * @param <IN>  输入类型
 * @param <OUT> 输出类型
 * @author Pipeline Framework Team
 * @since 1.0.0
 */
public class SimplePipeline<IN, OUT> implements Pipeline<IN, OUT> {
    
    private static final Logger log = LoggerFactory.getLogger(SimplePipeline.class);
    
    private final String name;
    private final DataSource<IN> source;
    private final List<Operator<?, ?>> operators;
    private final DataSink<OUT> sink;
    
    private final AtomicBoolean running = new AtomicBoolean(false);
    private final AtomicLong recordsProcessed = new AtomicLong(0);

    public SimplePipeline(String name,
                         DataSource<IN> source,
                         List<Operator<?, ?>> operators,
                         DataSink<OUT> sink) {
        this.name = name;
        this.source = source;
        this.operators = operators;
        this.sink = sink;
        
        log.info("Pipeline created: name={}, source={}, operators={}, sink={}",
            name, source.getName(), 
            operators.stream().map(Component::getName).collect(Collectors.joining(", ")),
            sink.getName());
    }

    @Override
    public DataSource<IN> getSource() {
        return source;
    }

    @Override
    public DataSink<OUT> getSink() {
        return sink;
    }

    @Override
    public List<Operator<?, ?>> getOperators() {
        return List.copyOf(operators);
    }

    /**
     * 执行 Pipeline 的核心方法。
     * <p>
     * 清晰的执行流程：
     * 1. 从 Source 读取数据流 (Flux)
     * 2. 依次通过每个 Operator 转换
     * 3. 最终写入 Sink
     * 4. 返回执行结果
     * </p>
     */
    @Override
    public Mono<PipelineResult> execute() {
        if (!running.compareAndSet(false, true)) {
            return Mono.error(new IllegalStateException("Pipeline is already running: " + name));
        }
        
        log.info("=== Starting Pipeline: {} ===", name);
        Instant startTime = Instant.now();
        
        return Mono.defer(() -> {
            try {
                // 核心逻辑：构建完整的响应式流
                Flux<OUT> dataFlow = buildDataFlow();
                
                // 执行流并写入 Sink
                return sink.write(dataFlow)
                    .then(Mono.defer(() -> {
                        // 创建执行结果
                        Instant endTime = Instant.now();
                        Duration duration = Duration.between(startTime, endTime);
                        
                        PipelineResult result = new DefaultPipelineResult(
                            true,
                            startTime,
                            endTime,
                            duration,
                            recordsProcessed.get(),
                            null,
                            null
                        );
                        
                        log.info("=== Pipeline Completed: {} ===", name);
                        log.info("Duration: {} ms", duration.toMillis());
                        log.info("Records: {}", recordsProcessed.get());
                        
                        return Mono.just(result);
                    }));
                    
            } catch (Exception e) {
                log.error("Failed to build pipeline: {}", name, e);
                return Mono.error(e);
            }
        })
        .doFinally(signal -> {
            running.set(false);
            log.info("=== Pipeline Finished: {} (signal: {}) ===", name, signal);
        })
        .onErrorResume(error -> {
            log.error("=== Pipeline Failed: {} ===", name, error);
            Instant endTime = Instant.now();
            Duration duration = Duration.between(startTime, endTime);
            
            PipelineResult result = new DefaultPipelineResult(
                false,
                startTime,
                endTime,
                duration,
                recordsProcessed.get(),
                error.getMessage(),
                error
            );
            
            return Mono.just(result);
        });
    }

    /**
     * 构建完整的数据流。
     * <p>
     * 这是 Pipeline 的核心：将 Source、Operators、Sink 串联成一个响应式流。
     * 使用泛型确保类型安全。
     * </p>
     */
    @SuppressWarnings("unchecked")
    private Flux<OUT> buildDataFlow() {
        log.debug("Building data flow for pipeline: {}", name);
        
        // 1. 从 Source 读取数据
        Flux<?> dataFlow = source.read()
            .doOnSubscribe(s -> log.info("Source started: {}", source.getName()))
            .doOnNext(data -> {
                recordsProcessed.incrementAndGet();
                log.trace("Read from source: {}", data);
            })
            .doOnComplete(() -> log.info("Source completed: {}", source.getName()))
            .doOnError(e -> log.error("Source error: {}", source.getName(), e));
        
        // 2. 依次通过每个 Operator
        for (int i = 0; i < operators.size(); i++) {
            Operator<Object, Object> operator = (Operator<Object, Object>) operators.get(i);
            final int index = i;
            
            dataFlow = operator.apply((Flux<Object>) dataFlow)
                .doOnSubscribe(s -> log.debug("Operator[{}] started: {}", index, operator.getName()))
                .doOnNext(data -> log.trace("Operator[{}] processed: {}", index, data))
                .doOnComplete(() -> log.debug("Operator[{}] completed: {}", index, operator.getName()))
                .doOnError(e -> log.error("Operator[{}] error: {}", index, operator.getName(), e));
        }
        
        log.debug("Data flow built with {} operators", operators.size());
        return (Flux<OUT>) dataFlow;
    }

    @Override
    public Mono<Void> stop() {
        log.info("Stopping pipeline: {}", name);
        running.set(false);
        return Mono.empty();
    }

    @Override
    public Mono<Void> forceStop() {
        log.warn("Force stopping pipeline: {}", name);
        running.set(false);
        return Mono.empty();
    }

    @Override
    public boolean isRunning() {
        return running.get();
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public long getRecordsProcessed() {
        return recordsProcessed.get();
    }
}
