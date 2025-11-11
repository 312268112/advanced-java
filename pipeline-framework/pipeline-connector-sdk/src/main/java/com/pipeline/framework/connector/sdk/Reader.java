package com.pipeline.framework.connector.sdk;

import java.io.Closeable;
import java.util.Iterator;

/**
 * 数据读取器接口。
 * <p>
 * Connector 开发者实现此接口以提供数据读取能力。
 * 不依赖 Reactor，使用简单的迭代器模式。
 * </p>
 *
 * @param <T> 记录类型
 * @author Pipeline Framework Team
 * @since 1.0.0
 */
public interface Reader<T> extends Iterator<T>, Closeable {

    /**
     * 打开读取器。
     * <p>
     * 在开始读取数据之前调用，用于初始化资源（如数据库连接、文件句柄等）。
     * </p>
     *
     * @throws Exception 如果打开失败
     */
    void open() throws Exception;

    /**
     * 检查是否还有更多数据。
     *
     * @return true 如果还有数据，false 否则
     */
    @Override
    boolean hasNext();

    /**
     * 读取下一条记录。
     *
     * @return 下一条记录
     * @throws java.util.NoSuchElementException 如果没有更多数据
     */
    @Override
    T next();

    /**
     * 关闭读取器并释放资源。
     */
    @Override
    void close();

    /**
     * 获取读取器元数据。
     *
     * @return 元数据
     */
    default ReaderMetadata getMetadata() {
        return ReaderMetadata.builder()
                .readerName(this.getClass().getSimpleName())
                .build();
    }
}
