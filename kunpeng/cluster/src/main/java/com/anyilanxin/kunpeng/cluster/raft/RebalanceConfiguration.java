/*
 * Copyright © 2020 camunda services GmbH (info@camunda.com)
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
package com.anyilanxin.kunpeng.cluster.raft;

import java.time.Duration;

/**
 * Rebalance 相关配置参数。
 *
 * @param replicationLagThreshold 允许的最大复制延迟（条目数），超过此值的目标节点不参与转移
 * @param replicationTimeout 等待目标节点追上的超时
 * @param maxTransferAttempts 放弃前的最大尝试次数
 */
public record RebalanceConfiguration(
    long replicationLagThreshold, Duration replicationTimeout, int maxTransferAttempts) {

  /** 默认配置 */
  public static RebalanceConfiguration ofDefault() {
    return new RebalanceConfiguration(1000, Duration.ofSeconds(30), 3);
  }
}
