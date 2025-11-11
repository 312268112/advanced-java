package com.pipeline.framework.connector.sdk;

/**
 * Reader 元数据。
 *
 * @author Pipeline Framework Team
 * @since 1.0.0
 */
public class ReaderMetadata {

    private String readerName;
    private boolean supportsBatchRead;
    private boolean supportsSeek;
    private int recommendedBatchSize;

    public ReaderMetadata() {
    }

    public String getReaderName() {
        return readerName;
    }

    public void setReaderName(String readerName) {
        this.readerName = readerName;
    }

    public boolean isSupportsBatchRead() {
        return supportsBatchRead;
    }

    public void setSupportsBatchRead(boolean supportsBatchRead) {
        this.supportsBatchRead = supportsBatchRead;
    }

    public boolean isSupportsSeek() {
        return supportsSeek;
    }

    public void setSupportsSeek(boolean supportsSeek) {
        this.supportsSeek = supportsSeek;
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
        private final ReaderMetadata metadata = new ReaderMetadata();

        public Builder readerName(String readerName) {
            metadata.readerName = readerName;
            return this;
        }

        public Builder supportsBatchRead(boolean supportsBatchRead) {
            metadata.supportsBatchRead = supportsBatchRead;
            return this;
        }

        public Builder supportsSeek(boolean supportsSeek) {
            metadata.supportsSeek = supportsSeek;
            return this;
        }

        public Builder recommendedBatchSize(int recommendedBatchSize) {
            metadata.recommendedBatchSize = recommendedBatchSize;
            return this;
        }

        public ReaderMetadata build() {
            return metadata;
        }
    }
}
