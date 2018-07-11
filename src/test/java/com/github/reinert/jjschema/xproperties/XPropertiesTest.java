/*
 * Copyright (c) 2014, Danilo Reinert (daniloreinert@growbit.com)
 *
 * This software is dual-licensed under:
 *
 * - the Lesser General Public License (LGPL) version 3.0 or, at your option, any
 *   later version;
 * - the Apache Software License (ASL) version 2.0.
 *
 * The text of both licenses is available under the src/resources/ directory of
 * this project (under the names LGPL-3.0.txt and ASL-2.0.txt respectively).
 *
 * Direct link to the sources:
 *
 * - LGPL 3.0: https://www.gnu.org/licenses/lgpl-3.0.txt
 * - ASL 2.0: http://www.apache.org/licenses/LICENSE-2.0.txt
 */

package com.github.reinert.jjschema.xproperties;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.github.reinert.jjschema.Attributes;
import com.github.reinert.jjschema.exception.UnavailableVersion;
import com.github.reinert.jjschema.v1.JsonSchemaFactory;
import com.github.reinert.jjschema.v1.JsonSchemaV4Factory;

import junit.framework.TestCase;

import java.io.IOException;
import java.io.InputStream;

/**
 * @author reinert
 */
public class XPropertiesTest extends TestCase {

    static ObjectMapper MAPPER = new ObjectMapper();
    JsonSchemaFactory schemaFactory = new JsonSchemaV4Factory();

    public XPropertiesTest(String testName) {
        super(testName);
    }

    /**
     * Test the scheme generate following a scheme source, avaliable at
     * http://json-schema.org/examples.html the output should match the example.
     *
     * @throws java.io.IOException
     * @throws com.fasterxml.jackson.core.JsonProcessingException
     *
     */
    public void testGenerateSchema() throws UnavailableVersion, JsonProcessingException, IOException {

        final InputStream in = XPropertiesTest.class.getResourceAsStream("/xproperties_example.json");
        if (in == null)
            throw new IOException("resource not found");
        JsonNode fromResource = MAPPER.readTree(in);
        JsonNode fromJavaType = schemaFactory.createSchema(XPropertiesExample.class);
        System.out.println(MAPPER.writerWithDefaultPrettyPrinter().writeValueAsString(fromJavaType));

        assertEquals(fromResource, fromJavaType);
    }

    public static class DoubleFactory {
        public static Double valueOf(String value) {
            return Double.valueOf(value);
        }
    }

    public static class RenameFactory implements XPropertyOperation {
        @Override
        public JsonNode applyXProperty(JsonNode schema, String value) {
            final String[] tokens = value.split("->");
            if (tokens.length != 2) {
                throw new IllegalArgumentException(value);
            }
            final String oldName = tokens[0].trim();
            final String newName = tokens[1].trim();
            final JsonNode node = ((ObjectNode) schema).remove(oldName);
            ((ObjectNode) schema).set(newName, node);
            return schema;
        }
    }

    @Attributes(title = "Example Schema", xProperties = {
            "fieldsets.0.fields.0 = :firstName",
            "fieldsets.0.fields.1 = :lastName",
            "fieldsets.1.fields.0 = :age",
            "properties = com.github.reinert.jjschema.xproperties.XPropertiesTest$RenameFactory:example -> another_name"
    })
    static class XPropertiesExample {
        @Attributes(title = "First Name", required = true, xProperties = {
                "widget.id = :string",
                "widget.aBooleanProp = true"
        })
        private String firstName;
        @Attributes(title = "Last Name", required = true, xProperties = {
                "widget.id = :string",
                "widget.anIntegerProp = 42",
        })
        private String lastName;
        @Attributes(title = "Age in years", minimum = 0, xProperties = {
                "widget.id = :number",
                "widget.aDoubleProp = com.github.reinert.jjschema.xproperties.XPropertiesTest$DoubleFactory:3.141"
        })
        private int age;
        @Attributes(title = "Example", xProperties = {
                "widget.id = :TO_BE_REMOVED",
                "widget.id = null"
        })
        private String example;
        @Attributes(title = "Enum String", xProperties = {
                "oneOf.0.enum.0 = :foo",
                "oneOf.0.description = :Foo",
                "oneOf.1.enum.0 = :bar",
                "oneOf.1.description = :Bar"
        })
        private String enumString;

        public String getFirstName() {
            return firstName;
        }

        public void setFirstName(String firstName) {
            this.firstName = firstName;
        }

        public String getLastName() {
            return lastName;
        }

        public void setLastName(String lastName) {
            this.lastName = lastName;
        }

        public int getAge() {
            return age;
        }

        public void setAge(int age) {
            this.age = age;
        }

        public void setExample(String example) {
            this.example = example;
        }

        public String getExample() {
            return this.example;
        }

        public String getEnumString() {
            return this.enumString;
        }

        public void setEnumString(String enumString) {
            this.enumString = enumString;
        }
    }
}
