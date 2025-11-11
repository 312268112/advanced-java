package com.pipeline.framework.core.connector;

import com.pipeline.framework.api.component.ComponentType;
import com.pipeline.framework.api.connector.ConnectorConfig;
import com.pipeline.framework.api.connector.ConnectorWriter;
import com.pipeline.framework.api.connector.adapter.WriterToSinkAdapter;
import com.pipeline.framework.api.sink.DataSink;
import com.pipeline.framework.api.sink.SinkConfig;
import com.pipeline.framework.api.sink.SinkType;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

/**
 * ConnectorWriter到DataSink的默认适配器实现。
 * <p>
 * 将ConnectorWriter（批量写入）转换为DataSink（响应式流）。
 * 支持批量写入、事务、自动刷新。
 * </p>
 *
 * @param <T> 数据类型
 * @param <C> 配置类型
 * @author Pipeline Framework Team
 * @since 1.0.0
 */
public class DefaultWriterToSinkAdapter<T, C extends ConnectorConfig>
    extends AbstractConnectorAdapter<ConnectorWriter<T, C>, DataSink<T>, C>
    implements WriterToSinkAdapter<T, C> {

    private int batchSize = 1000;
    private boolean autoFlushEnabled = true;
    private long flushInterval = 1000L;

    public DefaultWriterToSinkAdapter(ConnectorWriter<T, C> writer) {
        super(writer);
    }

    public DefaultWriterToSinkAdapter(ConnectorWriter<T, C> writer, int batchSize) {
        super(writer);
        this.batchSize = batchSize;
    }

    @Override
    protected DataSink<T> doAdapt(ConnectorWriter<T, C> writer) {
        return new AdaptedDataSink(writer);
    }

    @Override
    public int getBatchSize() {
        return batchSize;
    }

    @Override
    public void setBatchSize(int batchSize) {
        this.batchSize = batchSize;
    }

    @Override
    public boolean isAutoFlushEnabled() {
        return autoFlushEnabled;
    }

    public void setAutoFlushEnabled(boolean autoFlushEnabled) {
        this.autoFlushEnabled = autoFlushEnabled;
    }

    @Override
    public long getFlushInterval() {
        return flushInterval;
    }

    public void setFlushInterval(long flushInterval) {
        this.flushInterval = flushInterval;
    }

    /**
     * 适配后的DataSink内部类。
     */
    private class AdaptedDataSink implements DataSink<T> {

        private final ConnectorWriter<T, C> writer;
        private volatile boolean running = false;

        public AdaptedDataSink(ConnectorWriter<T, C> writer) {
            this.writer = writer;
        }

        @Override
        public Mono<Void> write(Flux<T> data) {
            return Mono.defer(() -> {
                try {
                    writer.open();
                    running = true;

                    if (writer.supportsTransaction()) {
                        writer.beginTransaction();
                    }
                } catch (Exception e) {
                    return Mono.error(e);
                }

                return data
                    .buffer(batchSize)
                    .flatMap(batch -> Mono.fromRunnable(() -> {
                        try {
                            writer.writeBatch(batch);
                        } catch (Exception e) {
                            throw new RuntimeException("Failed to write batch", e);
                        }
                    }))
                    .then(Mono.fromRunnable(() -> {
                        try {
                            if (autoFlushEnabled) {
                                writer.flush();
                            }
                            if (writer.supportsTransaction()) {
                                writer.commit();
                            }
                        } catch (Exception e) {
                            if (writer.supportsTransaction()) {
                                try {
                                    writer.rollback();
                                } catch (Exception rollbackEx) {
                                    logger.error("Rollback failed", rollbackEx);
                                }
                            }
                            throw new RuntimeException("Failed to flush/commit", e);
                        }
                    }))
                    .doFinally(signalType -> {
                        try {
                            writer.close();
                            running = false;
                        } catch (Exception e) {
                            logger.error("Error closing writer", e);
                        }
                    });
            });
        }

        @Override
        public Mono<Void> flush() {
            return Mono.fromRunnable(() -> {
                try {
                    writer.flush();
                } catch (Exception e) {
                    throw new RuntimeException("Flush failed", e);
                }
            });
        }

        @Override
        public SinkType getType() {
            return SinkType.CUSTOM;
        }

        @Override
        public String getName() {
            return writer.getName() + "-adapted-sink";
        }

        @Override
        public SinkConfig getConfig() {
            SinkConfig config = new SinkConfig();
            config.setName(getName());
            config.setType(getType());
            return config;
        }

        @Override
        public Mono<Void> start() {
            return Mono.fromRunnable(() -> logger.info("Starting adapted sink: {}", getName()));
        }

        @Override
        public Mono<Void> stop() {
            return Mono.fromRunnable(() -> {
                running = false;
                logger.info("Stopping adapted sink: {}", getName());
            });
        }

        @Override
        public boolean isRunning() {
            return running;
        }

        @Override
        public ComponentType getComponentType() {
            return ComponentType.SINK;
        }
    }
}
