/*******************************************************************************
 * Copyright 2018 General Electric Company
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

package org.eclipse.keti.acs.utils

import com.fasterxml.jackson.annotation.JsonInclude.Include
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import com.github.fge.jackson.JsonNodeReader
import org.slf4j.LoggerFactory
import java.io.IOException
import java.io.InputStream
import java.io.StringReader

private val LOGGER = LoggerFactory.getLogger(JsonUtils::class.java)

private val OBJECT_MAPPER = ObjectMapper().setSerializationInclusion(Include.NON_NULL)
private val READER = JsonNodeReader()
private const val UNEXPECTED_EXCEPTION = "Unexpected Exception"

/**
 * JSON utility methods convert from/to json files, objects and string.
 *
 * @author acs-engineers@ge.com
 */
class JsonUtils {

    fun <T> deserializeFromFile(
        fileName: String,
        type: Class<T>
    ): T? {
        try {
            inputStreamFromFileName(fileName)!!.use { fis -> return OBJECT_MAPPER.readValue(fis, type) }
        } catch (e: Exception) {
            LOGGER.error(UNEXPECTED_EXCEPTION, e)
            return null
        }
    }

    fun <T> serialize(jsonObject: T): String? {
        return try {
            OBJECT_MAPPER.writeValueAsString(jsonObject)
        } catch (e: Exception) {
            LOGGER.error(UNEXPECTED_EXCEPTION, e)
            null
        }
    }

    fun <T> deserialize(
        jsonString: String,
        type: Class<T>
    ): T? {
        return try {
            OBJECT_MAPPER.readValue(jsonString, type)
        } catch (e: IOException) {
            LOGGER.error(UNEXPECTED_EXCEPTION, e)
            null
        }
    }

    fun <C : Collection<T>, T> deserialize(
        jsonString: String,
        collectionType: Class<C>,
        elementType: Class<T>
    ): C? {
        return try {
            val javaType = OBJECT_MAPPER.typeFactory.constructCollectionType(collectionType, elementType)

            OBJECT_MAPPER.readValue<C>(jsonString, javaType)
        } catch (e: IOException) {
            LOGGER.error(UNEXPECTED_EXCEPTION, e)
            null
        }
    }

    fun readJsonNodeFromFile(jsonFileName: String): JsonNode? {

        try {
            inputStreamFromFileName(jsonFileName)!!.use { fis -> return READER.fromInputStream(fis) }
        } catch (e: Exception) {
            LOGGER.error(UNEXPECTED_EXCEPTION, e)
            return null
        }
    }

    fun <T> readJsonNodeFromObject(jsonObject: T): JsonNode? {

        val serializedObject = serialize(jsonObject)
        try {
            StringReader(serializedObject!!).use { objectReader -> return READER.fromReader(objectReader) }
        } catch (e: Exception) {
            LOGGER.error(UNEXPECTED_EXCEPTION, e)
            return null
        }
    }

    @Throws(IOException::class)
    private fun inputStreamFromFileName(fileName: String): InputStream? {
        val resource = this.javaClass.classLoader.getResource(fileName)
        if (resource == null) {
            LOGGER.error("Could not find file [{}]", fileName)
            return null
        }
        return resource.openStream()
    }
}
