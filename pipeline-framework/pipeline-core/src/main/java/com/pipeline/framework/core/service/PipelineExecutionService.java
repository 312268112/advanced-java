package com.pipeline.framework.core.service;

import com.pipeline.framework.api.graph.StreamGraph;
import com.pipeline.framework.core.builder.GraphPipelineBuilder;
import com.pipeline.framework.core.pipeline.Pipeline;
import com.pipeline.framework.core.pipeline.PipelineResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Scheduler;

/**
 * Pipeline 执行服务。
 * <p>
 * 提供统一的 Pipeline 执行入口。
 * </p>
 *
 * @author Pipeline Framework Team
 * @since 1.0.0
 */
@Service
public class PipelineExecutionService {
    
    private static final Logger log = LoggerFactory.getLogger(PipelineExecutionService.class);
    
    private final GraphPipelineBuilder pipelineBuilder;
    private final Scheduler pipelineScheduler;

    public PipelineExecutionService(
            GraphPipelineBuilder pipelineBuilder,
            @Qualifier("pipelineScheduler") Scheduler pipelineScheduler) {
        this.pipelineBuilder = pipelineBuilder;
        this.pipelineScheduler = pipelineScheduler;
        log.info("PipelineExecutionService initialized");
    }

    /**
     * 执行 Pipeline。
     * <p>
     * 完整流程：
     * 1. 从 Graph 构建 Pipeline
     * 2. 执行 Pipeline
     * 3. 返回结果
     * </p>
     *
     * @param graph StreamGraph 定义
     * @return 执行结果的 Mono
     */
    public Mono<PipelineResult> execute(StreamGraph graph) {
        log.info("Executing pipeline: {}", graph.getGraphId());
        
        return pipelineBuilder.buildFromGraph(graph)
            .flatMap(Pipeline::execute)
            .subscribeOn(pipelineScheduler)
            .doOnSuccess(result -> {
                if (result.isSuccess()) {
                    log.info("Pipeline execution succeeded: {} records in {} ms",
                        result.getRecordsProcessed(),
                        result.getDuration().toMillis());
                } else {
                    log.error("Pipeline execution failed: {}", result.getErrorMessage());
                }
            })
            .doOnError(e -> log.error("Pipeline execution error: {}", graph.getGraphId(), e));
    }

    /**
     * 异步执行 Pipeline（fire-and-forget）。
     *
     * @param graph StreamGraph 定义
     */
    public void executeAsync(StreamGraph graph) {
        execute(graph)
            .subscribe(
                result -> log.info("Async pipeline completed: {}", graph.getGraphId()),
                error -> log.error("Async pipeline failed: {}", graph.getGraphId(), error)
            );
    }
}
