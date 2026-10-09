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

import io.camunda.connector.api.inbound.InboundConnectorContext;
import io.camunda.connector.runtime.core.inbound.activitylog.ActivityLogWriter;
import io.camunda.connector.runtime.core.inbound.details.InboundConnectorDetails.ValidInboundConnectorDetails;
import java.util.function.Consumer;

/** Factory interface for creating {@link InboundConnectorContext} instances. */
public interface InboundConnectorContextFactory {

  /**
   * Creates an {@link InboundConnectorContext} instance based on the provided parameters.
   *
   * @param connectorDetails The specific inbound connector data which gives details about the
   *     connector and its related properties.
   * @param cancellationCallback Callback that gets invoked during connector execution errors or
   *     cancellations.
   * @return A newly created {@link InboundConnectorContext} instance, tailored to the provided
   *     parameters.
   */
  InboundConnectorContext createContext(
      final ValidInboundConnectorDetails connectorDetails,
      final Consumer<Throwable> cancellationCallback,
      final ActivityLogWriter logWriter);
}
