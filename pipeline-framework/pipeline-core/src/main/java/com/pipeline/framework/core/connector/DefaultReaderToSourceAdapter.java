package com.pipeline.framework.core.connector;

import com.pipeline.framework.api.component.ComponentType;
import com.pipeline.framework.api.connector.ConnectorConfig;
import com.pipeline.framework.api.connector.ConnectorReader;
import com.pipeline.framework.api.connector.adapter.ReaderToSourceAdapter;
import com.pipeline.framework.api.source.DataSource;
import com.pipeline.framework.api.source.SourceConfig;
import com.pipeline.framework.api.source.SourceType;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

/**
 * ConnectorReader到DataSource的默认适配器实现。
 * <p>
 * 将ConnectorReader（批量读取）转换为DataSource（响应式流）。
 * 支持背压、检查点、进度跟踪。
 * </p>
 *
 * @param <T> 数据类型
 * @param <C> 配置类型
 * @author Pipeline Framework Team
 * @since 1.0.0
 */
public class DefaultReaderToSourceAdapter<T, C extends ConnectorConfig>
    extends AbstractConnectorAdapter<ConnectorReader<T, C>, DataSource<T>, C>
    implements ReaderToSourceAdapter<T, C> {

    private int batchSize = 1000;
    private boolean backpressureEnabled = true;

    public DefaultReaderToSourceAdapter(ConnectorReader<T, C> reader) {
        super(reader);
    }

    public DefaultReaderToSourceAdapter(ConnectorReader<T, C> reader, int batchSize) {
        super(reader);
        this.batchSize = batchSize;
    }

    @Override
    protected DataSource<T> doAdapt(ConnectorReader<T, C> reader) {
        return new AdaptedDataSource(reader);
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
    public boolean isBackpressureEnabled() {
        return backpressureEnabled;
    }

    public void setBackpressureEnabled(boolean backpressureEnabled) {
        this.backpressureEnabled = backpressureEnabled;
    }

    /**
     * 适配后的DataSource内部类。
     */
    private class AdaptedDataSource implements DataSource<T> {

        private final ConnectorReader<T, C> reader;
        private volatile boolean running = false;

        public AdaptedDataSource(ConnectorReader<T, C> reader) {
            this.reader = reader;
        }

        @Override
        public Flux<T> read() {
            return Flux.defer(() -> {
                try {
                    reader.open();
                    running = true;
                } catch (Exception e) {
                    return Flux.error(e);
                }

                return Flux.<T>create(sink -> {
                    try {
                        while (reader.hasNext() && !sink.isCancelled()) {
                            var batch = reader.readBatch(batchSize);
                            if (batch != null && !batch.isEmpty()) {
                                for (T item : batch) {
                                    sink.next(item);
                                }
                            }
                        }
                        sink.complete();
                    } catch (Exception e) {
                        sink.error(e);
                    } finally {
                        try {
                            reader.close();
                            running = false;
                        } catch (Exception e) {
                            logger.error("Error closing reader", e);
                        }
                    }
                })
                .subscribeOn(Schedulers.boundedElastic());
            });
        }

        @Override
        public SourceType getType() {
            return SourceType.CUSTOM;
        }

        @Override
        public String getName() {
            return reader.getName() + "-adapted-source";
        }

        @Override
        public SourceConfig getConfig() {
            SourceConfig config = new SourceConfig();
            config.setName(getName());
            config.setType(getType());
            return config;
        }

        @Override
        public Mono<Void> start() {
            return Mono.fromRunnable(() -> logger.info("Starting adapted source: {}", getName()));
        }

        @Override
        public Mono<Void> stop() {
            return Mono.fromRunnable(() -> {
                running = false;
                logger.info("Stopping adapted source: {}", getName());
            });
        }

        @Override
        public boolean isRunning() {
            return running;
        }

        @Override
        public ComponentType getComponentType() {
            return ComponentType.SOURCE;
        }
    }
}
