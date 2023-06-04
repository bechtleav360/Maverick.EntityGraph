package org.av360.maverick.graph.model.errors;

public class InsufficientPrivilegeException extends SecurityException {
    public InsufficientPrivilegeException(String msg) {
        super(msg);
    }
}
