package com.pipeline.framework.core.scheduler;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import reactor.core.scheduler.Scheduler;
import reactor.core.scheduler.Schedulers;

/**
 * Reactor 线程池配置。
 * <p>
 * 提供不同场景的 Scheduler：
 * <ul>
 *   <li>ioScheduler: IO 密集型操作（数据库、网络）</li>
 *   <li>computeScheduler: CPU 密集型操作（计算、转换）</li>
 *   <li>boundedElasticScheduler: 阻塞操作包装</li>
 *   <li>pipelineScheduler: Pipeline 执行专用</li>
 * </ul>
 *
 * @author Pipeline Framework Team
 * @since 1.0.0
 */
@Configuration
@EnableConfigurationProperties(ReactorSchedulerProperties.class)
public class ReactorSchedulerConfiguration {
    
    private static final Logger log = LoggerFactory.getLogger(ReactorSchedulerConfiguration.class);

    @Bean(name = "ioScheduler", destroyMethod = "dispose")
    public Scheduler ioScheduler(ReactorSchedulerProperties properties) {
        ReactorSchedulerProperties.SchedulerConfig ioConfig = properties.getIo();
        
        log.info("Initializing IO Scheduler: poolSize={}, queueSize={}", 
            ioConfig.getPoolSize(), ioConfig.getQueueSize());
        
        return Schedulers.newBoundedElastic(
            ioConfig.getPoolSize(),
            ioConfig.getQueueSize(),
            ioConfig.getThreadNamePrefix(),
            60,
            true
        );
    }

    @Bean(name = "computeScheduler", destroyMethod = "dispose")
    public Scheduler computeScheduler(ReactorSchedulerProperties properties) {
        ReactorSchedulerProperties.SchedulerConfig computeConfig = properties.getCompute();
        
        int poolSize = computeConfig.getPoolSize();
        if (poolSize <= 0) {
            poolSize = Runtime.getRuntime().availableProcessors();
        }
        
        log.info("Initializing Compute Scheduler: poolSize={}", poolSize);
        
        return Schedulers.newParallel(
            computeConfig.getThreadNamePrefix(),
            poolSize,
            true
        );
    }

    @Bean(name = "boundedElasticScheduler", destroyMethod = "dispose")
    public Scheduler boundedElasticScheduler(ReactorSchedulerProperties properties) {
        ReactorSchedulerProperties.BoundedElasticConfig config = properties.getBoundedElastic();
        
        log.info("Initializing Bounded Elastic Scheduler: poolSize={}, queueSize={}, ttl={}s",
            config.getPoolSize(), config.getQueueSize(), config.getTtlSeconds());
        
        return Schedulers.newBoundedElastic(
            config.getPoolSize(),
            config.getQueueSize(),
            config.getThreadNamePrefix(),
            config.getTtlSeconds(),
            true
        );
    }

    @Bean(name = "pipelineScheduler", destroyMethod = "dispose")
    public Scheduler pipelineScheduler(ReactorSchedulerProperties properties) {
        ReactorSchedulerProperties.SchedulerConfig pipelineConfig = properties.getPipeline();
        
        log.info("Initializing Pipeline Scheduler: poolSize={}, queueSize={}",
            pipelineConfig.getPoolSize(), pipelineConfig.getQueueSize());
        
        return Schedulers.newBoundedElastic(
            pipelineConfig.getPoolSize(),
            pipelineConfig.getQueueSize(),
            pipelineConfig.getThreadNamePrefix(),
            60,
            true
        );
    }
}
