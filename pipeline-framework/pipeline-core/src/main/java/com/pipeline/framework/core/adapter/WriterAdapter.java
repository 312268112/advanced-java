package com.pipeline.framework.core.adapter;

import com.pipeline.framework.connector.sdk.Writer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.util.List;

/**
 * Writer 到 Reactor Mono 的适配器。
 * <p>
 * 将简单的 Writer 接口转换为 Reactor 响应式流消费者。
 * </p>
 *
 * @author Pipeline Framework Team
 * @since 1.0.0
 */
public class WriterAdapter {

    private static final Logger log = LoggerFactory.getLogger(WriterAdapter.class);

    /**
     * 将数据流写入 Writer。
     *
     * @param dataStream 数据流
     * @param writer     Writer实例
     * @param <T>        数据类型
     * @return 写入完成的Mono
     */
    public static <T> Mono<Void> write(Flux<T> dataStream, Writer<T> writer) {
        return write(dataStream, writer, 1);
    }

    /**
     * 将数据流批量写入 Writer。
     *
     * @param dataStream 数据流
     * @param writer     Writer实例
     * @param batchSize  批次大小
     * @param <T>        数据类型
     * @return 写入完成的Mono
     */
    public static <T> Mono<Void> write(Flux<T> dataStream, Writer<T> writer, int batchSize) {
        return Mono.<Void>create(sink -> {
            try {
                // 打开writer
                writer.open();
                log.info("Writer opened: {}", writer.getClass().getSimpleName());

                long[] totalCount = {0};  // 使用数组以便在lambda中修改

                // 订阅数据流并写入
                dataStream
                        .buffer(batchSize)
                        .doOnNext(batch -> {
                            try {
                                writer.writeBatch(batch);
                                totalCount[0] += batch.size();

                                // 每10000条记录输出一次日志
                                if (totalCount[0] % 10000 == 0) {
                                    log.debug("Writer processed {} records", totalCount[0]);
                                }
                            } catch (Exception e) {
                                throw new RuntimeException("Error writing batch", e);
                            }
                        })
                        .doOnComplete(() -> {
                            try {
                                writer.flush();
                                log.info("Writer completed: {} records written", totalCount[0]);
                                sink.success();
                            } catch (Exception e) {
                                sink.error(e);
                            }
                        })
                        .doOnError(error -> {
                            log.error("Writer error after {} records", totalCount[0], error);
                            sink.error(error);
                        })
                        .doFinally(signal -> {
                            try {
                                writer.close();
                                log.info("Writer closed");
                            } catch (Exception e) {
                                log.warn("Error closing writer", e);
                            }
                        })
                        .subscribeOn(Schedulers.boundedElastic())
                        .blockLast();  // 阻塞等待写入完成

            } catch (Exception e) {
                log.error("Writer initialization error", e);
                sink.error(e);
            }
        }).subscribeOn(Schedulers.boundedElastic());
    }

    /**
     * 批量写入数据列表。
     *
     * @param records 数据列表
     * @param writer  Writer实例
     * @param <T>     数据类型
     * @return 写入完成的Mono
     */
    public static <T> Mono<Void> writeBatch(List<T> records, Writer<T> writer) {
        return Mono.fromRunnable(() -> {
            try {
                writer.open();
                log.info("Writer opened for batch write: {} records", records.size());

                writer.writeBatch(records);
                writer.flush();

                log.info("Batch write completed: {} records written", records.size());
            } catch (Exception e) {
                log.error("Batch write error", e);
                throw new RuntimeException("Batch write failed", e);
            } finally {
                try {
                    writer.close();
                    log.info("Writer closed");
                } catch (Exception e) {
                    log.warn("Error closing writer", e);
                }
            }
        }).subscribeOn(Schedulers.boundedElastic()).then();
    }
}
