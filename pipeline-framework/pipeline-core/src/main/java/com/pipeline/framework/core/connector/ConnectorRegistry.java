package com.pipeline.framework.core.connector;

import com.pipeline.framework.connector.sdk.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Connector 注册中心。
 * <p>
 * 管理所有 Connector 的注册、查找和创建。
 * 支持插件化的 Connector 扩展。
 * </p>
 *
 * @author Pipeline Framework Team
 * @since 1.0.0
 */
public class ConnectorRegistry {

    private static final Logger log = LoggerFactory.getLogger(ConnectorRegistry.class);

    private final Map<String, ConnectorDescriptor> connectors = new ConcurrentHashMap<>();
    private final Map<String, ReaderFactory<?>> readerFactories = new ConcurrentHashMap<>();
    private final Map<String, WriterFactory<?>> writerFactories = new ConcurrentHashMap<>();

    /**
     * 注册 Connector。
     *
     * @param descriptor Connector 描述符
     */
    public void registerConnector(ConnectorDescriptor descriptor) {
        String name = descriptor.getName();
        if (connectors.containsKey(name)) {
            log.warn("Connector already registered, will be replaced: {}", name);
        }

        connectors.put(name, descriptor);
        log.info("Connector registered: name={}, type={}, version={}",
                name, descriptor.getType(), descriptor.getVersion());
    }

    /**
     * 注册 Reader 工厂。
     *
     * @param name    Connector 名称
     * @param factory Reader 工厂
     */
    public void registerReaderFactory(String name, ReaderFactory<?> factory) {
        readerFactories.put(name, factory);
        log.info("Reader factory registered: {}", name);
    }

    /**
     * 注册 Writer 工厂。
     *
     * @param name    Connector 名称
     * @param factory Writer 工厂
     */
    public void registerWriterFactory(String name, WriterFactory<?> factory) {
        writerFactories.put(name, factory);
        log.info("Writer factory registered: {}", name);
    }

    /**
     * 获取 Connector 描述符。
     *
     * @param name Connector 名称
     * @return Connector 描述符
     */
    public ConnectorDescriptor getConnector(String name) {
        return connectors.get(name);
    }

    /**
     * 创建 Reader。
     *
     * @param name   Connector 名称
     * @param config 配置参数
     * @param <T>    数据类型
     * @return Reader 实例
     * @throws Exception 如果创建失败
     */
    @SuppressWarnings("unchecked")
    public <T> Reader<T> createReader(String name, Object config) throws Exception {
        ReaderFactory<T> factory = (ReaderFactory<T>) readerFactories.get(name);
        if (factory == null) {
            throw new IllegalArgumentException("Reader factory not found: " + name);
        }

        Reader<T> reader = factory.create(config);
        log.info("Reader created: connector={}, class={}", name, reader.getClass().getSimpleName());
        return reader;
    }

    /**
     * 创建 BatchReader。
     *
     * @param name   Connector 名称
     * @param config 配置参数
     * @param <T>    数据类型
     * @return BatchReader 实例
     * @throws Exception 如果创建失败
     */
    @SuppressWarnings("unchecked")
    public <T> BatchReader<T> createBatchReader(String name, Object config) throws Exception {
        ReaderFactory<T> factory = (ReaderFactory<T>) readerFactories.get(name);
        if (factory == null) {
            throw new IllegalArgumentException("Reader factory not found: " + name);
        }

        BatchReader<T> reader = factory.createBatchReader(config);
        log.info("BatchReader created: connector={}, class={}", name, reader.getClass().getSimpleName());
        return reader;
    }

    /**
     * 创建 Writer。
     *
     * @param name   Connector 名称
     * @param config 配置参数
     * @param <T>    数据类型
     * @return Writer 实例
     * @throws Exception 如果创建失败
     */
    @SuppressWarnings("unchecked")
    public <T> Writer<T> createWriter(String name, Object config) throws Exception {
        WriterFactory<T> factory = (WriterFactory<T>) writerFactories.get(name);
        if (factory == null) {
            throw new IllegalArgumentException("Writer factory not found: " + name);
        }

        Writer<T> writer = factory.create(config);
        log.info("Writer created: connector={}, class={}", name, writer.getClass().getSimpleName());
        return writer;
    }

    /**
     * 获取所有已注册的 Connector 名称。
     *
     * @return Connector 名称集合
     */
    public java.util.Set<String> getConnectorNames() {
        return connectors.keySet();
    }

    /**
     * 检查 Connector 是否已注册。
     *
     * @param name Connector 名称
     * @return true 如果已注册，false 否则
     */
    public boolean isConnectorRegistered(String name) {
        return connectors.containsKey(name);
    }

    /**
     * Reader 工厂接口。
     *
     * @param <T> 数据类型
     */
    public interface ReaderFactory<T> {
        /**
         * 创建 Reader。
         *
         * @param config 配置参数
         * @return Reader 实例
         * @throws Exception 如果创建失败
         */
        Reader<T> create(Object config) throws Exception;

        /**
         * 创建 BatchReader（可选）。
         *
         * @param config 配置参数
         * @return BatchReader 实例
         * @throws Exception 如果创建失败
         */
        default BatchReader<T> createBatchReader(Object config) throws Exception {
            throw new UnsupportedOperationException("Batch reader not supported");
        }
    }

    /**
     * Writer 工厂接口。
     *
     * @param <T> 数据类型
     */
    public interface WriterFactory<T> {
        /**
         * 创建 Writer。
         *
         * @param config 配置参数
         * @return Writer 实例
         * @throws Exception 如果创建失败
         */
        Writer<T> create(Object config) throws Exception;
    }
}
