/*
 * Copyright © 2017 camunda services GmbH (info@camunda.com)
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
 */
package com.anyilanxin.kunpeng.client;

import com.anyilanxin.kunpeng.client.command.JsonMapper;
import java.io.InputStream;
import java.util.Map;
import tools.jackson.core.exc.JacksonIOException;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.DeserializationFeature;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.SerializationFeature;

public final class KunpengObjectMapper implements JsonMapper {

  private static final TypeReference<Map<String, Object>> MAP_TYPE_REFERENCE =
      new TypeReference<>() {};

  private static final TypeReference<Map<String, String>> STRING_MAP_TYPE_REFERENCE =
      new TypeReference<>() {};

  private final ObjectMapper objectMapper;

  public KunpengObjectMapper() {
    this(new ObjectMapper());
  }

  public KunpengObjectMapper(final ObjectMapper objectMapper) {
    this.objectMapper = objectMapper;
    this.objectMapper
        .rebuild()
        .configure(SerializationFeature.FAIL_ON_EMPTY_BEANS, false)
        .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
        .build();
  }

  @Override
  public <T> T fromJson(final String json, final Class<T> typeClass) {
    try {
      return objectMapper.readValue(json, typeClass);
    } catch (final JacksonIOException e) {
      throw new InternalClientException(
          String.format("Failed to deserialize json '%s' to class '%s'", json, typeClass), e);
    }
  }

  @Override
  public Map<String, Object> fromJsonAsMap(final String json) {
    try {
      return objectMapper.readValue(json, MAP_TYPE_REFERENCE);
    } catch (final JacksonIOException e) {
      throw new InternalClientException(
          String.format("Failed to deserialize json '%s' to 'Map<String, Object>'", json), e);
    }
  }

  @Override
  public Map<String, String> fromJsonAsStringMap(final String json) {
    try {
      return objectMapper.readValue(json, STRING_MAP_TYPE_REFERENCE);
    } catch (final JacksonIOException e) {
      throw new InternalClientException(
          String.format("Failed to deserialize json '%s' to 'Map<String, String>'", json), e);
    }
  }

  @Override
  public String toJson(final Object value) {
    try {
      return objectMapper.writeValueAsString(value);
    } catch (final Exception e) {
      throw new InternalClientException(
          String.format("Failed to serialize object '%s' to json", value), e);
    }
  }

  @Override
  public String validateJson(final String propertyName, final String jsonInput) {
    try {
      return objectMapper.readTree(jsonInput).toString();
    } catch (final Exception e) {
      throw new InternalClientException(
          String.format(
              "Failed to validate json input '%s' for property '%s'", jsonInput, propertyName),
          e);
    }
  }

  @Override
  public String validateJson(final String propertyName, final InputStream jsonInput) {
    try {
      return objectMapper.readTree(jsonInput).toString();
    } catch (final Exception e) {
      throw new InternalClientException(
          String.format("Failed to validate json input stream for property '%s'", propertyName), e);
    }
  }
}
