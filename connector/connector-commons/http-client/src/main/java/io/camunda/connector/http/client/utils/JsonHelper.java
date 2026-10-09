/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH
 * Copyright © 2026 anyilanxin zxh (anyilanxin@aliyun.com)
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
package io.camunda.connector.http.client.utils;

import io.camunda.connector.http.client.HttpClientObjectMapperSupplier;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

public class JsonHelper {

  private static final ObjectMapper objectMapper = HttpClientObjectMapperSupplier.getCopy();

  public static boolean isJsonStringValid(final String jsonString) {
    if (jsonString == null) {
      return false;
    }
    try {
      final JsonNode jsonNode = objectMapper.readTree(jsonString);
      return jsonNode.isObject() || jsonNode.isArray();
    } catch (final JacksonException e) {
      return false;
    }
  }
}
