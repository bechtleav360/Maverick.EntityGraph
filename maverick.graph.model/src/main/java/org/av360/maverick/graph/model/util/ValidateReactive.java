package org.av360.maverick.graph.model.util;

import org.apache.commons.lang3.Validate;
import org.springframework.lang.Nullable;
import reactor.core.publisher.Mono;

import java.util.Objects;

public abstract class ValidateReactive {


    public static Mono<Void> isTrue(boolean expression, String message) {
        try {
            Validate.isTrue(expression, message);
            return Mono.empty();
        } catch (Exception e) {
            return Mono.error(e);
        }
    }


    public static Mono<Void> isTrue(boolean expression) {
        try {
            Validate.isTrue(expression);
            return Mono.empty();
        } catch (Exception e) {
            return Mono.error(e);
        }
    }



    public static Mono<Void> isNull(@Nullable Object object, String message) {
        try {
            Validate.isTrue(Objects.isNull(object), message);
            return Mono.empty();
        } catch (Exception e) {
            return Mono.error(e);
        }
    }




    public static <T> Mono<T> notNull(final T object) {
        try {
            return Mono.just(Validate.notNull(object));
        } catch (Exception e) {
            return Mono.error(e);
        }
    }


    public static <T> Mono<T> notNull(final T object, String message) {
        try {
            return Mono.just(Validate.notNull(object, message));
        } catch (Exception e) {
            return Mono.error(e);
        }
    }



    public static <T extends CharSequence> Mono<T> notBlank(final T chars) {
        try {
            return Mono.just(Validate.notBlank(chars));
        } catch (Exception e) {
            return Mono.error(e);
        }
    }

    public static <T extends CharSequence> Mono<T> notBlank(final T chars, final String message, final Object... values) {
        try {
            return Mono.just(Validate.notBlank(chars, message, values));
        } catch (Exception e) {
            return Mono.error(e);
        }
    }
}
