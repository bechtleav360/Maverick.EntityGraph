package org.av360.maverick.graph.model.errors;

public class InvalidConfiguration extends Throwable {


    private final String message;

    public InvalidConfiguration(String message) {
        this.message = message;
    }

    @Override
    public String getMessage() {
        return "Invalid configuration: "+message;
    }


}
