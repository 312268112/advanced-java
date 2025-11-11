package com.pipeline.framework.core.connector;

import com.pipeline.framework.api.connector.ConnectorWriter;
import com.pipeline.framework.api.sink.DataSink;
import com.pipeline.framework.api.sink.SinkConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

/**
 * 将ConnectorWriter适配为DataSink。
 * <p>
 * 在需要消费响应式流时，将简单的Writer转换为Reactor的消费者。
 * </p>
 *
 * @param <T> 数据类型
 * @author Pipeline Framework Team
 * @since 1.0.0
 */
public class WriterSinkAdapter<T> implements DataSink<T> {

    private static final Logger log = LoggerFactory.getLogger(WriterSinkAdapter.class);

    private final ConnectorWriter<T> writer;
    private final int batchSize;
    private final SinkConfig config;

    public WriterSinkAdapter(ConnectorWriter<T> writer, int batchSize, SinkConfig config) {
        this.writer = writer;
        this.batchSize = batchSize;
        this.config = config;
    }

    @Override
    public Mono<Void> sink(Flux<T> dataStream) {
        return Mono.<Void>create(monoSink -> {
            try {
                writer.open();
                
                if (writer.supportsTransaction()) {
                    writer.beginTransaction();
                    log.info("Writer transaction started");
                }
                
                log.info("Writer opened: batchSize={}", batchSize);

                long[] totalCount = {0};

                dataStream
                        .buffer(batchSize)
                        .doOnNext(batch -> {
                            try {
                                writer.writeBatch(batch);
                                totalCount[0] += batch.size();

                                if (totalCount[0] % 10000 == 0) {
                                    log.debug("Written {} records", totalCount[0]);
                                }
                            } catch (Exception e) {
                                throw new RuntimeException("Error writing batch", e);
                            }
                        })
                        .doOnComplete(() -> {
                            try {
                                writer.flush();
                                
                                if (writer.supportsTransaction()) {
                                    writer.commit();
                                    log.info("Writer transaction committed");
                                }
                                
                                log.info("Writer completed: {} total records, writeCount={}", 
                                    totalCount[0], writer.getWriteCount());
                                monoSink.success();
                            } catch (Exception e) {
                                monoSink.error(e);
                            }
                        })
                        .doOnError(error -> {
                            try {
                                if (writer.supportsTransaction()) {
                                    writer.rollback();
                                    log.warn("Writer transaction rolled back");
                                }
                            } catch (Exception e) {
                                log.error("Error rolling back transaction", e);
                            }
                            monoSink.error(error);
                        })
                        .doFinally(signal -> {
                            try {
                                writer.close();
                            } catch (Exception e) {
                                log.warn("Error closing writer", e);
                            }
                        })
                        .subscribeOn(Schedulers.boundedElastic())
                        .blockLast();

            } catch (Exception e) {
                log.error("Writer error", e);
                
                try {
                    if (writer.supportsTransaction()) {
                        writer.rollback();
                    }
                } catch (Exception ex) {
                    log.error("Error rolling back transaction", ex);
                }
                
                monoSink.error(e);
            }
        }).subscribeOn(Schedulers.boundedElastic());
    }

    @Override
    public void start() {
        // 由sink方法处理
    }

    @Override
    public void stop() {
        // 由sink方法处理
    }

    @Override
    public SinkConfig getConfig() {
        return config;
    }
}
