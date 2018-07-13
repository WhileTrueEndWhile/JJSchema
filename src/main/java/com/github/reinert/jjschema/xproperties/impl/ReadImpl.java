package com.github.reinert.jjschema.xproperties.impl;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.Properties;
import java.util.stream.Collectors;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.github.reinert.jjschema.xproperties.XProperties;
import com.github.reinert.jjschema.xproperties.XProperty;
import com.github.reinert.jjschema.xproperties.XPropertyOperation;

/**
 * Read Implementation
 * 
 * @author WhileTrueEndWhile
 */
public class ReadImpl {

    //
    // X Properties Files Errors
    //
    private static final String ERROR_RESOURCE_NOT_FOUND = "Could not find resource";
    private static final String ERROR_RESOURCE_IO_ERROR = "Could not load resource";

    //
    // X Properties Errors
    //
    private static final String ERROR_NOT_EXACTLY_ONE_PROPERTY = "Exactly one property must be defined";
    private static final String ERROR_PROPERTY_VALUE_HAS_NO_SEPARATOR = "Property value has no separator (:) and is not null, a boolean or an integer";
    private static final String ERROR_CLASS_NOT_FOUND = "Custom property value factory/operation class not found:";
    private static final String ERROR_METHOD_NOT_FOUND = "Custom property value factory/operation method not found or has error:";

    /**
     * Separator for path keys (Key0.Key1.Key2).
     */
    private static final String SEPARATOR_PROPERTY_PATH = "\\.";

    /**
     * Separator for type and value (type:value)
     */
    private static final String SEPARATOR_PROPERTY_VALUE = ":";

    /**
     * Regular expression for null values.
     */
    private static final String REGEX_NULL = "^null$";

    /**
     * Regular expression for booleans.
     */
    private static final String REGEX_BOOLEAN = "^(false|true)$";

    /**
     * Regular expression for integers.
     */
    private static final String REGEX_INTEGER = "^[0-9]+$";

    /**
     * JSON Schema properties.
     */
    private static final String JSON_SCHEMA_PROPERTIES = "properties";

    /**
     * JSON Schema required.
     */
    private static final String JSON_SCHEMA_REQUIRED = "required";

    /**
     * JSON Schema required.
     */
    private static final String JSON_SCHEMA_DEFAULT = "default";

    /**
     * Reads X Properties from an annotation instance.
     * 
     * @param attributes
     *            An annotation instance.
     * 
     * @return A list of X Properties.
     */
    public static List<XProperty> readXProperties(XProperties attributes) {
        final List<XProperty> listOfProperties = new ArrayList<>();

        //
        // Read X Properties Files
        //

        listOfProperties.addAll(ReadImpl.readXPropertiesFiles(attributes));

        //
        // Read X Properties
        //

        if (attributes == null || attributes.value() == null) {
            return listOfProperties;
        }
        final String[] xProperties = attributes.value();
        for (int i = 0; i < xProperties.length; ++i) {
            final String xProperty = xProperties[i];
            final XProperty property;
            try {
                property = readProperty(xProperty);
            } catch (IllegalArgumentException e) {
                throw new IllegalArgumentException("xProperties[" + i + "]", e);
            }
            listOfProperties.add(property);
        }
        return listOfProperties;
    }

    /**
     * 
     * Reads X Properties from a JsonProperty annotation instance.
     * 
     * 
     * @param type
     *            The class containing the field read from.
     * 
     * @param schema
     *            Schema of the class containing the field to read from.
     * 
     * @param fieldName
     *            Name of the field to read X Properties from.
     * 
     * @param jsonProperty
     *            A JsonProperty annotation instance to read X Properties from.
     * 
     * @return A list of X Properties
     * 
     */
    public static List<XProperty> readXProperties(Class<?> type, ObjectNode schema, String fieldName,
            JsonProperty jsonProperty) {
        final List<XProperty> listOfProperties = new ArrayList<>();
        final boolean required = jsonProperty.required();
        final String defaultValue = jsonProperty.defaultValue();
        final String value = jsonProperty.value();

        //
        // @JsonProperty(required=true)
        //

        if (required) {
            final ArrayNode requiredArray = (ArrayNode) schema.get(JSON_SCHEMA_REQUIRED);
            if (requiredArray == null) {
                listOfProperties.add(new DefaultXProperty(
                        Arrays.asList(JSON_SCHEMA_REQUIRED, 0), fieldName));
            } else {
                final Iterator<JsonNode> it = requiredArray.elements();
                int index = 0;
                boolean isSet = false;
                isSetLoop: while (it.hasNext()) {
                    final String text = it.next().asText();
                    if (fieldName.equals(text)) {
                        isSet = true;
                        break isSetLoop;
                    }
                    ++index;
                }
                if (!isSet) {
                    listOfProperties.add(new DefaultXProperty(
                            Arrays.asList(JSON_SCHEMA_REQUIRED, index), fieldName));
                }
            }
        }

        //
        // @JsonProperty(defaultValue=???)
        //

        if (!defaultValue.isEmpty()) {
            listOfProperties.add(new DefaultXProperty(
                    Arrays.asList(JSON_SCHEMA_PROPERTIES, fieldName, JSON_SCHEMA_DEFAULT), defaultValue));
        }

        //
        // @JsonProperty(???)
        //

        if (!value.isEmpty()) {
            listOfProperties.add(
                    new DefaultXProperty(Arrays.asList(JSON_SCHEMA_PROPERTIES, value),
                            schema.get(JSON_SCHEMA_PROPERTIES).get(fieldName)));
            listOfProperties.add(
                    new DefaultXProperty(Arrays.asList(JSON_SCHEMA_PROPERTIES, fieldName), null));
        }
        return listOfProperties;
    }

