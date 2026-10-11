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
package com.anyilanxin.kunpeng.broker.client.business.client;

/**
 * 业务命令客户端聚合门面：按域持有命令客户端，各域命令的请求/响应记录类型在域客户端内完全确定， 调用方（gateway gRPC/REST 等）不接触 {@code
 * BrokerClient.sendRequest} 的泛型推断。
 *
 * @author zxuanhong
 * @since 2026.9.0
 */
public interface BusinessCommandClient {
  /** 部署域 */
  DeploymentClient deployment();

  /** 流程定义域 */
  ProcessDefinitionClient processDefinition();

  /** 流程实例域 */
  ProcessInstanceClient processInstance();

  /** job 域 */
  JobClient job();

  /** 用户任务域 */
  UserTaskClient userTask();

  /** 事件域 */
  IncidentClient incident();

  /** 消息域 */
  MessageClient message();

  /** 信号域 */
  SignalClient signal();

  /** 变量域 */
  VariableClient variable();

  /** 表达式域 */
  ExpressionClient expression();
}
