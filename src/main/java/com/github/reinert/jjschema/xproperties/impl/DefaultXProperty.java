package com.github.reinert.jjschema.xproperties.impl;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import com.github.reinert.jjschema.xproperties.XProperty;

/**
 * X Property Implementation
 * 
 * @author WhileTrueEndWhile
 */
public class DefaultXProperty implements XProperty {

    //
    // Errors
    //
    private static final String ERROR_PROPERTY_PATH_EMPTY = "Property path is empty";
    private static final String ERROR_PROPERTY_PATH_FIRST_KEY_NOT_STRING = "First key of the property path is not a string";
    private static final String ERROR_PROPERTY_PATH_FIRST_KEY_EMPTY = "First key of property path is empty";
    private static final String ERROR_PROPERTY_PATH_KEY_TYPE = "At least one key of the property path is not an integer or a string";

    /**
     * Property Path
     */
    private final Object[] propertyPath;

    /**
     * Property Value
     */
    private final Object propertyValue;

    /**
     * Creates an immutable X Property instance.
     * 
     * 
     * @param propertyPath
     *                      Property path as list of objects.
     * 
     * @param propertyValue
     *                      Property value as object (integer or string).
     */
    public DefaultXProperty(List<Object> propertyPath, Object propertyValue) {
        if (propertyPath.size() < 1) {
            throw new IllegalArgumentException(ERROR_PROPERTY_PATH_EMPTY);
        }
        if (!(propertyPath.get(0) instanceof String)) {
            throw new IllegalArgumentException(ERROR_PROPERTY_PATH_FIRST_KEY_NOT_STRING);
        }
        if (((String) propertyPath.get(0)).isEmpty()) {
            throw new IllegalArgumentException(ERROR_PROPERTY_PATH_FIRST_KEY_EMPTY);
        }
        propertyPath.forEach(propertyPathKey -> validatePropertyPathKey(propertyPathKey));
        propertyPath = new ArrayList<>(propertyPath);
        this.propertyPath = propertyPath.toArray();
        this.propertyValue = propertyValue;
    }

    /**
     * Gets the property path.
     * 
     * 
     * @return A List of integers and strings.
     */
    @Override
    public List<Object> getPropertyPath() {
        return Arrays.asList(this.propertyPath);
    }

    /**
     * Gets the property value.
     * 
     * 
     * @return An object supported by ArrayNode.insert/ObjectNode.put.
     */
    @Override
    public Object getPropertyValue() {
        return this.propertyValue;
    }

    /**
     * Validates one key of the property path.
     * 
     * 
     * @param propertyPathKey
     *                        Property path key to validate.
     */
    private static void validatePropertyPathKey(Object propertyPathKey) {
        final boolean isInteger = propertyPathKey instanceof Integer;
        final boolean isString = propertyPathKey instanceof String;
        if ((!isInteger) && (!isString)) {
            throw new IllegalArgumentException(ERROR_PROPERTY_PATH_KEY_TYPE);
        }
    }
}
