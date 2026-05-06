package com.modelengine.observability.dto;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Jakarta validation constraint that checks each element of the metrics list
 * is a valid metric defined in the {@code observability.metrics.definitions} configuration.
 * <p>
 * Empty list passes validation (meaning "query all metrics").
 * Validation is delegated to {@link MetricsValidator} which dynamically
 * checks against hot-reloaded metric definitions.
 */
@Documented
@Constraint(validatedBy = MetricsValidator.class)
@Target({ElementType.FIELD, ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
public @interface ValidMetrics {

    String message() default "Invalid metric name. Must match a configured metric definition.";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
