/*
 * Copyright © 2026 anyilanxin zxh (anyilanxin@aliyun.com)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * Software distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.anyilanxin.kunpeng.client.command.job.worker;

import com.anyilanxin.kunpeng.client.command.*;
import com.anyilanxin.kunpeng.client.command.job.ActivatedJob;
import com.anyilanxin.kunpeng.client.command.job.JobKind;
import com.anyilanxin.kunpeng.gateway.grpc.service.JobServiceOuterClass;
import com.fasterxml.jackson.annotation.JsonIgnore;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

public final class ActivatedJobImpl implements ActivatedJob {

  @JsonIgnore private final JsonMapper jsonMapper;

  private final long key;
  private final String type;
  private final Map<String, String> customHeaders;
  private final long processInstanceId;
  private final int processDefinitionVersion;
  private final long processDefinitionId;
  private final String processDefinitionKey;
  private final String activityDefinitionKey;
  private final long activityInstanceId;
  private final String tenantId;
  private final String worker;
  private final int retries;
  private final long deadline;
  private final String variables;
  private final UserTaskProperties userTask;
  private final JobKind kind;
  private final ListenerEventType listenerEventType;

  private Map<String, Object> variablesAsMap;

  public ActivatedJobImpl(
      final JsonMapper jsonMapper, final JobServiceOuterClass.JobDelivery delivery) {
    this.jsonMapper = jsonMapper;

    key = delivery.getJobKey();
    type = delivery.getType();

    // the default value of a string in Protobuf is an empty string, so this could fail if no
    // headers were given
    final String customHeaders = delivery.getCustomHeaders().toString(StandardCharsets.UTF_8);
    this.customHeaders =
        customHeaders.isEmpty() ? new HashMap<>() : jsonMapper.fromJsonAsStringMap(customHeaders);
    worker = delivery.getWorker();
    retries = delivery.getRetries();
    deadline = delivery.getDeadline();
    variables = delivery.getVariables().toString(StandardCharsets.UTF_8);
    processInstanceId = delivery.getProcessInstanceId();
    processDefinitionKey = "";
    processDefinitionId = -1;
    processDefinitionVersion = -1;
    activityDefinitionKey = delivery.getElementId();
    activityInstanceId = delivery.getActivityInstanceId();
    tenantId = delivery.getTenantId();
    final String userTaskJson = delivery.getUserTask().toString(StandardCharsets.UTF_8);
    userTask = userTaskJson.isEmpty() ? null : null; // user task 属性经 UserTaskService 承载
    kind = kindOf(delivery.getJobKind());
    listenerEventType = ListenerEventType.UNSPECIFIED; // 监听事件类型暂不经 wire 传递语义值
  }

  @Override
  public long getKey() {
    return key;
  }

  @Override
  public String getType() {
    return type;
  }

  @Override
  public long getProcessDefinitionId() {
    return processDefinitionId;
  }

  @Override
  public String getProcessDefinitionKey() {
    return processDefinitionKey;
  }

  @Override
  public int getProcessDefinitionVersion() {
    return processDefinitionVersion;
  }

  @Override
  public long getProcessInstanceId() {
    return processInstanceId;
  }

  @Override
  public long getActivityInstanceId() {
    return activityInstanceId;
  }

  @Override
  public String getActivityDefinitionKey() {
    return activityDefinitionKey;
  }

  @Override
  public Map<String, String> getCustomHeaders() {
    return customHeaders;
  }

  @Override
  public String getWorker() {
    return worker;
  }

  @Override
  public int getRetries() {
    return retries;
  }

  @Override
  public long getDeadline() {
    return deadline;
  }

  @Override
  public String getVariables() {
    return variables;
  }

  @Override
  public Map<String, Object> getVariablesAsMap() {
    if (variablesAsMap == null) {
      variablesAsMap = jsonMapper.fromJsonAsMap(variables);
    }
    return variablesAsMap;
  }

  @Override
  public <T> T getVariablesAsType(final Class<T> variableType) {
    return jsonMapper.fromJson(variables, variableType);
  }

  @Override
  public <T> T getVariablesAsType(final String name, final Class<T> variableType) {
    final Map<String, Object> variablesAsMap = getVariablesAsMap();
    final Object object = variablesAsMap.get(name);
    if (object == null) {
      throw new ClientException(String.format("The variable %s is not available", name));
    }
    return jsonMapper.fromJson(jsonMapper.toJson(object), variableType);
  }

  @Override
  public Object getVariable(final String name) {
    final Map<String, Object> variables = getVariablesAsMap();
    if (!variables.containsKey(name)) {
      throw new ClientException(String.format("The variable %s is not available", name));
    }
    return getVariablesAsMap().get(name);
  }

  @Override
  public UserTaskProperties getUserTask() {
    return userTask;
  }

  @Override
  public JobKind getKind() {
    return kind;
  }

  @Override
  public ListenerEventType getListenerEventType() {
    return listenerEventType;
  }

  @Override
  public String toJson() {
    return jsonMapper.toJson(this);
  }

  @Override
  public String getTenantId() {
    return tenantId;
  }

  @Override
  public String toString() {
    return toJson();
  }

  private static JobKind kindOf(final int wireValue) {
    final JobKind[] values = JobKind.values();
    return wireValue >= 0 && wireValue < values.length ? values[wireValue] : null;
  }
}
