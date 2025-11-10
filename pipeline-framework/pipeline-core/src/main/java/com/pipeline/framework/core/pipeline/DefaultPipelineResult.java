package com.pipeline.framework.core.pipeline;

import java.time.Duration;
import java.time.Instant;

/**
 * Pipeline执行结果默认实现。
 *
 * @author Pipeline Framework Team
 * @since 1.0.0
 */
public class DefaultPipelineResult implements PipelineResult {
    
    private final boolean success;
    private final Instant startTime;
    private final Instant endTime;
    private final Duration duration;
    private final long recordsProcessed;
    private final String errorMessage;
    private final Throwable exception;

    public DefaultPipelineResult(boolean success,
                                Instant startTime,
                                Instant endTime,
                                Duration duration,
                                long recordsProcessed,
                                String errorMessage,
                                Throwable exception) {
        this.success = success;
        this.startTime = startTime;
        this.endTime = endTime;
        this.duration = duration;
        this.recordsProcessed = recordsProcessed;
        this.errorMessage = errorMessage;
        this.exception = exception;
    }

    @Override
    public boolean isSuccess() {
        return success;
    }

    @Override
    public Instant getStartTime() {
        return startTime;
    }

    @Override
    public Instant getEndTime() {
        return endTime;
    }

    @Override
    public Duration getDuration() {
        return duration;
    }

    @Override
    public long getRecordsRead() {
        return recordsProcessed;
    }

    @Override
    public long getRecordsProcessed() {
        return recordsProcessed;
    }

    @Override
    public long getRecordsWritten() {
        return recordsProcessed;
    }

    @Override
    public String getErrorMessage() {
        return errorMessage;
    }

    @Override
    public Throwable getException() {
        return exception;
    }
}
