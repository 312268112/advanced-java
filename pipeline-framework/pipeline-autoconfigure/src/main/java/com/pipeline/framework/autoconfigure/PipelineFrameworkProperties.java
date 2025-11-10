package com.pipeline.framework.autoconfigure;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Pipeline框架配置属性。
 *
 * @author Pipeline Framework Team
 * @since 1.0.0
 */
@ConfigurationProperties(prefix = "pipeline.framework")
public class PipelineFrameworkProperties {

    /**
     * 是否启用Pipeline框架
     */
    private boolean enabled = true;

    /**
     * 执行器配置
     */
    private ExecutorProperties executor = new ExecutorProperties();

    /**
     * 调度器配置
     */
    private SchedulerProperties scheduler = new SchedulerProperties();

    /**
     * 检查点配置
     */
    private CheckpointProperties checkpoint = new CheckpointProperties();

    /**
     * 指标配置
     */
    private MetricsProperties metrics = new MetricsProperties();

    /**
     * 状态管理配置
     */
    private StateProperties state = new StateProperties();

    /**
     * SQL批量任务配置
     */
    private SqlBatchProperties sqlBatch = new SqlBatchProperties();

    // Getters and Setters
    
    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public ExecutorProperties getExecutor() {
        return executor;
    }

    public void setExecutor(ExecutorProperties executor) {
        this.executor = executor;
    }

    public SchedulerProperties getScheduler() {
        return scheduler;
    }

    public void setScheduler(SchedulerProperties scheduler) {
        this.scheduler = scheduler;
    }

    public CheckpointProperties getCheckpoint() {
        return checkpoint;
    }

    public void setCheckpoint(CheckpointProperties checkpoint) {
        this.checkpoint = checkpoint;
    }

    public MetricsProperties getMetrics() {
        return metrics;
    }

    public void setMetrics(MetricsProperties metrics) {
        this.metrics = metrics;
    }

    public StateProperties getState() {
        return state;
    }

    public void setState(StateProperties state) {
        this.state = state;
    }

    public SqlBatchProperties getSqlBatch() {
        return sqlBatch;
    }

    public void setSqlBatch(SqlBatchProperties sqlBatch) {
        this.sqlBatch = sqlBatch;
    }

    /**
     * 执行器配置
     */
    public static class ExecutorProperties {
        /**
         * 核心线程池大小
         */
        private int corePoolSize = 10;

        /**
         * 最大线程池大小
         */
        private int maxPoolSize = 50;

        /**
         * 队列容量
         */
        private int queueCapacity = 500;

        /**
         * 线程空闲时间（秒）
         */
        private int keepAliveSeconds = 60;

        /**
         * 线程名称前缀
         */
        private String threadNamePrefix = "pipeline-exec-";

        /**
         * 任务执行超时时间（秒），0表示不超时
         */
        private long executionTimeoutSeconds = 0;

        /**
         * 是否允许核心线程超时
         */
        private boolean allowCoreThreadTimeout = false;

        // Getters and Setters

        public int getCorePoolSize() {
            return corePoolSize;
        }

        public void setCorePoolSize(int corePoolSize) {
            this.corePoolSize = corePoolSize;
        }

        public int getMaxPoolSize() {
            return maxPoolSize;
        }

        public void setMaxPoolSize(int maxPoolSize) {
            this.maxPoolSize = maxPoolSize;
        }

        public int getQueueCapacity() {
            return queueCapacity;
        }

        public void setQueueCapacity(int queueCapacity) {
            this.queueCapacity = queueCapacity;
        }

        public int getKeepAliveSeconds() {
            return keepAliveSeconds;
        }

        public void setKeepAliveSeconds(int keepAliveSeconds) {
            this.keepAliveSeconds = keepAliveSeconds;
        }

        public String getThreadNamePrefix() {
            return threadNamePrefix;
        }

        public void setThreadNamePrefix(String threadNamePrefix) {
            this.threadNamePrefix = threadNamePrefix;
        }

