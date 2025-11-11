package com.pipeline.framework.core.adapter;

import com.pipeline.framework.connector.sdk.BatchReader;
import com.pipeline.framework.connector.sdk.Position;
import com.pipeline.framework.connector.sdk.Reader;
import com.pipeline.framework.connector.sdk.Seekable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Flux;
import reactor.core.scheduler.Schedulers;

import java.util.List;

/**
 * Reader 到 Reactor Flux 的适配器。
 * <p>
 * 将简单的 Reader/BatchReader 接口转换为 Reactor 响应式流。
 * </p>
 *
 * @author Pipeline Framework Team
 * @since 1.0.0
 */
public class ReaderAdapter {

    private static final Logger log = LoggerFactory.getLogger(ReaderAdapter.class);

    /**
     * 将 Reader 适配为 Flux。
     *
     * @param reader Reader实例
     * @param <T>    数据类型
     * @return Flux流
     */
    public static <T> Flux<T> toFlux(Reader<T> reader) {
        return toFlux(reader, null);
    }

    /**
     * 将 Reader 适配为 Flux，支持断点续传。
     *
     * @param reader   Reader实例
     * @param position 起始位置（可选）
     * @param <T>      数据类型
     * @return Flux流
     */
    public static <T> Flux<T> toFlux(Reader<T> reader, Position position) {
        return Flux.<T>create(sink -> {
            try {
                // 支持断点续传
                if (position != null && reader instanceof Seekable) {
                    ((Seekable) reader).seek(position);
                    log.info("Reader seeked to position: {}", position);
                }

                // 打开reader
                reader.open();
                log.info("Reader opened: {}", reader.getClass().getSimpleName());

                // 读取数据
                long count = 0;
                while (reader.hasNext() && !sink.isCancelled()) {
                    T record = reader.next();
                    sink.next(record);
                    count++;

                    // 每1000条记录输出一次日志
                    if (count % 1000 == 0) {
                        log.debug("Reader processed {} records", count);
                    }
                }

                log.info("Reader completed: {} records processed", count);
                sink.complete();

            } catch (Exception e) {
                log.error("Reader error", e);
                sink.error(e);
            } finally {
                try {
                    reader.close();
                    log.info("Reader closed");
                } catch (Exception e) {
                    log.warn("Error closing reader", e);
                }
            }
        }).subscribeOn(Schedulers.boundedElastic());
    }

    /**
     * 将 BatchReader 适配为 Flux。
     *
     * @param batchReader BatchReader实例
     * @param batchSize   批次大小
     * @param <T>         数据类型
     * @return Flux流
     */
    public static <T> Flux<T> toFlux(BatchReader<T> batchReader, int batchSize) {
        return toFlux(batchReader, batchSize, null);
    }

    /**
     * 将 BatchReader 适配为 Flux，支持断点续传。
     *
     * @param batchReader BatchReader实例
     * @param batchSize   批次大小
     * @param position    起始位置（可选）
     * @param <T>         数据类型
     * @return Flux流
     */
    public static <T> Flux<T> toFlux(BatchReader<T> batchReader, int batchSize, Position position) {
        return Flux.<List<T>>create(sink -> {
            try {
                // 支持断点续传
                if (position != null && batchReader instanceof Seekable) {
                    ((Seekable) batchReader).seek(position);
                    log.info("BatchReader seeked to position: {}", position);
                }

                // 打开reader
                batchReader.open();
                log.info("BatchReader opened: {}", batchReader.getClass().getSimpleName());

                // 批量读取数据
                long totalCount = 0;
                while (batchReader.hasMore() && !sink.isCancelled()) {
                    List<T> batch = batchReader.readBatch(batchSize);
                    if (batch == null || batch.isEmpty()) {
                        break;
                    }

                    sink.next(batch);
                    totalCount += batch.size();

                    // 每10000条记录输出一次日志
                    if (totalCount % 10000 == 0) {
                        log.debug("BatchReader processed {} records", totalCount);
                    }
                }

                log.info("BatchReader completed: {} records processed", totalCount);
                sink.complete();

            } catch (Exception e) {
                log.error("BatchReader error", e);
                sink.error(e);
            } finally {
                try {
                    batchReader.close();
                    log.info("BatchReader closed");
                } catch (Exception e) {
                    log.warn("Error closing batch reader", e);
                }
            }
        })
        .flatMap(Flux::fromIterable)  // 将批次展开为单条记录
        .subscribeOn(Schedulers.boundedElastic());
    }
}
