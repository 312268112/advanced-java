package com.pipeline.framework.connectors.kafka;

import com.pipeline.framework.api.source.DataSource;
import com.pipeline.framework.api.source.SourceConfig;
import com.pipeline.framework.api.source.SourceType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Flux;
import reactor.kafka.receiver.KafkaReceiver;
import reactor.kafka.receiver.ReceiverOptions;
import reactor.kafka.receiver.ReceiverRecord;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Kafka数据源实现。
 * <p>
 * 使用 reactor-kafka 实现响应式的Kafka消费。
 * </p>
 *
 * @param <T> 数据类型
 * @author Pipeline Framework Team
 * @since 1.0.0
 */
public class KafkaSource<T> implements DataSource<T> {
    
    private static final Logger log = LoggerFactory.getLogger(KafkaSource.class);
    
    private final String name;
    private final SourceConfig config;
    private final AtomicBoolean initialized = new AtomicBoolean(false);
    
    private KafkaReceiver<String, T> kafkaReceiver;

    public KafkaSource(String name, SourceConfig config) {
        this.name = name;
        this.config = config;
    }

    /**
     * 读取Kafka数据流。
     * <p>
     * 返回一个无限的Flux流，持续消费Kafka消息。
     * </p>
     */
    @Override
    public Flux<T> read() {
        if (!initialized.get()) {
            initialize();
        }
        
        return kafkaReceiver.receive()
            .doOnSubscribe(s -> log.info("Started consuming from Kafka: topic={}", getTopic()))
            .doOnNext(record -> log.debug("Received message: partition={}, offset={}", 
                record.partition(), record.offset()))
            .map(ReceiverRecord::value)
            .doOnError(e -> log.error("Error consuming from Kafka", e))
            .doOnComplete(() -> log.info("Kafka consumer completed"));
    }

    /**
     * 初始化Kafka消费者。
     */
    private void initialize() {
        if (initialized.compareAndSet(false, true)) {
            log.info("Initializing Kafka source: {}", name);
            
            Map<String, Object> props = new HashMap<>();
            props.put("bootstrap.servers", config.getProperty("bootstrap.servers", "localhost:9092"));
            props.put("group.id", config.getProperty("group.id", "pipeline-framework"));
            props.put("key.deserializer", "org.apache.kafka.common.serialization.StringDeserializer");
            props.put("value.deserializer", config.getProperty("value.deserializer"));
            props.put("auto.offset.reset", config.getProperty("auto.offset.reset", "latest"));
            
            ReceiverOptions<String, T> receiverOptions = ReceiverOptions.<String, T>create(props)
                .subscription(Collections.singleton(getTopic()));
            
            this.kafkaReceiver = KafkaReceiver.create(receiverOptions);
            
            log.info("Kafka source initialized: topic={}", getTopic());
        }
    }

    private String getTopic() {
        return config.getProperty("topic");
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public SourceType getType() {
        return SourceType.KAFKA;
    }

    @Override
    public SourceConfig getConfig() {
        return config;
    }
}
