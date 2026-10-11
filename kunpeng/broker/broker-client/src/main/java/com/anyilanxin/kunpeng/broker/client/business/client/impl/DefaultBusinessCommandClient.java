/*
 * Copyright © 2026 anyilanxin zxh(anyilanxin@aliyun.com)
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
package com.anyilanxin.kunpeng.broker.client.business.client.impl;

import com.anyilanxin.kunpeng.broker.client.business.client.BusinessCommandClient;
import com.anyilanxin.kunpeng.broker.client.business.client.DeploymentClient;
import com.anyilanxin.kunpeng.broker.client.business.client.ExpressionClient;
import com.anyilanxin.kunpeng.broker.client.business.client.IncidentClient;
import com.anyilanxin.kunpeng.broker.client.business.client.JobClient;
import com.anyilanxin.kunpeng.broker.client.business.client.MessageClient;
import com.anyilanxin.kunpeng.broker.client.business.client.ProcessDefinitionClient;
import com.anyilanxin.kunpeng.broker.client.business.client.ProcessInstanceClient;
import com.anyilanxin.kunpeng.broker.client.business.client.SignalClient;
import com.anyilanxin.kunpeng.broker.client.business.client.UserTaskClient;
import com.anyilanxin.kunpeng.broker.client.business.client.VariableClient;
import com.anyilanxin.kunpeng.broker.client.core.BrokerClient;

/**
 * 业务命令客户端聚合门面默认实现：包一个 {@link BrokerClient}，按域构建各命令客户端。
 *
 * @author zxuanhong
 * @since 2026.9.0
 */
public class DefaultBusinessCommandClient implements BusinessCommandClient {
  private final DeploymentClient deployment;
  private final ProcessDefinitionClient processDefinition;
  private final ProcessInstanceClient processInstance;
  private final JobClient job;
  private final UserTaskClient userTask;
  private final IncidentClient incident;
  private final MessageClient message;
  private final SignalClient signal;
  private final VariableClient variable;
  private final ExpressionClient expression;

  public DefaultBusinessCommandClient(final BrokerClient brokerClient) {
    this.deployment = new DefaultDeploymentClient(brokerClient);
    this.processDefinition = new DefaultProcessDefinitionClient(brokerClient);
    this.processInstance = new DefaultProcessInstanceClient(brokerClient);
    this.job = new DefaultJobClient(brokerClient);
    this.userTask = new DefaultUserTaskClient(brokerClient);
    this.incident = new DefaultIncidentClient(brokerClient);
    this.message = new DefaultMessageClient(brokerClient);
    this.signal = new DefaultSignalClient(brokerClient);
    this.variable = new DefaultVariableClient(brokerClient);
    this.expression = new DefaultExpressionClient(brokerClient);
  }

  @Override
  public DeploymentClient deployment() {
    return deployment;
  }

  @Override
  public ProcessDefinitionClient processDefinition() {
    return processDefinition;
  }

  @Override
  public ProcessInstanceClient processInstance() {
    return processInstance;
  }

  @Override
  public JobClient job() {
    return job;
  }

  @Override
  public UserTaskClient userTask() {
    return userTask;
  }

  @Override
  public IncidentClient incident() {
    return incident;
  }

  @Override
  public MessageClient message() {
    return message;
  }

  @Override
  public SignalClient signal() {
    return signal;
  }

  @Override
  public VariableClient variable() {
    return variable;
  }

  @Override
  public ExpressionClient expression() {
    return expression;
  }
}
