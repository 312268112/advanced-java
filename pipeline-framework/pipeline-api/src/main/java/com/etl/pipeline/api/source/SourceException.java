package com.pipeline.framework.api.source;

/**
 * 数据源异常。
 *
 * @author ETL Framework Team
 * @since 1.0.0
 */
public class SourceException extends Exception {

    public SourceException(String message) {
        super(message);
    }

    public SourceException(String message, Throwable cause) {
        super(message, cause);
    }

    public SourceException(Throwable cause) {
        super(cause);
    }
}
