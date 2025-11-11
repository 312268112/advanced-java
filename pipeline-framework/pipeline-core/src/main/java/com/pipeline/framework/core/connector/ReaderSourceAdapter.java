package com.pipeline.framework.core.connector;

import com.pipeline.framework.api.connector.ConnectorReader;
import com.pipeline.framework.api.source.DataSource;
import com.pipeline.framework.api.source.SourceConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Flux;
import reactor.core.scheduler.Schedulers;

import java.util.List;

/**
 * 将ConnectorReader适配为DataSource。
 * <p>
 * 在需要创建响应式流时，将简单的Reader转换为Reactor的Flux。
 * </p>
 *
 * @param <T> 数据类型
 * @author Pipeline Framework Team
 * @since 1.0.0
 */
public class ReaderSourceAdapter<T> implements DataSource<T> {

    private static final Logger log = LoggerFactory.getLogger(ReaderSourceAdapter.class);

    private final ConnectorReader<T> reader;
    private final int batchSize;
    private final SourceConfig config;

    public ReaderSourceAdapter(ConnectorReader<T> reader, int batchSize, SourceConfig config) {
        this.reader = reader;
        this.batchSize = batchSize;
        this.config = config;
    }

    @Override
    public Flux<T> getDataStream() {
        return Flux.<T>create(sink -> {
            try {
                reader.open();
                log.info("Reader opened: batchSize={}", batchSize);

                long totalCount = 0;
                Object lastCheckpoint = null;

                while (reader.hasNext() && !sink.isCancelled()) {
                    List<T> batch = reader.readBatch(batchSize);
                    
                    if (batch == null || batch.isEmpty()) {
                        break;
                    }

                    for (T record : batch) {
                        sink.next(record);
                    }
                    
                    totalCount += batch.size();

                    // 定期记录检查点
                    if (reader.supportsCheckpoint() && totalCount % 10000 == 0) {
                        lastCheckpoint = reader.getCheckpoint();
                        log.debug("Checkpoint saved at {} records", totalCount);
                    }

                    // 定期输出进度
                    if (totalCount % 10000 == 0) {
                        double progress = reader.getProgress();
                        if (progress >= 0) {
                            log.debug("Progress: {:.2f}%, {} records", progress * 100, totalCount);
                        } else {
                            log.debug("Processed {} records", totalCount);
                        }
                    }
                }

                log.info("Reader completed: {} total records, readCount={}", 
                    totalCount, reader.getReadCount());
                sink.complete();

            } catch (Exception e) {
                log.error("Reader error", e);
                sink.error(e);
            } finally {
                try {
                    reader.close();
                } catch (Exception e) {
                    log.warn("Error closing reader", e);
                }
            }
        }).subscribeOn(Schedulers.boundedElastic());
    }

    /**
     * 从检查点恢复并获取数据流。
     *
     * @param checkpoint 检查点
     * @return 数据流
     */
    public Flux<T> getDataStream(Object checkpoint) {
        return Flux.<T>create(sink -> {
            try {
                reader.open();
                
                if (checkpoint != null && reader.supportsCheckpoint()) {
                    reader.seekToCheckpoint(checkpoint);
                    log.info("Reader resumed from checkpoint");
                }

                long totalCount = 0;

                while (reader.hasNext() && !sink.isCancelled()) {
                    List<T> batch = reader.readBatch(batchSize);
                    
                    if (batch == null || batch.isEmpty()) {
                        break;
                    }

                    for (T record : batch) {
                        sink.next(record);
                    }
                    
                    totalCount += batch.size();

                    if (totalCount % 10000 == 0) {
                        log.debug("Processed {} records", totalCount);
                    }
                }

                log.info("Reader completed: {} records", totalCount);
                sink.complete();

            } catch (Exception e) {
                log.error("Reader error", e);
                sink.error(e);
            } finally {
                try {
                    reader.close();
                } catch (Exception e) {
                    log.warn("Error closing reader", e);
                }
            }
        }).subscribeOn(Schedulers.boundedElastic());
    }

    @Override
    public void start() {
        // 由getDataStream处理
    }

    @Override
    public void stop() {
        // 由getDataStream处理
    }

    @Override
    public SourceConfig getConfig() {
        return config;
    }
}
