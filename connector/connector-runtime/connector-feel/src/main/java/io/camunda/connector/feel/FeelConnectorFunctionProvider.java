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

import io.camunda.connector.feel.function.BackoffFunction;
import io.camunda.connector.feel.function.BpmnErrorFunction;
import io.camunda.connector.feel.function.IgnoreErrorFunction;
import io.camunda.connector.feel.function.JobErrorFunction;
import io.camunda.connector.feel.function.QLFunction;
import java.util.List;

/** Provider of Connector-related Expression functions like 'bpmnError'. */
public final class FeelConnectorFunctionProvider {

  public static final String ERROR_TYPE_PROPERTY = "errorType";
  public static final String BPMN_ERROR_TYPE_VALUE = "bpmnError";
  public static final String JOB_ERROR_TYPE_VALUE = "jobError";
  public static final String IGNORE_ERROR_TYPE_VALUE = "ignoreError";

  /** Connector-specific Expression functions registered in the local QLExpress engine. */
  public static final List<QLFunction> FUNCTIONS =
      List.of(
          new BpmnErrorFunction(),
          new JobErrorFunction(),
          new IgnoreErrorFunction(),
          new BackoffFunction());
}
