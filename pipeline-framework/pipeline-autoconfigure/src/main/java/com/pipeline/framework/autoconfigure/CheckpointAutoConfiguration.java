package com.pipeline.framework.autoconfigure;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;

/**
 * 检查点自动配置类。
 *
 * @author Pipeline Framework Team
 * @since 1.0.0
 */
@AutoConfiguration
@EnableConfigurationProperties(PipelineFrameworkProperties.class)
@ConditionalOnProperty(prefix = "pipeline.framework.checkpoint", name = "enabled", havingValue = "true", matchIfMissing = true)
public class CheckpointAutoConfiguration {

    private static final Logger log = LoggerFactory.getLogger(CheckpointAutoConfiguration.class);

    public CheckpointAutoConfiguration(PipelineFrameworkProperties properties) {
        PipelineFrameworkProperties.CheckpointProperties checkpoint = properties.getCheckpoint();
        log.info("Checkpoint Auto Configuration initialized: enabled={}, intervalSeconds={}, storagePath={}",
                checkpoint.isEnabled(), checkpoint.getIntervalSeconds(), checkpoint.getStoragePath());
    }

    // 检查点相关的Bean将在后续实现时添加
}
