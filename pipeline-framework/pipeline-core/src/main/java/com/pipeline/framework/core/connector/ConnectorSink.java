package com.pipeline.framework.core.connector;

import com.pipeline.framework.api.sink.DataSink;
import com.pipeline.framework.api.sink.SinkConfig;
import com.pipeline.framework.connector.sdk.Lifecycle;
import com.pipeline.framework.connector.sdk.Writable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

/**
 * 将Connector转换为Sink。
 * <p>
 * 在需要消费响应式流时，将简单的Connector转换为Reactor的消费者。
 * </p>
 *
 * @param <T> 数据类型
 * @author Pipeline Framework Team
 * @since 1.0.0
 */
public class ConnectorSink<T> implements DataSink<T> {

    private static final Logger log = LoggerFactory.getLogger(ConnectorSink.class);

    private final Writable<T> writable;
    private final Lifecycle lifecycle;
    private final int batchSize;
    private final SinkConfig config;

    public ConnectorSink(Writable<T> writable, int batchSize, SinkConfig config) {
        this.writable = writable;
        this.lifecycle = writable instanceof Lifecycle ? (Lifecycle) writable : null;
        this.batchSize = batchSize;
        this.config = config;
    }

    @Override
    public Mono<Void> sink(Flux<T> dataStream) {
        return Mono.<Void>create(monoSink -> {
            try {
                // 打开连接
                if (lifecycle != null) {
                    lifecycle.open();
                }
                log.info("Connector sink opened");

                long[] totalCount = {0};

                // 批量消费数据流
                dataStream
                        .buffer(batchSize)
                        .doOnNext(batch -> {
                            try {
                                writable.write(batch);
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
                                writable.flush();
                                log.info("Connector sink completed: {} records written", totalCount[0]);
                                monoSink.success();
                            } catch (Exception e) {
                                monoSink.error(e);
                            }
                        })
                        .doOnError(monoSink::error)
                        .doFinally(signal -> {
                            try {
                                if (lifecycle != null) {
                                    lifecycle.close();
                                }
                            } catch (Exception e) {
                                log.warn("Error closing connector", e);
                            }
                        })
                        .subscribeOn(Schedulers.boundedElastic())
                        .blockLast();

            } catch (Exception e) {
                log.error("Connector sink error", e);
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
