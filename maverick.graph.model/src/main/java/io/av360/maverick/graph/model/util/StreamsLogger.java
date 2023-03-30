package io.av360.maverick.graph.model.util;

import org.reactivestreams.Subscription;
import org.slf4j.Logger;

import java.util.function.Consumer;

public class StreamsLogger {

    public static Consumer<? super Subscription> trace(Logger log, String message, Object... params) {
        return subscription -> {
            if(log.isTraceEnabled())
                log.trace(message, params);
        };
    }

    public static Consumer<? super Subscription> debug(Logger log, String message, Object... params) {
        return subscription -> {
            if(log.isDebugEnabled())
                log.debug(message, params);
        };
    }

    public static Consumer<? super Subscription> error(Logger log, String message, Throwable e) {
        return subscription -> {
            if(log.isErrorEnabled())
                log.error(message, e);
        };
    }


}
