package com.course.entity;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

/**
 * Converts between {@link RoleName} and its DB representation.
 *
 * DB stores values as Postgres enum type (role_name). JDBC binds them as strings.
 */
@Converter(autoApply = false)
public class RoleNameConverter implements AttributeConverter<RoleName, String> {

    @Override
    public String convertToDatabaseColumn(RoleName attribute) {
        return attribute == null ? null : attribute.name();
    }

    @Override
    public RoleName convertToEntityAttribute(String dbData) {
        return dbData == null ? null : RoleName.valueOf(dbData);
    }
}
