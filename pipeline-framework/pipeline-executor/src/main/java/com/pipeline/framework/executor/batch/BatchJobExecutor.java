package com.pipeline.framework.executor.batch;

import com.pipeline.framework.api.executor.ExecutionMetrics;
import com.pipeline.framework.api.executor.ExecutionStatus;
import com.pipeline.framework.api.executor.JobExecutor;
import com.pipeline.framework.api.executor.JobResult;
import com.pipeline.framework.api.graph.StreamGraph;
import com.pipeline.framework.api.job.Job;
import com.pipeline.framework.api.job.JobType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Scheduler;
import reactor.core.scheduler.Schedulers;

import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 批量任务执行器。
 * <p>
 * 用于执行批处理任务（BATCH）和SQL批量任务（SQL_BATCH）。
 * 与流式任务不同，批量任务执行完成后会自动结束。
 * </p>
 *
 * @author Pipeline Framework Team
 * @since 1.0.0
 */
public class BatchJobExecutor implements JobExecutor {

    private static final Logger log = LoggerFactory.getLogger(BatchJobExecutor.class);

    private final Scheduler executorScheduler;
    private final Map<String, JobExecutionContext> runningJobs;

    public BatchJobExecutor() {
        this(Schedulers.boundedElastic());
    }

    public BatchJobExecutor(Scheduler executorScheduler) {
        this.executorScheduler = executorScheduler;
        this.runningJobs = new ConcurrentHashMap<>();
    }

    @Override
    public Mono<JobResult> execute(Job job) {
        if (job.getType() == JobType.STREAMING) {
            return Mono.error(new IllegalArgumentException(
                    "BatchJobExecutor does not support STREAMING jobs. Use StreamingJobExecutor instead."));
        }

        String jobId = job.getJobId();
        log.info("Starting batch job execution: jobId={}, type={}", jobId, job.getType());

        return Mono.defer(() -> {
            Instant startTime = Instant.now();
            JobExecutionContext context = new JobExecutionContext(job, startTime);
            runningJobs.put(jobId, context);

            return executeBatchJob(job)
                    .map(metrics -> {
                        Instant endTime = Instant.now();
                        Duration duration = Duration.between(startTime, endTime);
                        
                        log.info("Batch job completed: jobId={}, duration={}ms, recordsProcessed={}",
                                jobId, duration.toMillis(), metrics.getRecordsProcessed());

                        return createJobResult(jobId, ExecutionStatus.COMPLETED, metrics, null);
                    })
                    .onErrorResume(error -> {
                        log.error("Batch job failed: jobId={}", jobId, error);
                        
                        ExecutionMetrics errorMetrics = ExecutionMetrics.builder()
                                .recordsProcessed(context.getRecordsProcessed())
                                .recordsFailed(context.getRecordsFailed())
                                .build();
                        
                        return Mono.just(createJobResult(jobId, ExecutionStatus.FAILED, errorMetrics, error));
                    })
                    .doFinally(signal -> {
                        runningJobs.remove(jobId);
                        log.debug("Batch job removed from running jobs: jobId={}, signal={}", jobId, signal);
                    });
        }).subscribeOn(executorScheduler);
    }

    private Mono<ExecutionMetrics> executeBatchJob(Job job) {
        StreamGraph graph = job.getStreamGraph();
        
        // TODO: 实际的批量任务执行逻辑
        // 这里应该遍历StreamGraph，执行Source -> Operators -> Sink
        // 目前只是一个简单的示例实现
        
        return Mono.fromCallable(() -> {
            long recordsProcessed = 0;
            long recordsFailed = 0;
            
            // 模拟批量处理
            log.debug("Executing batch job: {}", job.getJobId());
            
            // 这里应该调用实际的图执行逻辑
            // 例如: graphExecutor.execute(graph).blockLast();
            
            return ExecutionMetrics.builder()
                    .recordsProcessed(recordsProcessed)
                    .recordsFailed(recordsFailed)
                    .bytesProcessed(0L)
                    .build();
        }).subscribeOn(executorScheduler);
    }

    @Override
    public Mono<Void> cancel(String jobId) {
        log.info("Cancelling batch job: jobId={}", jobId);
        
        JobExecutionContext context = runningJobs.get(jobId);
        if (context != null) {
            context.cancel();
            runningJobs.remove(jobId);
            return Mono.empty();
        }
        
        return Mono.error(new IllegalArgumentException("Job not found: " + jobId));
    }

    @Override
    public Mono<JobResult> getJobResult(String jobId) {
        JobExecutionContext context = runningJobs.get(jobId);
        
        if (context == null) {
            return Mono.error(new IllegalArgumentException("Job not found: " + jobId));
        }
        
        ExecutionMetrics metrics = ExecutionMetrics.builder()
                .recordsProcessed(context.getRecordsProcessed())
                .recordsFailed(context.getRecordsFailed())
                .build();
        
        return Mono.just(createJobResult(jobId, ExecutionStatus.RUNNING, metrics, null));
    }

    private JobResult createJobResult(String jobId, ExecutionStatus status, 
                                     ExecutionMetrics metrics, Throwable error) {
        return new JobResult() {
            @Override
            public String getJobId() {
                return jobId;
            }

            @Override
            public ExecutionStatus getStatus() {
                return status;
            }

            @Override
            public ExecutionMetrics getMetrics() {
                return metrics;
            }

            @Override
            public Throwable getError() {
                return error;
            }

            @Override
            public Map<String, Object> getDetails() {
                Map<String, Object> details = new HashMap<>();
                details.put("jobId", jobId);
                details.put("status", status);
                details.put("metrics", metrics);
                if (error != null) {
                    details.put("errorMessage", error.getMessage());
                }
                return details;
            }
        };
    }

    /**
     * 任务执行上下文
     */
    private static class JobExecutionContext {
        private final Job job;
        private final Instant startTime;
        private volatile long recordsProcessed;
        private volatile long recordsFailed;
        private volatile boolean cancelled;

        public JobExecutionContext(Job job, Instant startTime) {
            this.job = job;
            this.startTime = startTime;
            this.recordsProcessed = 0;
            this.recordsFailed = 0;
            this.cancelled = false;
        }

        public void cancel() {
            this.cancelled = true;
        }

        public boolean isCancelled() {
            return cancelled;
        }

        public long getRecordsProcessed() {
            return recordsProcessed;
        }

        public long getRecordsFailed() {
            return recordsFailed;
        }

        public void incrementRecordsProcessed() {
            this.recordsProcessed++;
        }

        public void incrementRecordsFailed() {
            this.recordsFailed++;
        }
    }
}
