package com.example.marketdata.stats.collector;

import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MetricNameTest {

    @Test
    void publicConstantsAreUniqueAndNonBlank() throws IllegalAccessException {
        Set<String> values = new HashSet<>();

        for (Field field : MetricName.class.getDeclaredFields()) {
            if (!Modifier.isPublic(field.getModifiers()) || field.getType() != String.class) {
                continue;
            }

            String value = (String) field.get(null);
            assertFalse(value.isBlank(), "Metric constant must not be blank: " + field.getName());
            assertTrue(values.add(value), "Duplicate metric value found: " + value);
        }
    }
}
