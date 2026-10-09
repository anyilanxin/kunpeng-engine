/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH
 * under one or more contributor license agreements. Licensed under a proprietary license.
 * See the License.txt file for more information. Do not use this file
 * except in compliance with the proprietary license.
 */
package io.camunda.connector.http.base;

import static io.camunda.connector.http.client.utils.JsonHelper.isJsonStringValid;

import io.camunda.connector.http.base.model.HttpCommonResult;
import io.camunda.connector.http.client.mapper.ResponseMapper;
import io.camunda.connector.http.client.mapper.StreamingHttpResponse;
import io.camunda.connector.http.client.utils.HeadersHelper;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tools.jackson.databind.ObjectMapper;

/** Maps a {@link StreamingHttpResponse} to a {@link HttpCommonResult}. */
public class HttpCommonResultMapper implements ResponseMapper<HttpCommonResult> {

  private static final Logger LOGGER = LoggerFactory.getLogger(HttpCommonResultMapper.class);
  private final ObjectMapper objectMapper;

  public HttpCommonResultMapper(final ObjectMapper objectMapper) {
    this.objectMapper = objectMapper;
  }

  @Override
  public HttpCommonResult apply(final StreamingHttpResponse streamingHttpResponse) {
    final Object body = deserializeBody(streamingHttpResponse.body());
    final Map<String, Object> headers =
        HeadersHelper.flattenHeaders(streamingHttpResponse.headers());
    return new HttpCommonResult(
        streamingHttpResponse.status(), headers, body, streamingHttpResponse.reason());
  }

  /**
   * Deserializes the body from the input stream. Tries to parse the body as JSON, if it fails,
   * returns the body as a string.
   *
   * @param bodyInputStream the input stream of the response body
   * @return the deserialized body
   */
  private Object deserializeBody(final InputStream bodyInputStream) {
    if (bodyInputStream == null) {
      return null;
    } else {
      try (bodyInputStream) {
        return deserializeBody(bodyInputStream.readAllBytes());
      } catch (final IOException e) {
        LOGGER.error("Failed to read response body: {}", e.getMessage(), e);
        throw new RuntimeException("Failed to read response body: " + e.getMessage(), e);
      }
    }
  }

  /**
   * Extracts the body from the response content. Tries to parse the body as JSON, if it fails,
   * returns the body as a string.
   *
   * @param content the response content
   */
  private Object deserializeBody(final byte[] content) throws IOException {
    final String bodyString = new String(content, StandardCharsets.UTF_8);
    if (StringUtils.isNotBlank(bodyString)) {
      return isJsonStringValid(bodyString)
          ? objectMapper.readValue(bodyString, Object.class)
          : bodyString;
    }
    return null;
  }
}
