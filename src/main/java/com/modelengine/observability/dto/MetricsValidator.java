package com.modelengine.observability.dto;

import com.modelengine.observability.config.MetricsDefinitionLoader;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Validates that each element in the metrics list is defined in the
 * current {@link MetricsDefinitionLoader} configuration.
 * Null or empty lists pass validation (empty = "query all metrics").
 * <p>
 * This validator is Spring-managed so that it can dynamically check
 * against the hot-reloaded metric definitions instead of a hardcoded enum.
 */
@Component
public class MetricsValidator implements ConstraintValidator<ValidMetrics, List<String>> {

    private final MetricsDefinitionLoader metricsDefinitionLoader;

    public MetricsValidator(MetricsDefinitionLoader metricsDefinitionLoader) {
        this.metricsDefinitionLoader = metricsDefinitionLoader;
    }

    @Override
    public boolean isValid(List<String> metrics, ConstraintValidatorContext context) {
        // null or empty means query all metrics → valid
        if (metrics == null || metrics.isEmpty()) {
            return true;
        }

        for (String m : metrics) {
            if (m == null || !metricsDefinitionLoader.hasDefinition(m)) {
                return false;
            }
        }
        return true;
    }
}
