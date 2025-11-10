package com.pipeline.framework.core.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import reactor.core.scheduler.Scheduler;
import reactor.core.scheduler.Schedulers;

import java.time.Duration;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicLong;

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
public class ReactorSchedulerConfig {
    
    private static final Logger log = LoggerFactory.getLogger(ReactorSchedulerConfig.class);

    /**
     * IO 密集型操作调度器。
     * <p>
     * 适用场景：
     * - 数据库查询
     * - HTTP 请求
     * - 文件读写
     * - 消息队列操作
     * </p>
     */
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

    /**
     * CPU 密集型操作调度器。
     * <p>
     * 适用场景：
     * - 数据转换
     * - 计算密集型任务
     * - 数据聚合
     * </p>
     */
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

    /**
     * 有界弹性调度器。
     * <p>
     * 适用场景：
     * - 包装阻塞 API（如 JDBC）
     * - 同步第三方库调用
     * - 文件系统操作
     * </p>
     */
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

    /**
     * Pipeline 执行专用调度器。
     * <p>
     * 适用场景：
     * - Pipeline 主流程执行
     * - Job 调度
     * - Graph 构建和执行
     * </p>
     */
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

    /**
     * 自定义线程工厂。
     */
    private static class NamedThreadFactory implements ThreadFactory {
        private final String namePrefix;
        private final AtomicLong counter = new AtomicLong(0);
        private final boolean daemon;

        public NamedThreadFactory(String namePrefix, boolean daemon) {
            this.namePrefix = namePrefix;
            this.daemon = daemon;
        }

        @Override
        public Thread newThread(Runnable r) {
            Thread thread = new Thread(r, namePrefix + counter.incrementAndGet());
            thread.setDaemon(daemon);
            return thread;
        }
    }
}
