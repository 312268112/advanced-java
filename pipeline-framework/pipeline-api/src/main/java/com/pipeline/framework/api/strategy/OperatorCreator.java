package com.pipeline.framework.api.strategy;

import com.pipeline.framework.api.operator.Operator;
import com.pipeline.framework.api.operator.OperatorConfig;

/**
 * Operator 创建策略接口。
 *
 * @author Pipeline Framework Team
 * @since 1.0.0
 */
public interface OperatorCreator extends ComponentCreator<Operator<?, ?>, OperatorConfig> {
}
