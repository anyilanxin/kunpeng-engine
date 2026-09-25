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
package com.anyilanxin.kunpeng.client.command;

import java.io.InputStream;
import java.util.Collections;
import java.util.Map;

public abstract class CommandWithVariables2<T> {

  protected final JsonMapper objectMapper;

  public CommandWithVariables2(final JsonMapper jsonMapper) {
    objectMapper = jsonMapper;
  }

  public T variables(final InputStream variables) {
    ArgumentUtil.ensureNotNull("variables", variables);
    return setVariablesInternal(objectMapper.validateJson("variables", variables));
  }

  public T variables(final String variables) {
    ArgumentUtil.ensureNotNull("variables", variables);
    return setVariablesInternal(objectMapper.validateJson("variables", variables));
  }

  public T variables(final Map<String, Object> variables) {
    ArgumentUtil.ensureNotNull("variables", variables);
    return variables((Object) variables);
  }

  public T variables(final Object variables) {
    ArgumentUtil.ensureNotNull("variables", variables);
    return setVariablesInternal(objectMapper.toJson(variables));
  }

  public T variable(final String key, final Object value) {
    ArgumentUtil.ensureNotNull("key", key);
    return variables(Collections.singletonMap(key, value));
  }

  protected abstract T setVariablesInternal(String variables);
}
