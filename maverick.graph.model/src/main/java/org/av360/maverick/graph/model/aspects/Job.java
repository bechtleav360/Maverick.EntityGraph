package org.av360.maverick.graph.model.aspects;


import org.springframework.core.annotation.AliasFor;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Repository;

import java.lang.annotation.*;

/**
 * Indicates that an annotated class is a scheduled "Job". It is logically a Spring Boot Component, but is used to
 * differentiate the services (which are called by the exposed controllers and require all public methods to be annotated
 * with requires privilege methods) from jobs.
 *
 * @see Component
 * @see Repository
 * @see RequiresPrivilege
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Component
public @interface Job {
    /**
     * The value may indicate a suggestion for a logical component name,
     * to be turned into a Spring bean in case of an autodetected component.
     * @return the suggested component name, if any (or empty String otherwise)
     */
    @AliasFor(annotation = Component.class)
    String value() default "";
}
