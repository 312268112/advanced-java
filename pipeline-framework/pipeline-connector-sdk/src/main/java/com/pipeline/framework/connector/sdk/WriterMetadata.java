package com.pipeline.framework.connector.sdk;

/**
 * Writer 元数据。
 *
 * @author Pipeline Framework Team
 * @since 1.0.0
 */
public class WriterMetadata {

    private String writerName;
    private boolean supportsBatchWrite;
    private boolean supportsTransaction;
    private int recommendedBatchSize;

    public WriterMetadata() {
    }

    public String getWriterName() {
        return writerName;
    }

    public void setWriterName(String writerName) {
        this.writerName = writerName;
    }

    public boolean isSupportsBatchWrite() {
        return supportsBatchWrite;
    }

    public void setSupportsBatchWrite(boolean supportsBatchWrite) {
        this.supportsBatchWrite = supportsBatchWrite;
    }

    public boolean isSupportsTransaction() {
        return supportsTransaction;
    }

    public void setSupportsTransaction(boolean supportsTransaction) {
        this.supportsTransaction = supportsTransaction;
    }

    public int getRecommendedBatchSize() {
        return recommendedBatchSize;
    }

    public void setRecommendedBatchSize(int recommendedBatchSize) {
        this.recommendedBatchSize = recommendedBatchSize;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private final WriterMetadata metadata = new WriterMetadata();

        public Builder writerName(String writerName) {
            metadata.writerName = writerName;
            return this;
        }

        public Builder supportsBatchWrite(boolean supportsBatchWrite) {
            metadata.supportsBatchWrite = supportsBatchWrite;
            return this;
        }

        public Builder supportsTransaction(boolean supportsTransaction) {
            metadata.supportsTransaction = supportsTransaction;
            return this;
        }

        public Builder recommendedBatchSize(int recommendedBatchSize) {
            metadata.recommendedBatchSize = recommendedBatchSize;
            return this;
        }

        public WriterMetadata build() {
            return metadata;
        }
    }
}
