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
package io.camunda.connector.runtime.core.inbound;

import com.anyilanxin.kunpeng.client.KunpengClient;
import io.camunda.connector.api.inbound.InboundConnectorContext;
import io.camunda.connector.api.validation.ValidationProvider;
import io.camunda.connector.runtime.core.inbound.activitylog.ActivityLogWriter;
import io.camunda.connector.runtime.core.inbound.correlation.InboundCorrelationHandler;
import io.camunda.connector.runtime.core.inbound.details.InboundConnectorDetails.ValidInboundConnectorDetails;
import io.camunda.connector.runtime.core.secret.SecretProviderAggregator;
import java.util.Objects;
import java.util.function.Consumer;
import tools.jackson.databind.ObjectMapper;

public class DefaultInboundConnectorContextFactory implements InboundConnectorContextFactory {
  private final ObjectMapper objectMapper;
  private final InboundCorrelationHandler correlationHandler;
  private final SecretProviderAggregator secretProviderAggregator;
  private final ValidationProvider validationProvider;
  private final KunpengClient client;

  public DefaultInboundConnectorContextFactory(
      final ObjectMapper mapper,
      final InboundCorrelationHandler correlationHandler,
      final SecretProviderAggregator secretProviderAggregator,
      final ValidationProvider validationProvider,
      final KunpengClient client) {
    this.objectMapper = mapper;
    this.correlationHandler = correlationHandler;
    this.secretProviderAggregator = secretProviderAggregator;
    this.validationProvider = validationProvider;
    this.client = Objects.requireNonNull(client, "client must not be null");
  }

  @Override
  public InboundConnectorContext createContext(
      final ValidInboundConnectorDetails connectorDetails,
      final Consumer<Throwable> cancellationCallback,
      final ActivityLogWriter logWriter) {
    return new InboundConnectorContextImpl(
        secretProviderAggregator,
        validationProvider,
        connectorDetails,
        correlationHandler,
        cancellationCallback,
        objectMapper,
        logWriter,
        client);
  }
}
