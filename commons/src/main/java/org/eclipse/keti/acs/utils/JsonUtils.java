/*******************************************************************************
 * Copyright 2017 General Electric Company
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * SPDX-License-Identifier: Apache-2.0
 *******************************************************************************/

package org.eclipse.keti.acs.utils;

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
 * @author acs-engineers@ge.com
 */
@SuppressWarnings("nls")
public class JsonUtils {
    private static final Logger LOGGER = LoggerFactory.getLogger(JsonUtils.class);

    private static final ObjectMapper OBJECT_MAPPER;
    private static final JsonNodeReader READER = new JsonNodeReader();
    private static final String UNEXPECTED_EXCEPTION = "Unexpected Exception";

    static {
        OBJECT_MAPPER = new ObjectMapper();
        OBJECT_MAPPER.setSerializationInclusion(Include.NON_NULL);
    }

    public <T> T deserializeFromFile(final String fileName, final Class<T> type) {
        try (InputStream fis = inputStreamFromFileName(fileName)) {
            return OBJECT_MAPPER.readValue(fis, type);
        } catch (Exception e) {
            LOGGER.error(UNEXPECTED_EXCEPTION, e);
            return null;
        }
    }

    public <T> String serialize(final T object) {
        try {
            return OBJECT_MAPPER.writeValueAsString(object);
        } catch (Exception e) {
            LOGGER.error(UNEXPECTED_EXCEPTION, e);
            return null;
        }
    }

    public <T> T deserialize(final String jsonString, final Class<T> type) {
        try {
            return OBJECT_MAPPER.readValue(jsonString, type);
        } catch (IOException e) {
            LOGGER.error(UNEXPECTED_EXCEPTION, e);
            return null;
        }
    }

    public <C extends Collection<T>, T> C deserialize(final String jsonString, final Class<C> collectionType,
            final Class<T> elementType) {
        try {
            final CollectionType javaType = OBJECT_MAPPER.getTypeFactory()
                    .constructCollectionType(collectionType, elementType);

            return OBJECT_MAPPER.readValue(jsonString, javaType);
        } catch (IOException e) {
            LOGGER.error(UNEXPECTED_EXCEPTION, e);
            return null;
        }
    }

    public JsonNode readJsonNodeFromFile(final String jsonFileName) {

        try (InputStream fis = inputStreamFromFileName(jsonFileName)) {
            return READER.fromInputStream(fis);
        } catch (Exception e) {
            LOGGER.error(UNEXPECTED_EXCEPTION, e);
            return null;
        }
    }

    public <T> JsonNode readJsonNodeFromObject(final T object) {

        String serializedObject = serialize(object);
        try (Reader objectReader = new StringReader(serializedObject)) {
            return READER.fromReader(objectReader);
        } catch (Exception e) {
            LOGGER.error(UNEXPECTED_EXCEPTION, e);
            return null;
        }
    }

    private InputStream inputStreamFromFileName(final String fileName) throws IOException {
        URL resource = this.getClass().getClassLoader().getResource(fileName);
        if (resource == null) {
            LOGGER.error("Could not find file [{}]", fileName);
            return null;
        }
        return resource.openStream();
    }

}
