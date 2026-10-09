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
package io.camunda.connector.runtime.core.testutil.command;

import com.anyilanxin.kunpeng.client.command.KunpengClientFutureImpl;
import com.anyilanxin.kunpeng.client.command.message.correlation.MessageCorrelationCommand;
import com.anyilanxin.kunpeng.client.command.message.correlation.MessageCorrelationCommand.MessageCorrelationCommandStep1;
import io.camunda.connector.runtime.core.testutil.response.CorrelateMessageResponseDummy;
import java.io.InputStream;
import java.time.Duration;
import java.util.Map;

public class CorrelateMessageCommandDummy
    implements MessageCorrelationCommand, MessageCorrelationCommandStep1 {

  @Override
  public MessageCorrelationCommandStep1 messageName(String messageName) {
    return this;
  }

  @Override
  public MessageCorrelationCommandStep1 processInstanceId(long processInstanceId) {
    return this;
  }

  @Override
  public MessageCorrelationCommandStep1 correlationKey(String correlationKey) {
    return this;
  }

  @Override
  public MessageCorrelationCommandStep1 tenantId(String tenantId) {
    return this;
  }

  @Override
  public MessageCorrelationCommandStep1 variables(InputStream variables) {
    return this;
  }

  @Override
  public MessageCorrelationCommandStep1 variables(String variables) {
    return this;
  }

  @Override
  public MessageCorrelationCommandStep1 variables(Map<String, Object> variables) {
    return this;
  }

  @Override
  public MessageCorrelationCommandStep1 variables(Object variables) {
    return this;
  }

  @Override
  public MessageCorrelationCommandStep1 variable(String key, Object value) {
    return this;
  }

  @Override
  public MessageCorrelationCommandStep1 requestTimeout(Duration requestTimeout) {
    return this;
  }

  @SuppressWarnings({"rawtypes", "unchecked"})
  @Override
  public KunpengClientFutureImpl send() {
    KunpengClientFutureImpl future = new KunpengClientFutureImpl<>();
    future.complete(new CorrelateMessageResponseDummy());
    return future;
  }
}
