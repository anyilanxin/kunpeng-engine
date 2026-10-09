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
import com.anyilanxin.kunpeng.client.command.processinstance.CreateProcessInstanceCommand;
import com.anyilanxin.kunpeng.client.command.processinstance.CreateProcessInstanceCommand.CreateProcessInstanceCommandStep2;
import com.anyilanxin.kunpeng.client.command.processinstance.CreateProcessInstanceCommand.CreateProcessInstanceCommandStep3;
import com.anyilanxin.kunpeng.client.command.processinstance.CreateProcessInstanceCommand.CreateProcessInstanceWithResultCommandStep1;
import io.camunda.connector.runtime.core.testutil.response.ProcessInstanceEventDummy;
import io.camunda.connector.runtime.core.testutil.response.ProcessInstanceResultDummy;
import java.io.InputStream;
import java.time.Duration;
import java.util.Map;

public class CreateCommandDummy
    implements CreateProcessInstanceCommand,
        CreateProcessInstanceCommandStep2,
        CreateProcessInstanceCommandStep3 {

  private final Map<String, Object> variables;

  public CreateCommandDummy() {
    this.variables = Map.of();
  }

  public CreateCommandDummy(Map<String, Object> variables) {
    this.variables = variables;
  }

  @Override
  public CreateProcessInstanceCommandStep2 processDefinitionKey(String processDefinitionKey) {
    return this;
  }

  @Override
  public CreateProcessInstanceCommandStep3 processDefinitionId(long processDefinitionId) {
    return this;
  }

  @Override
  public CreateProcessInstanceCommandStep3 version(int version) {
    return this;
  }

  @Override
  public CreateProcessInstanceCommandStep3 latestVersion() {
    return this;
  }

  @Override
  public CreateProcessInstanceCommandStep3 tenantId(String tenantId) {
    return this;
  }

  @Override
  public CreateProcessInstanceCommandStep3 variables(InputStream variables) {
    return this;
  }

  @Override
  public CreateProcessInstanceCommandStep3 variables(String variables) {
    return this;
  }

  @Override
  public CreateProcessInstanceCommandStep3 variables(Map<String, Object> variables) {
    return this;
  }

  @Override
  public CreateProcessInstanceCommandStep3 variables(Object variables) {
    return this;
  }

  @Override
  public CreateProcessInstanceCommandStep3 variable(String key, Object value) {
    return this;
  }

  @Override
  public CreateProcessInstanceCommandStep3 startBeforeElement(String activityDefinitionKey) {
    return this;
  }

  @Override
  public CreateProcessInstanceCommandStep3 terminateAfterElement(String activityDefinitionKey) {
    return this;
  }

  @Override
  public CreateProcessInstanceWithResultCommandStep1 withResult() {
    return new WithResultCommandDummy(variables);
  }

  @Override
  public CreateProcessInstanceCommandStep3 requestTimeout(Duration requestTimeout) {
    return this;
  }

  @SuppressWarnings({"rawtypes", "unchecked"})
  @Override
  public KunpengClientFutureImpl send() {
    KunpengClientFutureImpl future = new KunpengClientFutureImpl<>();
    future.complete(new ProcessInstanceEventDummy());
    return future;
  }

  public static class WithResultCommandDummy
      implements CreateProcessInstanceWithResultCommandStep1 {

    private final Map<String, Object> variables;

    public WithResultCommandDummy(Map<String, Object> variables) {
      this.variables = variables;
    }

    @Override
    public CreateProcessInstanceWithResultCommandStep1 tenantId(String tenantId) {
      return this;
    }

    @Override
    public CreateProcessInstanceWithResultCommandStep1 fetchVariables(
        java.util.List<String> fetchVariables) {
      return this;
    }

    @Override
    public CreateProcessInstanceWithResultCommandStep1 fetchVariables(String... fetchVariables) {
      return this;
    }

    @Override
    public CreateProcessInstanceWithResultCommandStep1 requestTimeout(Duration requestTimeout) {
      return this;
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    @Override
    public KunpengClientFutureImpl send() {
      KunpengClientFutureImpl future = new KunpengClientFutureImpl<>();
      future.complete(new ProcessInstanceResultDummy(variables));
      return future;
    }
  }
}
