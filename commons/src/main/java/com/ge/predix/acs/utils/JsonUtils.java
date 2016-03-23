/*******************************************************************************
 * Copyright 2016 General Electric Company. 
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); 
 * you may not use this file except in compliance with the License. 
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software 
 * distributed under the License is distributed on an "AS IS" BASIS, 
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. 
 * See the License for the specific language governing permissions and 
 * limitations under the License.
 *******************************************************************************/
package com.ge.predix.acs.utils;

import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.io.StringReader;
import java.net.URL;
import java.util.Collection;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.type.CollectionType;
import com.github.fge.jackson.JsonNodeReader;

/**
 * JSON utility methods convert from/to json files, objects and string.
 *
 * @author 212360328
 */
@SuppressWarnings("nls")
public class JsonUtils {
    private static final Logger LOGGER = LoggerFactory.getLogger(JsonUtils.class);

    private static final ObjectMapper OBJECT_MAPPER;
    private static final JsonNodeReader READER = new JsonNodeReader();

    static {
        OBJECT_MAPPER = new ObjectMapper();
        OBJECT_MAPPER.setSerializationInclusion(Include.NON_NULL);
    }

    /**
     * @param fileName
     *            File Name
     * @param type
     *            The type of the class being unmarshalled
     * @return An object instance of type type from the JSON payload stored in file fileName
     */
    public <T> T deserializeFromFile(final String fileName, final Class<T> type) {
        try (InputStream fis = inputStreamFromFileName(fileName)) {
            return OBJECT_MAPPER.readValue(fis, type);
        } catch (Exception e) {
            LOGGER.error(String.format("Unexpected Exception %s", e.getMessage()));
            return null;
        }
    }

    /**
     * @param object
     *            object instance to be converted into String
     * @return a JSON String representation of the object
     */
    public <T> String serialize(final T object) {
        try {
            return OBJECT_MAPPER.writeValueAsString(object);
        } catch (Exception e) {
            LOGGER.error(String.format("Unexpected Exception %s", e.getMessage()));
            return null;
        }
    }

    /**
     * @param jsonString
     *            the JSON String being converted into a object instance
     * @param type
     *            The target type of the object to be created
     * @return The type of the target class
     */
    public <T> T deserialize(final String jsonString, final Class<T> type) {
        try {
            return OBJECT_MAPPER.readValue(jsonString, type);
        } catch (IOException e) {
            LOGGER.error(String.format("Unexpected Exception %s", e.getMessage()));
            return null;
        }
    }

    /**
     * @param jsonString
     *            the JSON String being converted into a object instance
     * @param collectionType
     *            The type of the collection, ex: Set, List, etc.
     * @param elementType
     *            The type of the elements on this collection
     * @param type
     *            The target type of the object to be created
     * @return The type of the target class
     */
    public <C extends Collection<T>, T> C deserialize(final String jsonString, final Class<C> collectionType,
            final Class<T> elementType) {
        try {
            final CollectionType javaType = OBJECT_MAPPER.getTypeFactory().constructCollectionType(collectionType,
                    elementType);

            return OBJECT_MAPPER.readValue(jsonString, javaType);
        } catch (IOException e) {
            LOGGER.error(String.format("Unexpected Exception %s", e.getMessage()));
            return null;
        }
    }

    /**
     * @param jsonFileName
     *            The file name of the file containing the JSON string to be read
     * @return A JsonNode of the corresponding JSON String
     */
    public JsonNode readJsonNodeFromFile(final String jsonFileName) {

        try (InputStream fis = inputStreamFromFileName(jsonFileName)) {
            return READER.fromInputStream(fis);
        } catch (Exception e) {
            LOGGER.error(String.format("Unexpected Exception %s", e.getMessage()));
            return null;
        }
    }

    /**
     * @param object
     *            The source object to be converted into a JsonNode
     * @return The corresponding JsonNode of the given Object
     */
    public <T> JsonNode readJsonNodeFromObject(final T object) {

        String serializedObject = serialize(object);
        try (Reader objectReader = new StringReader(serializedObject)) {
            return READER.fromReader(objectReader);
        } catch (Exception e) {
            LOGGER.error(String.format("Unexpected Exception %s", e.getMessage()));
            return null;
        }
    }

    private InputStream inputStreamFromFileName(final String fileName) throws IOException {
        URL resource = this.getClass().getClassLoader().getResource(fileName);
        if (resource == null) {
            LOGGER.error("Could not find file [" + fileName + "]");
            return null;
        }
        return resource.openStream();
    }

}
