package io.av360.maverick.graph.model.errors;

import org.springframework.http.HttpStatus;

public abstract class InvalidRequest extends Throwable {
    public abstract HttpStatus getStatusCode();

    public String getReasonPhrase() {
        return this.getStatusCode().getReasonPhrase();
    }
}
