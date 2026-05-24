package com.vyms.entity;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import java.util.Set;

/**
 * Maps legacy role strings to the current Role enum values.
 */
@Converter(autoApply = false)
public class RoleConverter implements AttributeConverter<Role, String> {

    private static final Set<String> LEGACY_ROLES = Set.of("SALES", "INVENTORY", "MECHANIC");

    @Override
    public String convertToDatabaseColumn(Role attribute) {
        return attribute == null ? null : attribute.name();
    }

    @Override
    public Role convertToEntityAttribute(String dbData) {
        if (dbData == null || dbData.trim().isEmpty()) {
            return null;
        }
        String normalized = dbData.trim().toUpperCase();
        if (LEGACY_ROLES.contains(normalized)) {
            return null;
        }
        try {
            return Role.valueOf(normalized);
        } catch (IllegalArgumentException ex) {
            return null;
        }
    }
}
