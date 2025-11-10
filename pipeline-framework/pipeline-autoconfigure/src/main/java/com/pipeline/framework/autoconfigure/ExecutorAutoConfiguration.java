package com.pipeline.framework.autoconfigure;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;
import java.util.concurrent.ThreadPoolExecutor;

/**
 * 执行器自动配置类。
 *
 * @author Pipeline Framework Team
 * @since 1.0.0
 */
@AutoConfiguration
@EnableConfigurationProperties(PipelineFrameworkProperties.class)
@ConditionalOnProperty(prefix = "pipeline.framework", name = "enabled", havingValue = "true", matchIfMissing = true)
public class ExecutorAutoConfiguration {

    private static final Logger log = LoggerFactory.getLogger(ExecutorAutoConfiguration.class);

    @Bean(name = "pipelineExecutor", destroyMethod = "shutdown")
    @ConditionalOnMissingBean(name = "pipelineExecutor")
    public Executor pipelineExecutor(PipelineFrameworkProperties properties) {
        PipelineFrameworkProperties.ExecutorProperties config = properties.getExecutor();
        
        log.info("Initializing Pipeline Executor: corePoolSize={}, maxPoolSize={}, queueCapacity={}",
                config.getCorePoolSize(), config.getMaxPoolSize(), config.getQueueCapacity());

        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(config.getCorePoolSize());
        executor.setMaxPoolSize(config.getMaxPoolSize());
        executor.setQueueCapacity(config.getQueueCapacity());
        executor.setKeepAliveSeconds(config.getKeepAliveSeconds());
        executor.setThreadNamePrefix(config.getThreadNamePrefix());
        executor.setAllowCoreThreadTimeOut(config.isAllowCoreThreadTimeout());
        
        // 拒绝策略：调用者运行策略
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
        
        // 等待所有任务完成后再关闭线程池
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(60);
        
        executor.initialize();
        
        log.info("Pipeline Executor initialized successfully");
        return executor;
    }
}
