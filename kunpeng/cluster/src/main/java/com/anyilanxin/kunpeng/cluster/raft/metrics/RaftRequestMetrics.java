/*
 * Copyright 2016-present Open Networking Foundation
 * Copyright © 2020 camunda services GmbH (info@camunda.com)
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
package com.anyilanxin.kunpeng.cluster.raft.metrics;

import static com.anyilanxin.kunpeng.cluster.raft.metrics.RaftRequestMetricsDoc.RAFT_MESSAGE_RECEIVED;
import static com.anyilanxin.kunpeng.cluster.raft.metrics.RaftRequestMetricsDoc.RAFT_MESSAGE_SEND;

import com.anyilanxin.kunpeng.utils.micrometer.Micrometers;
import io.micrometer.core.instrument.MeterRegistry;

/** Raft 协议消息收发相关指标采集 */
public class RaftRequestMetrics extends RaftMetrics {

  private final MeterRegistry registry;

  public RaftRequestMetrics(final String partitionName, final MeterRegistry registry) {
    super(partitionName);
    this.registry = registry;
  }

  /** 记录收到一条 Raft 消息 */
  public void receivedMessage(final String type) {
    Micrometers.counter(
            RAFT_MESSAGE_RECEIVED,
            registry,
            "type",
            type,
            "partitionGroupName",
            partitionGroupName)
        .increment();
  }

  /** 记录发出一条 Raft 消息 */
  public void sendMessage(final String memberId, final String type) {
    Micrometers.counter(
            RAFT_MESSAGE_SEND,
            registry,
            "to",
            memberId,
            "type",
            type,
            "partitionGroupName",
            partitionGroupName)
        .increment();
  }
}
