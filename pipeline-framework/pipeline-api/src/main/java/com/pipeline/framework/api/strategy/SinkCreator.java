package com.pipeline.framework.api.strategy;

import com.pipeline.framework.api.sink.DataSink;
import com.pipeline.framework.api.sink.SinkConfig;

/**
 * Sink 创建策略接口。
 *
 * @author Pipeline Framework Team
 * @since 1.0.0
 */
public interface SinkCreator extends ComponentCreator<DataSink<?>, SinkConfig> {
}
