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
package io.camunda.connector.feel.function;

import static io.camunda.connector.feel.FeelConnectorFunctionProvider.ERROR_TYPE_PROPERTY;
import static io.camunda.connector.feel.FeelConnectorFunctionProvider.IGNORE_ERROR_TYPE_VALUE;

import com.alibaba.qlexpress4.runtime.Parameters;
import com.alibaba.qlexpress4.runtime.QContext;
import java.util.LinkedHashMap;
import java.util.Map;

/** Expression function {@code ignoreError([variables])}. */
public class IgnoreErrorFunction implements QLFunction {

  public static final String NAME = "ignoreError";

  private static final String VARIABLES = "variables";

  @Override
  public Object call(final QContext qContext, final Parameters parameters) throws Throwable {
    final Map<String, Object> result = new LinkedHashMap<>();
    result.put(ERROR_TYPE_PROPERTY, IGNORE_ERROR_TYPE_VALUE);
    if (parameters.size() > 0) {
      result.put(VARIABLES, FunctionHelper.toMap(parameters, 0, NAME, VARIABLES));
    }
    return result;
  }

  @Override
  public String getSignature() {
    return NAME;
  }
}
