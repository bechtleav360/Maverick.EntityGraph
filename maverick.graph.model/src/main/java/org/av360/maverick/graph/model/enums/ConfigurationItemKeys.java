package org.av360.maverick.graph.model.enums;

public enum ConfigurationItemKeys {
    LOCAL_CONTENT_PATH("content_path"),
    S3_HOST("export_s3_host"),
    S3_BUCKET("export_s3_bucketId"),
    EXPORT_FREQUENCY("export_frequency");

    private final String label;



    ConfigurationItemKeys(String label) {
        this.label = label;
    }

    @Override
    public String toString() {
        return this.label;
    }
}
