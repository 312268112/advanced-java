package com.pipeline.framework.api.graph;

/**
 * 图验证异常。
 *
 * @author ETL Framework Team
 * @since 1.0.0
 */
public class GraphValidationException extends Exception {

    public GraphValidationException(String message) {
        super(message);
    }

    public GraphValidationException(String message, Throwable cause) {
        super(message, cause);
    }
}
