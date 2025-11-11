package com.pipeline.framework.api.connector;

/**
 * Connector异常。
 *
 * @author Pipeline Framework Team
 * @since 1.0.0
 */
public class ConnectorException extends Exception {

    private static final long serialVersionUID = 1L;

    private final ConnectorType connectorType;
    private final String connectorName;

    public ConnectorException(String message) {
        super(message);
        this.connectorType = null;
        this.connectorName = null;
    }

    public ConnectorException(String message, Throwable cause) {
        super(message, cause);
        this.connectorType = null;
        this.connectorName = null;
    }

    public ConnectorException(String message, ConnectorType connectorType, String connectorName) {
        super(message);
        this.connectorType = connectorType;
        this.connectorName = connectorName;
    }

    public ConnectorException(String message, Throwable cause, ConnectorType connectorType, String connectorName) {
        super(message, cause);
        this.connectorType = connectorType;
        this.connectorName = connectorName;
    }

    public ConnectorType getConnectorType() {
        return connectorType;
    }

    public String getConnectorName() {
        return connectorName;
    }
}
