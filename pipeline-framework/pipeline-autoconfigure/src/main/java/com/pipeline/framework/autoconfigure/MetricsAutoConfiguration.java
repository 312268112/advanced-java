package com.pipeline.framework.autoconfigure;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.binder.jvm.JvmGcMetrics;
import io.micrometer.core.instrument.binder.jvm.JvmMemoryMetrics;
import io.micrometer.core.instrument.binder.jvm.JvmThreadMetrics;
import io.micrometer.core.instrument.binder.system.ProcessorMetrics;
import io.micrometer.core.instrument.binder.system.UptimeMetrics;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 指标自动配置类。
 *
 * @author Pipeline Framework Team
 * @since 1.0.0
 */
@AutoConfiguration
@EnableConfigurationProperties(PipelineFrameworkProperties.class)
@ConditionalOnProperty(prefix = "pipeline.framework.metrics", name = "enabled", havingValue = "true", matchIfMissing = true)
@ConditionalOnClass(MeterRegistry.class)
public class MetricsAutoConfiguration {

    private static final Logger log = LoggerFactory.getLogger(MetricsAutoConfiguration.class);

    public MetricsAutoConfiguration(PipelineFrameworkProperties properties) {
        PipelineFrameworkProperties.MetricsProperties metrics = properties.getMetrics();
        log.info("Metrics Auto Configuration initialized: enabled={}, reportIntervalSeconds={}, prefix={}",
                metrics.isEnabled(), metrics.getReportIntervalSeconds(), metrics.getPrefix());
    }

    /**
     * JVM指标配置
     */
    @Configuration
    @ConditionalOnProperty(prefix = "pipeline.framework.metrics", name = "jvm-metrics", havingValue = "true", matchIfMissing = true)
    @ConditionalOnBean(MeterRegistry.class)
    static class JvmMetricsConfiguration {

        @Bean
        public JvmMemoryMetrics jvmMemoryMetrics() {
            return new JvmMemoryMetrics();
        }

        @Bean
        public JvmGcMetrics jvmGcMetrics() {
            return new JvmGcMetrics();
        }

        @Bean
        public JvmThreadMetrics jvmThreadMetrics() {
            return new JvmThreadMetrics();
        }
    }

    /**
     * 系统指标配置
     */
    @Configuration
    @ConditionalOnProperty(prefix = "pipeline.framework.metrics", name = "system-metrics", havingValue = "true", matchIfMissing = true)
    @ConditionalOnBean(MeterRegistry.class)
    static class SystemMetricsConfiguration {

        @Bean
        public ProcessorMetrics processorMetrics() {
            return new ProcessorMetrics();
        }

        @Bean
        public UptimeMetrics uptimeMetrics() {
            return new UptimeMetrics();
        }
    }
}
