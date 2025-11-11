package com.pipeline.framework.core.connector;

import com.pipeline.framework.api.source.DataSource;
import com.pipeline.framework.api.source.SourceConfig;
import com.pipeline.framework.connector.sdk.Lifecycle;
import com.pipeline.framework.connector.sdk.Position;
import com.pipeline.framework.connector.sdk.Readable;
import com.pipeline.framework.connector.sdk.Seekable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Flux;
import reactor.core.scheduler.Schedulers;

import java.util.List;

/**
 * 将Connector转换为Source。
 * <p>
 * 在需要创建响应式流时，将简单的Connector转换为Reactor的Flux。
 * </p>
 *
 * @param <T> 数据类型
 * @author Pipeline Framework Team
 * @since 1.0.0
 */
public class ConnectorSource<T> implements DataSource<T> {

    private static final Logger log = LoggerFactory.getLogger(ConnectorSource.class);

    private final Readable<T> readable;
    private final Lifecycle lifecycle;
    private final Seekable seekable;
    private final int batchSize;
    private final SourceConfig config;

    public ConnectorSource(Readable<T> readable, int batchSize, SourceConfig config) {
        this.readable = readable;
        this.lifecycle = readable instanceof Lifecycle ? (Lifecycle) readable : null;
        this.seekable = readable instanceof Seekable ? (Seekable) readable : null;
        this.batchSize = batchSize;
        this.config = config;
    }

    @Override
    public Flux<T> getDataStream() {
        return Flux.<T>create(sink -> {
            try {
                // 打开连接
                if (lifecycle != null) {
                    lifecycle.open();
                }
                log.info("Connector source opened");

                long totalCount = 0;

                // 读取数据
                while (readable.hasMore() && !sink.isCancelled()) {
                    List<T> batch = readable.read(batchSize);
                    
                    if (batch == null || batch.isEmpty()) {
                        break;
                    }

                    // 发送数据
                    batch.forEach(sink::next);
                    totalCount += batch.size();

                    if (totalCount % 10000 == 0) {
                        log.debug("Processed {} records", totalCount);
                    }
                }

                log.info("Connector source completed: {} records", totalCount);
                sink.complete();

            } catch (Exception e) {
                log.error("Connector source error", e);
                sink.error(e);
            } finally {
                try {
                    if (lifecycle != null) {
                        lifecycle.close();
                    }
                } catch (Exception e) {
                    log.warn("Error closing connector", e);
                }
            }
        }).subscribeOn(Schedulers.boundedElastic());
    }

    /**
     * 支持断点续传的数据流。
     *
     * @param position 起始位置
     * @return 数据流
     */
    public Flux<T> getDataStream(Position position) {
        if (seekable == null) {
            log.warn("Connector does not support seek, ignoring position");
            return getDataStream();
        }

        return Flux.<T>create(sink -> {
            try {
                if (lifecycle != null) {
                    lifecycle.open();
                }

                // 定位到指定位置
                seekable.seek(position);
                log.info("Seeked to position: {}", position);

                long totalCount = 0;

                while (readable.hasMore() && !sink.isCancelled()) {
                    List<T> batch = readable.read(batchSize);
                    
                    if (batch == null || batch.isEmpty()) {
                        break;
                    }

                    batch.forEach(sink::next);
                    totalCount += batch.size();

                    if (totalCount % 10000 == 0) {
                        log.debug("Processed {} records", totalCount);
                    }
                }

                log.info("Connector source completed: {} records", totalCount);
                sink.complete();

            } catch (Exception e) {
                log.error("Connector source error", e);
                sink.error(e);
            } finally {
                try {
                    if (lifecycle != null) {
                        lifecycle.close();
                    }
                } catch (Exception e) {
                    log.warn("Error closing connector", e);
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
