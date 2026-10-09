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
package io.camunda.connector.feel;

import com.anyilanxin.kunpeng.client.KunpengClient;
import io.camunda.connector.jackson.ConnectorsObjectMapperSupplier;
import tools.jackson.databind.ObjectMapper;

/**
 * Step builder for {@link FeelExpressionEvaluator} instances. The entry points return distinct
 * sub-builders so that backend-specific options (e.g. {@code scopeKey}, {@code tenantId}) are only
 * reachable for the backend that actually supports them.
 *
 * <p>Pick {@link #local()} for embedded Expression evaluation, or {@link #client(KunpengClient)}
 * for cluster-based evaluation (allowing access to cluster variables like {@code
 * camunda.vars.env.*}).
 *
 * <pre>{@code
 * FeelExpressionEvaluator local = FeelExpressionEvaluatorBuilder.local().build();
 *
 * FeelExpressionEvaluator cluster = FeelExpressionEvaluatorBuilder.client(client)
 *     .tenantId("acme")
 *     .scopeKey(elementInstanceKey)
 *     .objectMapper(objectMapper)
 *     .build();
 * }</pre>
 */
public final class FeelExpressionEvaluatorBuilder {

  private FeelExpressionEvaluatorBuilder() {}

  /** Start building a {@link LocalFeelExpressionEvaluator}. */
  public static LocalStep local() {
    return new LocalStep();
  }

  /** Start building a {@link KunpengClientFeelExpressionEvaluator}. */
  public static KunpengClientStep client(final KunpengClient client) {
    if (client == null) {
      throw new IllegalArgumentException("client must not be null");
    }
    return new KunpengClientStep(client);
  }

  /** Step builder for the embedded Expression engine evaluator. */
  public static final class LocalStep {
    private LocalStep() {}

    public FeelExpressionEvaluator build() {
      return new LocalFeelExpressionEvaluator();
    }
  }

  /** Step builder for the cluster-based evaluator. */
  public static final class KunpengClientStep {
    private final KunpengClient client;
    private String tenantId;
    private Long scopeKey;
    private ObjectMapper objectMapper;

    private KunpengClientStep(final KunpengClient client) {
      this.client = client;
    }

    public KunpengClientStep tenantId(final String tenantId) {
      this.tenantId = tenantId;
      return this;
    }

    public KunpengClientStep scopeKey(final Long scopeKey) {
      this.scopeKey = scopeKey;
      return this;
    }

    public KunpengClientStep objectMapper(final ObjectMapper objectMapper) {
      this.objectMapper = objectMapper;
      return this;
    }

    public FeelExpressionEvaluator build() {
      final ObjectMapper mapper =
          objectMapper != null ? objectMapper : ConnectorsObjectMapperSupplier.getCopy();
      return new KunpengClientFeelExpressionEvaluator(client, tenantId, scopeKey, mapper);
    }
  }
}
