package com.pipeline.framework.connector.sdk;

/**
 * 可定位接口，支持断点续传。
 * <p>
 * Connector 实现此接口以支持从特定位置开始读取，
 * 用于实现容错和断点续传功能。
 * </p>
 *
 * @author Pipeline Framework Team
 * @since 1.0.0
 */
public interface Seekable {

    /**
     * 定位到指定位置。
     * <p>
     * 位置的含义由具体 Connector 定义，例如：
     * - 文件：字节偏移量
     * - Kafka：分区+偏移量
     * - 数据库：主键值或行号
     * </p>
     *
     * @param position 位置信息
     * @throws Exception 如果定位失败
     */
    void seek(Position position) throws Exception;

    /**
     * 获取当前位置。
     * <p>
     * 返回的位置可以用于保存检查点，在恢复时传给 seek() 方法。
     * </p>
     *
     * @return 当前位置
     */
    Position getCurrentPosition();

    /**
     * 检查是否支持定位。
     *
     * @return true 如果支持定位，false 否则
     */
    default boolean supportsSeek() {
        return true;
    }
}
