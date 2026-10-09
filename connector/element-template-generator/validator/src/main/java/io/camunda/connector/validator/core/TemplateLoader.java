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
package io.camunda.connector.validator.core;

import java.nio.file.Path;
import tools.jackson.core.StreamReadFeature;
import tools.jackson.core.TokenStreamLocation;
import tools.jackson.core.exc.JacksonIOException;
import tools.jackson.core.exc.StreamReadException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;

/**
 * Parses element-template JSON files. Strict mode rejects duplicate object keys — which Jackson
 * would otherwise silently coalesce (last-wins) — and surfaces them as a {@code duplicate-keys}
 * finding instead of {@code json-parse}. Mirrors the duplicate-key check that the Camunda Web
 * Modeler runs at editor save-time.
 */
public final class TemplateLoader {

  public static final String DUPLICATE_KEYS_RULE = "duplicate-keys";
  public static final String JSON_PARSE_RULE = "json-parse";

  private static final ObjectMapper MAPPER =
      JsonMapper.builder().enable(StreamReadFeature.STRICT_DUPLICATE_DETECTION).build();

  private TemplateLoader() {}

  public static Result load(Path path) {
    try {
      return new Result(MAPPER.readTree(path.toFile()), null);
    } catch (StreamReadException e) {
      String original = e.getOriginalMessage();
      // Jackson 3 reports duplicates as `Duplicate Object property "x"` (Jackson 2 said
      // `Duplicate field 'x'`); STRICT_DUPLICATE_DETECTION produces no other duplicate wording
      if (original != null && original.startsWith("Duplicate Object property")) {
        TokenStreamLocation loc = e.getLocation();
        String where =
            loc == null ? "" : " at line " + loc.getLineNr() + ", column " + loc.getColumnNr();
        return new Result(
            null, Finding.error(path, "/", DUPLICATE_KEYS_RULE, original + where + "."));
      }
      return new Result(
          null,
          Finding.error(path, "/", JSON_PARSE_RULE, "Failed to parse JSON: " + e.getMessage()));
    } catch (JacksonIOException e) {
      return new Result(
          null,
          Finding.error(path, "/", JSON_PARSE_RULE, "Failed to parse JSON: " + e.getMessage()));
    }
  }

  public record Result(JsonNode node, Finding finding) {
    public boolean ok() {
      return finding == null;
    }
  }
}
