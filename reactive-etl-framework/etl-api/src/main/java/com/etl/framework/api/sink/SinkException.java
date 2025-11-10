package com.etl.framework.api.sink;

/**
 * Sink异常。
 *
 * @author ETL Framework Team
 * @since 1.0.0
 */
public class SinkException extends Exception {

    public SinkException(String message) {
        super(message);
    }

    public SinkException(String message, Throwable cause) {
        super(message, cause);
    }

    public SinkException(Throwable cause) {
        super(cause);
    }
}
