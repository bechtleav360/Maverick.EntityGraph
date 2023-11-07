package org.av360.maverick.graph.model.enums;

public enum UriSchemes {

    HTTP("http"),
    FILE("file"),
    S3("s3");

    private final String label;

    UriSchemes(String label) {
        this.label = label;
    }

    @Override
    public String toString() {
        return this.label;
    }
}