        public long getExecutionTimeoutSeconds() {
            return executionTimeoutSeconds;
        }

        public void setExecutionTimeoutSeconds(long executionTimeoutSeconds) {
            this.executionTimeoutSeconds = executionTimeoutSeconds;
        }

        public boolean isAllowCoreThreadTimeout() {
            return allowCoreThreadTimeout;
        }

        public void setAllowCoreThreadTimeout(boolean allowCoreThreadTimeout) {
            this.allowCoreThreadTimeout = allowCoreThreadTimeout;
        }
    }

    /**
     * 调度器配置
     */
    public static class SchedulerProperties {
        /**
         * 调度线程池大小
         */
        private int poolSize = 5;

        /**
         * 调度间隔检查时间（毫秒）
         */
        private long scheduleCheckIntervalMs = 1000;

        /**
         * 是否启用调度器
         */
        private boolean enabled = true;

        // Getters and Setters

        public int getPoolSize() {
            return poolSize;
        }

        public void setPoolSize(int poolSize) {
            this.poolSize = poolSize;
        }

        public long getScheduleCheckIntervalMs() {
            return scheduleCheckIntervalMs;
        }

        public void setScheduleCheckIntervalMs(long scheduleCheckIntervalMs) {
            this.scheduleCheckIntervalMs = scheduleCheckIntervalMs;
        }

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }
    }

    /**
     * 检查点配置
     */
    public static class CheckpointProperties {
        /**
         * 是否启用检查点
         */
        private boolean enabled = true;

        /**
         * 检查点间隔（秒）
         */
        private int intervalSeconds = 60;

        /**
         * 检查点超时时间（秒）
         */
        private int timeoutSeconds = 300;

        /**
         * 最小检查点间隔（秒）
         */
        private int minPauseBetweenSeconds = 10;

        /**
         * 最大并发检查点数
         */
        private int maxConcurrentCheckpoints = 1;

        /**
         * 检查点存储路径
         */
        private String storagePath = "./checkpoints";

        /**
         * 是否启用外部化检查点
         */
        private boolean externalizedCheckpoint = false;

        /**
         * 保留的检查点数量
         */
        private int retainedCheckpoints = 3;

        // Getters and Setters

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        public int getIntervalSeconds() {
            return intervalSeconds;
        }

        public void setIntervalSeconds(int intervalSeconds) {
            this.intervalSeconds = intervalSeconds;
        }

        public int getTimeoutSeconds() {
            return timeoutSeconds;
        }

        public void setTimeoutSeconds(int timeoutSeconds) {
            this.timeoutSeconds = timeoutSeconds;
        }

        public int getMinPauseBetweenSeconds() {
            return minPauseBetweenSeconds;
        }

        public void setMinPauseBetweenSeconds(int minPauseBetweenSeconds) {
            this.minPauseBetweenSeconds = minPauseBetweenSeconds;
        }

        public int getMaxConcurrentCheckpoints() {
            return maxConcurrentCheckpoints;
        }

        public void setMaxConcurrentCheckpoints(int maxConcurrentCheckpoints) {
            this.maxConcurrentCheckpoints = maxConcurrentCheckpoints;
        }

        public String getStoragePath() {
            return storagePath;
        }

        public void setStoragePath(String storagePath) {
            this.storagePath = storagePath;
        }

        public boolean isExternalizedCheckpoint() {
            return externalizedCheckpoint;
        }

        public void setExternalizedCheckpoint(boolean externalizedCheckpoint) {
            this.externalizedCheckpoint = externalizedCheckpoint;
        }

        public int getRetainedCheckpoints() {
            return retainedCheckpoints;
        }

        public void setRetainedCheckpoints(int retainedCheckpoints) {
            this.retainedCheckpoints = retainedCheckpoints;
        }
    }

    /**
     * 指标配置
     */
    public static class MetricsProperties {
        /**
         * 是否启用指标收集
         */
        private boolean enabled = true;

        /**
         * 指标上报间隔（秒）
         */
        private int reportIntervalSeconds = 30;

        /**
         * 是否启用JVM指标
         */
        private boolean jvmMetrics = true;

        /**
         * 是否启用系统指标
         */
        private boolean systemMetrics = true;

        /**
         * 指标前缀
         */
        private String prefix = "pipeline.framework";

        // Getters and Setters

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        public int getReportIntervalSeconds() {
            return reportIntervalSeconds;
        }

        public void setReportIntervalSeconds(int reportIntervalSeconds) {
            this.reportIntervalSeconds = reportIntervalSeconds;
        }

        public boolean isJvmMetrics() {
            return jvmMetrics;
        }

        public void setJvmMetrics(boolean jvmMetrics) {
            this.jvmMetrics = jvmMetrics;
        }

        public boolean isSystemMetrics() {
            return systemMetrics;
        }

        public void setSystemMetrics(boolean systemMetrics) {
            this.systemMetrics = systemMetrics;
        }

        public String getPrefix() {
            return prefix;
        }

        public void setPrefix(String prefix) {
            this.prefix = prefix;
        }
    }

    /**
     * 状态管理配置
     */
    public static class StateProperties {
        /**
         * 状态后端类型: memory, rocksdb
         */
        private String backend = "memory";

        /**
         * 状态存储路径
         */
        private String storagePath = "./state";

        /**
         * 是否启用增量检查点
         */
        private boolean incrementalCheckpoints = false;

        // Getters and Setters

        public String getBackend() {
            return backend;
        }

        public void setBackend(String backend) {
            this.backend = backend;
        }

        public String getStoragePath() {
            return storagePath;
        }

        public void setStoragePath(String storagePath) {
            this.storagePath = storagePath;
        }

        public boolean isIncrementalCheckpoints() {
            return incrementalCheckpoints;
        }

        public void setIncrementalCheckpoints(boolean incrementalCheckpoints) {
            this.incrementalCheckpoints = incrementalCheckpoints;
        }
    }

    /**
     * SQL批量任务配置
     */
    public static class SqlBatchProperties {
        /**
         * 是否启用SQL批量任务
         */
        private boolean enabled = true;

        /**
         * 默认批次大小
         */
        private int batchSize = 1000;

        /**
         * 默认获取大小
         */
        private int fetchSize = 500;

        /**
         * 查询超时时间（秒）
         */
        private int queryTimeoutSeconds = 300;

        /**
         * 是否启用并行查询
         */
        private boolean parallelQuery = true;

        /**
         * 并行度
         */
        private int parallelism = 4;

        /**
         * 最大内存使用（MB）
         */
        private int maxMemoryMb = 512;

        /**
         * 是否自动提交
         */
        private boolean autoCommit = false;

        // Getters and Setters

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        public int getBatchSize() {
            return batchSize;
        }

        public void setBatchSize(int batchSize) {
            this.batchSize = batchSize;
        }

        public int getFetchSize() {
            return fetchSize;
        }

        public void setFetchSize(int fetchSize) {
            this.fetchSize = fetchSize;
        }

        public int getQueryTimeoutSeconds() {
            return queryTimeoutSeconds;
        }

        public void setQueryTimeoutSeconds(int queryTimeoutSeconds) {
            this.queryTimeoutSeconds = queryTimeoutSeconds;
        }

        public boolean isParallelQuery() {
            return parallelQuery;
        }

        public void setParallelQuery(boolean parallelQuery) {
            this.parallelQuery = parallelQuery;
        }

        public int getParallelism() {
            return parallelism;
        }

        public void setParallelism(int parallelism) {
            this.parallelism = parallelism;
        }

        public int getMaxMemoryMb() {
            return maxMemoryMb;
        }

        public void setMaxMemoryMb(int maxMemoryMb) {
            this.maxMemoryMb = maxMemoryMb;
        }

        public boolean isAutoCommit() {
            return autoCommit;
        }

        public void setAutoCommit(boolean autoCommit) {
            this.autoCommit = autoCommit;
        }
    }
}
