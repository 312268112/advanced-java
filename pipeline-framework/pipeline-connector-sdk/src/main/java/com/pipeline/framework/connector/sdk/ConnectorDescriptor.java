package com.pipeline.framework.connector.sdk;

/**
 * Connector 描述符。
 * <p>
 * 用于描述一个 Connector 的基本信息和能力。
 * </p>
 *
 * @author Pipeline Framework Team
 * @since 1.0.0
 */
public class ConnectorDescriptor {

    private String name;
    private String version;
    private String description;
    private ConnectorType type;
    private Class<?> readerClass;
    private Class<?> writerClass;
    private boolean supportsSeek;
    private boolean supportsBatchRead;
    private boolean supportsBatchWrite;

    public ConnectorDescriptor() {
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public ConnectorType getType() {
        return type;
    }

    public void setType(ConnectorType type) {
        this.type = type;
    }

    public Class<?> getReaderClass() {
        return readerClass;
    }

    public void setReaderClass(Class<?> readerClass) {
        this.readerClass = readerClass;
    }

    public Class<?> getWriterClass() {
        return writerClass;
    }

    public void setWriterClass(Class<?> writerClass) {
        this.writerClass = writerClass;
    }

    public boolean isSupportsSeek() {
        return supportsSeek;
    }

    public void setSupportsSeek(boolean supportsSeek) {
        this.supportsSeek = supportsSeek;
    }

    public boolean isSupportsBatchRead() {
        return supportsBatchRead;
    }

    public void setSupportsBatchRead(boolean supportsBatchRead) {
        this.supportsBatchRead = supportsBatchRead;
    }

    public boolean isSupportsBatchWrite() {
        return supportsBatchWrite;
    }

    public void setSupportsBatchWrite(boolean supportsBatchWrite) {
        this.supportsBatchWrite = supportsBatchWrite;
    }

    public static Builder builder() {
        return new Builder();
    }

    /**
     * Connector 类型
     */
    public enum ConnectorType {
        DATABASE,   // 数据库
        FILE,       // 文件
        MESSAGE_QUEUE, // 消息队列
        CACHE,      // 缓存
        API,        // API
        CUSTOM      // 自定义
    }

    public static class Builder {
        private final ConnectorDescriptor descriptor = new ConnectorDescriptor();

        public Builder name(String name) {
            descriptor.name = name;
            return this;
        }

        public Builder version(String version) {
            descriptor.version = version;
            return this;
        }

        public Builder description(String description) {
            descriptor.description = description;
            return this;
        }

        public Builder type(ConnectorType type) {
            descriptor.type = type;
            return this;
        }

        public Builder readerClass(Class<?> readerClass) {
            descriptor.readerClass = readerClass;
            return this;
        }

        public Builder writerClass(Class<?> writerClass) {
            descriptor.writerClass = writerClass;
            return this;
        }

        public Builder supportsSeek(boolean supportsSeek) {
            descriptor.supportsSeek = supportsSeek;
            return this;
        }

        public Builder supportsBatchRead(boolean supportsBatchRead) {
            descriptor.supportsBatchRead = supportsBatchRead;
            return this;
        }

        public Builder supportsBatchWrite(boolean supportsBatchWrite) {
            descriptor.supportsBatchWrite = supportsBatchWrite;
            return this;
        }

        public ConnectorDescriptor build() {
            if (descriptor.name == null || descriptor.name.isEmpty()) {
                throw new IllegalArgumentException("Connector name is required");
            }
            return descriptor;
        }
    }
}
