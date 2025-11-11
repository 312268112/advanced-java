package com.pipeline.framework.connector.sdk;

/**
 * 可定位能力接口，支持断点续传。
 * <p>
 * Connector实现此接口以支持从特定位置开始读取。
 * </p>
 *
 * @author Pipeline Framework Team
 * @since 1.0.0
 */
public interface Seekable {

    /**
     * 定位到指定位置。
     *
     * @param position 位置
     * @throws Exception 定位失败
     */
    void seek(Position position) throws Exception;

    /**
     * 获取当前位置。
     *
     * @return 当前位置
     */
    Position currentPosition();
}