    // -----------------------------------------------------------------------

    /**
     * Reads X Properties from an annotation instance.
     * 
     * @param attributes
     *            An annotation instance.
     * 
     * @return A list of X Properties.
     */
    private static List<XProperty> readXPropertiesFiles(XProperties attributes) {
        final List<XProperty> listOfProperties = new ArrayList<>();
        if (attributes == null || attributes.files() == null) {
            return listOfProperties;
        }
        final String[] xPropertiesFiles = attributes.files();
        for (int i = 0; i < xPropertiesFiles.length; ++i) {
            final String xPropertiesFileName = xPropertiesFiles[i];
            final InputStream xPropertiesFile = System.class.getResourceAsStream(xPropertiesFileName);
            if (xPropertiesFile == null) {
                throw new IllegalArgumentException(ERROR_RESOURCE_NOT_FOUND + ": " + xPropertiesFileName);
            }
            final Properties properties = new Properties();
            try {
                properties.load(xPropertiesFile);
            } catch (IOException e) {
                throw new IllegalArgumentException(ERROR_RESOURCE_IO_ERROR + ": " + xPropertiesFileName, e);
            }
            final Enumeration<?> propertyNames = properties.propertyNames();
            while (propertyNames.hasMoreElements()) {
                final String propertyPath = (String) propertyNames.nextElement();
                final String propertyValue = properties.getProperty(propertyPath);
                final XProperty xProperty = readProperty(propertyPath, propertyValue);
                listOfProperties.add(xProperty);
            }
        }
        return listOfProperties;
    }

    /**
     * Reads a property.
     * 
     * 
     * @param property
     *            Property as string
     * 
     * @return Property as object
     */
    private static XProperty readProperty(String property) {
        final InputStream xPropertiesStream = new ByteArrayInputStream(property.getBytes());
        final Properties properties = new Properties();
        try {
            properties.load(xPropertiesStream);
        } catch (IOException e) {
            throw new IllegalArgumentException(e);
        }
        final Enumeration<?> propertyNames = properties.propertyNames();
        final List<XProperty> listOfProperties = new ArrayList<>();
        while (propertyNames.hasMoreElements()) {
            final String propertyPath = (String) propertyNames.nextElement();
            final String propertyValue = properties.getProperty(propertyPath);
            final XProperty xProperty = readProperty(propertyPath, propertyValue);
            listOfProperties.add(xProperty);
        }
        if (listOfProperties.size() != 1) {
            throw new IllegalArgumentException(ERROR_NOT_EXACTLY_ONE_PROPERTY);
        }
        return listOfProperties.get(0);
    }

    /**
     * Reads a property.
     * 
     * 
     * @param propertyPath
     *            Property path as string.
     * 
     * @param propertyValue
     *            Property value as string.
     * 
     * @return Property as object.
     */
    private static XProperty readProperty(String propertyPath, String propertyValue) {
        propertyPath = Objects.requireNonNull(propertyPath);

        //
        // k0.k1.k2 = value
        //

        final List<Object> propertyPathAsList = readPropertyPath(propertyPath);
        final Object propertyValueAsObject;
        propertyValueAsObject = readPropertyValue(propertyValue);
        return new DefaultXProperty(propertyPathAsList, propertyValueAsObject);
    }

    /**
     * Reads a property path.
     * 
     * 
     * @param propertyPath
     *            Property path as string.
     * 
     * @return Property path as list of objects.
     */
    private static List<Object> readPropertyPath(String propertyPath) {

        //
        // k0.k1.k2
        //

        return Arrays.asList(propertyPath.split(SEPARATOR_PROPERTY_PATH)).stream()
                .map((String k) -> readPropertyPathKey(k)).collect(Collectors.toList());
    }

