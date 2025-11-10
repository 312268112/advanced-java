package com.pipeline.framework.core.pipeline;

import com.pipeline.framework.api.sink.DataSink;
import com.pipeline.framework.api.source.DataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Mono;
import reactor.core.publisher.Flux;

import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Pipeline默认实现。
 * <p>
 * 核心流程：Source.read() → OperatorChain.execute() → Sink.write()
 * </p>
 *
 * @param <IN>  输入类型
 * @param <OUT> 输出类型
 * @author Pipeline Framework Team
 * @since 1.0.0
 */
public class DefaultPipeline<IN, OUT> implements Pipeline<IN, OUT> {
    
    private static final Logger log = LoggerFactory.getLogger(DefaultPipeline.class);
    
    private final String name;
    private final DataSource<IN> source;
    private final OperatorChain<IN, OUT> operatorChain;
    private final DataSink<OUT> sink;
    
    private final AtomicBoolean running = new AtomicBoolean(false);
    private final AtomicLong recordsProcessed = new AtomicLong(0);

    public DefaultPipeline(String name,
                          DataSource<IN> source,
                          OperatorChain<IN, OUT> operatorChain,
                          DataSink<OUT> sink) {
        this.name = name;
        this.source = source;
        this.operatorChain = operatorChain;
        this.sink = sink;
    }

    @Override
    public DataSource<IN> getSource() {
        return source;
    }

    @Override
    public OperatorChain<IN, OUT> getOperatorChain() {
        return operatorChain;
    }

    @Override
    public DataSink<OUT> getSink() {
        return sink;
    }

    @Override
    public Mono<PipelineResult> execute() {
        if (!running.compareAndSet(false, true)) {
            return Mono.error(new IllegalStateException("Pipeline is already running"));
        }
        
        log.info("Starting pipeline: {}", name);
        Instant startTime = Instant.now();
        
        return Mono.defer(() -> {
            // 1. 启动Source
            return source.start()
                .then(Mono.defer(() -> {
                    // 2. 启动Sink
                    return sink.start();
                }))
                .then(Mono.defer(() -> {
                    // 3. 构建数据流
                    return executePipeline();
                }))
                .then(Mono.defer(() -> {
                    // 4. 创建执行结果
                    Instant endTime = Instant.now();
                    Duration duration = Duration.between(startTime, endTime);
                    
                    return Mono.just(new DefaultPipelineResult(
                        true,
                        startTime,
                        endTime,
                        duration,
                        recordsProcessed.get(),
                        null,
                        null
                    ));
                }));
        })
        .doOnSuccess(result -> {
            running.set(false);
            log.info("Pipeline completed: {}, duration: {}ms, records: {}", 
                name, result.getDuration().toMillis(), result.getRecordsProcessed());
        })
        .doOnError(error -> {
            running.set(false);
            log.error("Pipeline failed: {}", name, error);
        })
        .onErrorResume(error -> {
            Instant endTime = Instant.now();
            Duration duration = Duration.between(startTime, endTime);
            
            return Mono.just(new DefaultPipelineResult(
                false,
                startTime,
                endTime,
                duration,
                recordsProcessed.get(),
                error.getMessage(),
                error
            ));
        });
    }

    /**
     * 执行Pipeline的核心逻辑。
     * <p>
     * 关键：使用响应式流连接Source、Operator Chain和Sink
     * </p>
     */
    private Mono<Void> executePipeline() {
        return Mono.defer(() -> {
            // 从Source读取数据
            Flux<IN> sourceFlux = source.read()
                .doOnNext(data -> {
                    log.trace("Read from source: {}", data);
                })
                .doOnError(e -> log.error("Source error", e));
            
            // 通过算子链处理
            Flux<OUT> processedFlux = operatorChain.execute(sourceFlux)
                .doOnNext(data -> {
                    recordsProcessed.incrementAndGet();
                    log.trace("Processed data: {}", data);
                })
                .doOnError(e -> log.error("Operator chain error", e));
            
            // 写入Sink
            return sink.write(processedFlux)
                .doOnSuccess(v -> log.debug("Sink write completed"))
                .doOnError(e -> log.error("Sink error", e));
        });
    }

    @Override
    public Mono<Void> stop() {
        log.info("Stopping pipeline: {}", name);
        
        return Mono.when(
            source.stop()
                .doOnSuccess(v -> log.debug("Source stopped"))
                .onErrorResume(e -> {
                    log.warn("Error stopping source", e);
                    return Mono.empty();
                }),
            sink.stop()
                .doOnSuccess(v -> log.debug("Sink stopped"))
                .onErrorResume(e -> {
                    log.warn("Error stopping sink", e);
                    return Mono.empty();
                })
        )
        .doFinally(signal -> {
            running.set(false);
            log.info("Pipeline stopped: {}", name);
        });
    }

    @Override
    public Mono<Void> forceStop() {
        log.warn("Force stopping pipeline: {}", name);
        running.set(false);
        
        return Mono.when(
            source.stop().onErrorResume(e -> Mono.empty()),
            sink.stop().onErrorResume(e -> Mono.empty())
        ).timeout(Duration.ofSeconds(5))
        .onErrorResume(e -> {
            log.error("Force stop timeout", e);
            return Mono.empty();
        });
    }

    @Override
    public boolean isRunning() {
        return running.get();
    }

    @Override
    public String getName() {
        return name;
    }
}
