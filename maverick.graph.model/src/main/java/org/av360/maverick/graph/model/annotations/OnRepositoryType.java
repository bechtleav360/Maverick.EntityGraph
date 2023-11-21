package org.av360.maverick.graph.model.annotations;

import org.av360.maverick.graph.model.enums.RepositoryType;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Used for annotating service methods called by the controllers. Will set the requested repository type in the session context.
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface OnRepositoryType {

    public RepositoryType value() default RepositoryType.ENTITIES;


}
