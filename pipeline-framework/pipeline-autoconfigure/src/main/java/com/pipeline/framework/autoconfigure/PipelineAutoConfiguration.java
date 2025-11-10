package com.pipeline.framework.autoconfigure;

import com.pipeline.framework.core.builder.GraphPipelineBuilder;
import com.pipeline.framework.core.factory.OperatorFactory;
import com.pipeline.framework.core.factory.SinkFactory;
import com.pipeline.framework.core.factory.SourceFactory;
import com.pipeline.framework.core.graph.EnhancedGraphExecutor;
import com.pipeline.framework.core.graph.NodeExecutorRegistry;
import com.pipeline.framework.core.service.PipelineExecutionService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import reactor.core.scheduler.Scheduler;

/**
 * Pipeline框架主自动配置类。
 *
 * @author Pipeline Framework Team
 * @since 1.0.0
 */
@AutoConfiguration
@EnableConfigurationProperties(PipelineFrameworkProperties.class)
@ConditionalOnProperty(prefix = "pipeline.framework", name = "enabled", havingValue = "true", matchIfMissing = true)
public class PipelineAutoConfiguration {

    private static final Logger log = LoggerFactory.getLogger(PipelineAutoConfiguration.class);

    public PipelineAutoConfiguration() {
        log.info("Pipeline Framework Auto Configuration initialized");
    }

    @Bean
    @ConditionalOnMissingBean
    public SourceFactory sourceFactory() {
        log.info("Creating SourceFactory bean");
        return new SourceFactory();
    }

    @Bean
    @ConditionalOnMissingBean
    public OperatorFactory operatorFactory() {
        log.info("Creating OperatorFactory bean");
        return new OperatorFactory();
    }

    @Bean
    @ConditionalOnMissingBean
    public SinkFactory sinkFactory() {
        log.info("Creating SinkFactory bean");
        return new SinkFactory();
    }

    @Bean
    @ConditionalOnMissingBean
    public NodeExecutorRegistry nodeExecutorRegistry() {
        log.info("Creating NodeExecutorRegistry bean");
        return new NodeExecutorRegistry();
    }

    @Bean
    @ConditionalOnMissingBean
    public EnhancedGraphExecutor enhancedGraphExecutor(
            SourceFactory sourceFactory,
            OperatorFactory operatorFactory,
            SinkFactory sinkFactory,
            NodeExecutorRegistry nodeExecutorRegistry) {
        log.info("Creating EnhancedGraphExecutor bean");
        return new EnhancedGraphExecutor(sourceFactory, operatorFactory, sinkFactory, nodeExecutorRegistry);
    }

    @Bean
    @ConditionalOnMissingBean
    public GraphPipelineBuilder graphPipelineBuilder(
            SourceFactory sourceFactory,
            OperatorFactory operatorFactory,
            SinkFactory sinkFactory) {
        log.info("Creating GraphPipelineBuilder bean");
        return new GraphPipelineBuilder(sourceFactory, operatorFactory, sinkFactory);
    }

    @Bean
    @ConditionalOnMissingBean
    public PipelineExecutionService pipelineExecutionService(
            EnhancedGraphExecutor graphExecutor,
            Scheduler pipelineScheduler) {
        log.info("Creating PipelineExecutionService bean");
        return new PipelineExecutionService(graphExecutor, pipelineScheduler);
    }
}