    /**
     * Reads a property path key.
     * 
     * 
     * @param propertyPathKey
     *            Property path key as string.
     * 
     * @return Property path key as object (integer or string).
     */
    private static Object readPropertyPathKey(String propertyPathKey) {
        propertyPathKey = propertyPathKey.trim();

        //
        // Integer
        //

        if (propertyPathKey.matches(REGEX_INTEGER)) {
            return Integer.valueOf(propertyPathKey);
        }

        //
        // String
        //

        return propertyPathKey;
    }

    /**
     * Reads a property value.
     * 
     * 
     * @param propertyValue
     *            Property value as string.
     * 
     * @return An object supported by ArrayNode.insert/ObjectNode.put.
     */
    private static Object readPropertyValue(String propertyValue) {
        propertyValue = propertyValue.trim();

        //
        // value without ':'
        //

        if (propertyValue.matches(REGEX_NULL)) {
            return null;
        }
        if (propertyValue.matches(REGEX_BOOLEAN)) {
            return Boolean.valueOf(propertyValue);
        }
        if (propertyValue.matches(REGEX_INTEGER)) {
            return Integer.valueOf(propertyValue);
        }

        //
        // value with ':'
        //

        final int index = propertyValue.indexOf(SEPARATOR_PROPERTY_VALUE);
        if (index < 0) {
            throw new IllegalArgumentException(ERROR_PROPERTY_VALUE_HAS_NO_SEPARATOR);
        }
        final String type = propertyValue.substring(0, index).trim();
        final String value = propertyValue.substring(index + 1).trim();

        //
        // Short Names for java.lang.*
        //

        switch (type) {
        case "Boolean":
        case "b":
            return callStaticFactoryMethod("java.lang.Boolean", value);
        case "Double":
        case "d":
            return callStaticFactoryMethod("java.lang.Double", value);
        case "Float":
        case "f":
            return callStaticFactoryMethod("java.lang.Float", value);
        case "Integer":
        case "i":
            return callStaticFactoryMethod("java.lang.Integer", value);
        case "Long":
        case "l":
            return callStaticFactoryMethod("java.lang.Long", value);
        case "String":
        case "s":
            return value;
        }
        return callStaticFactoryMethod(type, value);
    }

    /**
     * Calls applyXProperty or valueOf.
     * 
     * @param type
     *            Name of the class to invoke applyXProperty or valueOf.
     *
     * @param value
     *            Value to pass to applyXProperty or valueOf.
     *
     * @return An object supported by ArrayNode.insert/ObjectNode.put.
     */
    private static Object callStaticFactoryMethod(String type, String value) {
        final Class<?> typeClass;
        try {
            typeClass = Class.forName(type);
        } catch (ReflectiveOperationException e) {
            throw new IllegalArgumentException(ERROR_CLASS_NOT_FOUND + " " + type, e);
        }

        //
        // Try to call the custom static factory method:
        //
        // Object applyXProperty(JsonNode schema, String value)
        //

        {
            try {
                final Method applyXProperty = typeClass.getMethod(
                        XPropertyOperation.APPlY_X_PROPERTY_NAME,
                        XPropertyOperation.APPLY_X_PROPERTY_ARGS);
                final Object typeInstance = typeClass.getConstructor().newInstance();
                final Runnable runnable = new Runnable() {
                    public JsonNode input = null;
                    @SuppressWarnings("unused")
                    public Object output = null;

                    @Override
                    public void run() {
                        try {
                            final Object valueObject = applyXProperty.invoke(typeInstance, input, value);
                            this.output = valueObject;
                        } catch (ReflectiveOperationException e) {
                            throw new IllegalArgumentException(
                                    ERROR_METHOD_NOT_FOUND + " " + XPropertyOperation.APPlY_X_PROPERTY_NAME, e);
                        }
                    }
                };
                return runnable;
            } catch (ReflectiveOperationException e) {
                // Use default valueOf below...
            }
        }

        //
        // Call the default static factory method:
        //
        // Object valueOf (String value)
        //

        {
            try {

                final Method valueOf = typeClass.getMethod(
                        XPropertyOperation.STATIC_FALL_BACK_NAME,
                        XPropertyOperation.STATIC_FALL_BACK_ARGS);
                final Object valueObject = valueOf.invoke(null, value);
                return valueObject;
            } catch (ReflectiveOperationException e) {
                throw new IllegalArgumentException(ERROR_METHOD_NOT_FOUND, e);
            }
        }
    }
}
