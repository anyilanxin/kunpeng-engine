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
import java.util.Map;

public interface CommandWithLocalVariables<T> {
  /**
   * @param localVariables the localVariables (JSON) as String
   * @return the builder for this command. Call {@link #send()} to complete the command and send it
   *     to the broker.
   */
  T localVariables(String localVariables);

  /**
   * @param localVariables the localVariables as object
   * @return the builder for this command. Call {@link #send()} to complete the command and send it
   *     to the broker.
   */
  T localVariables(Object localVariables);

  /**
   * @param localVariables the localVariables (JSON) as stream
   * @return the builder for this command. Call {@link #send()} to complete the command and send it
   *     to the broker.
   */
  T localVariables(InputStream localVariables);

  /**
   * @param localVariables the localVariables as map
   * @return the builder for this command. Call {@link #send()} to complete the command and send it
   *     to the broker.
   */
  T localVariables(Map<String, Object> localVariables);

  /**
   * @param key the key of the variable as string
   * @param value the value of the variable as object
   * @return the builder for this command. Call {@link #send()} to complete the command and send it
   *     to the broker.
   */
  T localVariable(String key, Object value);
}
