package org.av360.maverick.graph.model.errors.requests;

import org.av360.maverick.graph.model.errors.InvalidRequest;
import org.springframework.http.HttpStatus;
import org.springframework.util.StringUtils;

public class SchedulingException extends InvalidRequest {
    private final String reason;

    public SchedulingException(String reason) {
        this.reason = reason;
    }



    @Override
    public String getMessage() {
        StringBuilder sb = new StringBuilder("Scheduling conflict.'");
        if (StringUtils.hasLength(this.reason)) {
            sb.append(this.reason).append(".");
        }
        return sb.toString();
    }

    @Override
    public HttpStatus getStatusCode() {
        return HttpStatus.BAD_REQUEST;
    }
}
