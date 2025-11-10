package com.pipeline.framework.api.strategy;

import com.pipeline.framework.api.source.DataSource;
import com.pipeline.framework.api.source.SourceConfig;

/**
 * Source 创建策略接口。
 *
 * @author Pipeline Framework Team
 * @since 1.0.0
 */
public interface SourceCreator extends ComponentCreator<DataSource<?>, SourceConfig> {
}
