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
package io.camunda.connector.http.client.client.apache.builder.parts;

import static org.apache.hc.core5.http.ContentType.MULTIPART_FORM_DATA;
import static org.apache.hc.core5.http.HttpHeaders.CONTENT_TYPE;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.camunda.connector.api.error.ConnectorException;
import io.camunda.connector.http.client.HttpClientObjectMapperSupplier;
import io.camunda.connector.http.client.model.HttpClientRequest;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import org.apache.hc.client5.http.entity.mime.HttpMultipartMode;
import org.apache.hc.client5.http.entity.mime.MultipartEntityBuilder;
import org.apache.hc.core5.http.ContentType;
import org.apache.hc.core5.http.HttpEntity;
import org.apache.hc.core5.http.io.entity.HttpEntities;
import org.apache.hc.core5.http.io.entity.StringEntity;
import org.apache.hc.core5.http.io.support.ClassicRequestBuilder;
import org.apache.hc.core5.http.message.BasicNameValuePair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;

/**
 * Maps the request body of a {@link HttpClientRequest} to an Apache {@link ClassicRequestBuilder}.
 */
public class ApacheRequestBodyBuilder implements ApacheRequestPartBuilder {

  private static final Logger LOG = LoggerFactory.getLogger(ApacheRequestBodyBuilder.class);

  public static final String EMPTY_BODY = "";
  public static final ObjectMapper mapperIgnoreNull =
      HttpClientObjectMapperSupplier.getCopy()
          .rebuild()
          .changeDefaultPropertyInclusion(
              _ ->
                  JsonInclude.Value.construct(
                      JsonInclude.Include.NON_NULL, JsonInclude.Include.NON_NULL))
          .build();
  public static final ObjectMapper mapperSendNull = HttpClientObjectMapperSupplier.getCopy();

  @Override
  public void build(final ClassicRequestBuilder builder, final HttpClientRequest request) {
    if (request.getMethod().supportsBody) {
      if (!request.hasBody()) {
        /**
         * We need to set the body to something not null due to how {@link
         * CustomApacheHttpClient}{@link #build(ClassicRequestBuilder, HttpClientRequest)} works. If
         * the body is null, the {@link ClassicRequestBuilder} will override it in some cases
         * (PUT/POST using query parameters).
         */
        builder.setEntity(EMPTY_BODY);
        return;
      }

      if (request.getBody() instanceof final Map<?, ?> body) {
        tryGetContentType(request)
            .ifPresentOrElse(
                contentType ->
                    builder.setEntity(createEntityForContentType(contentType, body, request)),
                () -> builder.setEntity(createStringEntity(request)));
      } else {
        builder.setEntity(createStringEntity(request));
      }
    }
  }

  private HttpEntity createEntityForContentType(
      final ContentType contentType, final Map<?, ?> body, final HttpClientRequest request) {
    final HttpEntity entity;
    if (contentType.getMimeType().equalsIgnoreCase(MULTIPART_FORM_DATA.getMimeType())) {
      entity = createMultipartEntity(contentType, body);
    } else if (contentType
        .getMimeType()
        .equalsIgnoreCase(ContentType.APPLICATION_FORM_URLENCODED.getMimeType())) {
      entity = createUrlEncodedFormEntity(body);
    } else {
      entity = createStringEntity(request);
    }
    return entity;
  }

  private HttpEntity createMultipartEntity(final ContentType contentType, final Map<?, ?> body) {
    final MultipartEntityBuilder builder = MultipartEntityBuilder.create();
    builder.setMode(HttpMultipartMode.LEGACY);
    Optional.ofNullable(contentType.getParameter("boundary")).ifPresent(builder::setBoundary);
    for (final Map.Entry<?, ?> entry : body.entrySet()) {
      if (entry.getValue() != null) {
        builder.addTextBody(
            String.valueOf(entry.getKey()), String.valueOf(entry.getValue()), MULTIPART_FORM_DATA);
      }
    }
    return builder.build();
  }

  private Optional<ContentType> tryGetContentType(final HttpClientRequest request) {
    return request.getHeader(CONTENT_TYPE).map(ContentType::parse);
  }

  private HttpEntity createStringEntity(final HttpClientRequest request) {
    final Object body = request.getBody();
    final Optional<ContentType> contentType = tryGetContentType(request);
    try {
      return body instanceof final String s
          ? new StringEntity(
              s, contentType.orElse(ContentType.TEXT_PLAIN.withCharset(StandardCharsets.UTF_8)))
          : new StringEntity(
              request.isIgnoreNullValues()
                  ? mapperIgnoreNull.writeValueAsString(body)
                  : mapperSendNull.writeValueAsString(body),
              contentType.orElse(ContentType.APPLICATION_JSON.withCharset(StandardCharsets.UTF_8)));
    } catch (final JacksonException e) {
      throw new ConnectorException("Failed to serialize request body:" + body, e);
    }
  }

  private HttpEntity createUrlEncodedFormEntity(final Map<?, ?> body) {
    return HttpEntities.createUrlEncoded(
        body.entrySet().stream()
            .map(
                e ->
                    new BasicNameValuePair(
                        String.valueOf(e.getKey()),
                        Optional.ofNullable(e.getValue()).map(String::valueOf).orElse(null)))
            .collect(Collectors.toList()),
        StandardCharsets.UTF_8);
  }
}
