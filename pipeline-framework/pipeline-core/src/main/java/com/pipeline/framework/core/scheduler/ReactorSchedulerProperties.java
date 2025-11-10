package com.pipeline.framework.core.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Reactor Scheduler 配置属性。
 *
 * @author Pipeline Framework Team
 * @since 1.0.0
 */
@Component
@ConfigurationProperties(prefix = "reactor.scheduler")
public class ReactorSchedulerProperties {

    private SchedulerConfig io = new SchedulerConfig();
    private SchedulerConfig compute = new SchedulerConfig();
    private BoundedElasticConfig boundedElastic = new BoundedElasticConfig();
    private SchedulerConfig pipeline = new SchedulerConfig();

    public SchedulerConfig getIo() {
        return io;
    }

    public void setIo(SchedulerConfig io) {
        this.io = io;
    }

    public SchedulerConfig getCompute() {
        return compute;
    }

    public void setCompute(SchedulerConfig compute) {
        this.compute = compute;
    }

    public BoundedElasticConfig getBoundedElastic() {
        return boundedElastic;
    }

    public void setBoundedElastic(BoundedElasticConfig boundedElastic) {
        this.boundedElastic = boundedElastic;
    }

    public SchedulerConfig getPipeline() {
        return pipeline;
    }

    public void setPipeline(SchedulerConfig pipeline) {
        this.pipeline = pipeline;
    }

    /**
     * 基础调度器配置。
     */
    public static class SchedulerConfig {
        private int poolSize = 10;
        private int queueSize = 1000;
        private String threadNamePrefix = "reactor-";

        public int getPoolSize() {
            return poolSize;
        }

        public void setPoolSize(int poolSize) {
            this.poolSize = poolSize;
        }

        public int getQueueSize() {
            return queueSize;
        }

        public void setQueueSize(int queueSize) {
            this.queueSize = queueSize;
        }

        public String getThreadNamePrefix() {
            return threadNamePrefix;
        }

        public void setThreadNamePrefix(String threadNamePrefix) {
            this.threadNamePrefix = threadNamePrefix;
        }
    }

    /**
     * 有界弹性调度器配置。
     */
    public static class BoundedElasticConfig extends SchedulerConfig {
        private int ttlSeconds = 60;

        public int getTtlSeconds() {
            return ttlSeconds;
        }

        public void setTtlSeconds(int ttlSeconds) {
            this.ttlSeconds = ttlSeconds;
        }
    }
}
