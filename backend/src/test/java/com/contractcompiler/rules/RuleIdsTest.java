package com.contractcompiler.rules;

import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.util.HashSet;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Ensures all rule IDs are unique and follow the UPPER_SNAKE_CASE convention.
 */
class RuleIdsTest {

    @Test
    void allIdsAreUnique() throws Exception {
        Set<String> ids = new HashSet<>();
        for (Field field : RuleIds.class.getDeclaredFields()) {
            if (field.getType() == String.class && java.lang.reflect.Modifier.isStatic(field.getModifiers())) {
                String value = (String) field.get(null);
                assertThat(ids.add(value))
                        .as("Duplicate rule ID: %s", value)
                        .isTrue();
            }
        }
        assertThat(ids).hasSizeGreaterThan(10); // sanity check
    }

    @Test
    void allIdsMatchUpperSnakeCase() throws Exception {
        for (Field field : RuleIds.class.getDeclaredFields()) {
            if (field.getType() == String.class && java.lang.reflect.Modifier.isStatic(field.getModifiers())) {
                String value = (String) field.get(null);
                assertThat(value).matches("[A-Z][A-Z0-9_]+");
            }
        }
    }

    @Test
    void fieldNamesMatchValues() throws Exception {
        for (Field field : RuleIds.class.getDeclaredFields()) {
            if (field.getType() == String.class && java.lang.reflect.Modifier.isStatic(field.getModifiers())) {
                String fieldName = field.getName();
                String value = (String) field.get(null);
                assertThat(value).as("Field %s value should match its name", fieldName).isEqualTo(fieldName);
            }
        }
    }
}
