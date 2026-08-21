/*
 * Copyright © 2020 camunda services GmbH (info@camunda.com)
 * Copyright © 2026 anyilanxin zxh (anyilanxin@aliyun.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.anyilanxin.kunpeng.cluster.raft.partition.impl;

import static com.anyilanxin.kunpeng.cluster.raft.partition.RaftPartition.PARTITION_NAME_FORMAT;

import com.anyilanxin.kunpeng.cluster.cluster.MemberId;
import com.anyilanxin.kunpeng.cluster.cluster.messaging.ClusterCommunicationService;
import com.anyilanxin.kunpeng.cluster.raft.protocol.LeadershipTransferInitiateRequest;
import com.anyilanxin.kunpeng.cluster.raft.protocol.LeadershipTransferInitiateResponse;
import com.anyilanxin.kunpeng.cluster.raft.protocol.LeadershipTransferResultRequest;
import com.anyilanxin.kunpeng.cluster.raft.protocol.LeadershipTransferResultResponse;
import com.anyilanxin.kunpeng.cluster.utils.serializer.Serializer;
import java.time.Duration;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.function.Function;
import org.jspecify.annotations.NullMarked;

/**
 * Allows the rebalancing coordinator to address the leadership transfer protocol from (potentially)
 * outside the Raft group of the partition.
 */
@NullMarked
public final class LeadershipTransferClient implements AutoCloseable {

  private final ClusterCommunicationService communicationService;
  private final Duration requestTimeout;
  private final Serializer serializer = Serializer.using(RaftNamespaces.RAFT_PROTOCOL);
  private final Set<String> subscriptions = new CopyOnWriteArraySet<>();

  public LeadershipTransferClient(
      final ClusterCommunicationService communicationService, final Duration requestTimeout) {
    this.communicationService = communicationService;
    this.requestTimeout = requestTimeout;
  }

  /**
   * Asks the current leader of the given partition to transfer its leadership. The response only
   * says whether the transfer was accepted or skipped (accepted transfers report their outcome
   * through {@link #onResult}.
   */
  public CompletableFuture<LeadershipTransferInitiateResponse> initiate(
      final MemberId leader,
      final String partitionGroup,
      final int partitionId,
      final LeadershipTransferInitiateRequest request) {
    return communicationService.send(
        subjects(partitionGroup, partitionId).getLeadershipTransferInitiateSubject(),
        request,
        serializer::encode,
        serializer::decode,
        leader,
        requestTimeout);
  }

  /**
   * Handles the terminal outcome the given partition's leader reports back. The result is
   * correlated to the initiate request by {@link LeadershipTransferResultRequest#correlationId()}.
   */
  public void onResult(
      final String partitionGroup,
      final int partitionId,
      final Function<
              LeadershipTransferResultRequest, CompletableFuture<LeadershipTransferResultResponse>>
          handler) {
    final var subject = subjects(partitionGroup, partitionId).getLeadershipTransferResultSubject();
    subscriptions.add(subject);
    communicationService.replyTo(subject, serializer::decode, handler, serializer::encode);
  }

  @Override
  public void close() {
    subscriptions.forEach(communicationService::unsubscribe);
    subscriptions.clear();
  }

  private RaftMessageContext subjects(final String partitionGroup, final int partitionId) {
    return new RaftMessageContext(PARTITION_NAME_FORMAT.formatted(partitionGroup, partitionId));
  }
}
