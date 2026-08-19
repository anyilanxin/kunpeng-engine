/*
 * Copyright 2017-present Open Networking Foundation
 * Copyright © 2020 camunda services GmbH (info@camunda.com)
 * Copyright © 2026 anyilanxin zxh(anyilanxin@aliyun.com)
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

import com.google.common.base.Preconditions;
import io.micrometer.core.instrument.MeterRegistry;
import com.anyilanxin.kunpeng.cluster.cluster.MemberId;
import com.anyilanxin.kunpeng.cluster.cluster.messaging.ClusterCommunicationService;
import com.anyilanxin.kunpeng.cluster.raft.metrics.RaftRequestMetrics;
import com.anyilanxin.kunpeng.cluster.raft.protocol.AppendRequest;
import com.anyilanxin.kunpeng.cluster.raft.protocol.AppendResponse;
import com.anyilanxin.kunpeng.cluster.raft.protocol.ConfigureRequest;
import com.anyilanxin.kunpeng.cluster.raft.protocol.ConfigureResponse;
import com.anyilanxin.kunpeng.cluster.raft.protocol.InstallRequest;
import com.anyilanxin.kunpeng.cluster.raft.protocol.InstallResponse;
import com.anyilanxin.kunpeng.cluster.raft.protocol.PollRequest;
import com.anyilanxin.kunpeng.cluster.raft.protocol.PollResponse;
import com.anyilanxin.kunpeng.cluster.raft.protocol.RaftMessage;
import com.anyilanxin.kunpeng.cluster.raft.protocol.RaftServerProtocol;
import com.anyilanxin.kunpeng.cluster.raft.protocol.ReconfigureRequest;
import com.anyilanxin.kunpeng.cluster.raft.protocol.ReconfigureResponse;
import com.anyilanxin.kunpeng.cluster.raft.protocol.TransferRequest;
import com.anyilanxin.kunpeng.cluster.raft.protocol.TransferResponse;
import com.anyilanxin.kunpeng.cluster.raft.protocol.VoteRequest;
import com.anyilanxin.kunpeng.cluster.raft.protocol.VoteResponse;
import com.anyilanxin.kunpeng.cluster.utils.serializer.Serializer;
import java.time.Duration;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;

/** Raft server protocol that uses a {@link ClusterCommunicationService}. */
public class RaftServerCommunicator implements RaftServerProtocol {

  private final RaftMessageContext context;
  private final Serializer serializer;
  private final ClusterCommunicationService clusterCommunicator;
  private final String partitionName;
  private final RaftRequestMetrics metrics;
  private final Duration requestTimeout;

  public RaftServerCommunicator(
      final String prefix,
      final MeterRegistry meterRegistry,
      final Serializer serializer,
      final ClusterCommunicationService clusterCommunicator,
      final Duration requestTimeout) {
    context = new RaftMessageContext(prefix);
    partitionName = prefix;
    this.serializer = Preconditions.checkNotNull(serializer, "serializer cannot be null");
    this.clusterCommunicator =
        Preconditions.checkNotNull(clusterCommunicator, "clusterCommunicator cannot be null");
    this.requestTimeout = requestTimeout;
    metrics = new RaftRequestMetrics(partitionName, meterRegistry);
  }

  @Override
  public CompletableFuture<ConfigureResponse> configure(
      final MemberId memberId, final ConfigureRequest request) {
    return sendAndReceive(context.configureSubject, request, memberId);
  }

  @Override
  public CompletableFuture<ReconfigureResponse> reconfigure(
      final MemberId memberId, final ReconfigureRequest request) {
    return sendAndReceive(context.reconfigureSubject, request, memberId);
  }

  @Override
  public CompletableFuture<InstallResponse> install(
      final MemberId memberId, final InstallRequest request) {
    return sendAndReceive(context.installSubject, request, memberId);
  }

  @Override
  public CompletableFuture<TransferResponse> transfer(
      final MemberId memberId, final TransferRequest request) {
    return sendAndReceive(context.transferSubject, request, memberId);
  }

  @Override
  public CompletableFuture<PollResponse> poll(final MemberId memberId, final PollRequest request) {
    return sendAndReceive(context.pollSubject, request, memberId);
  }

  @Override
  public CompletableFuture<VoteResponse> vote(final MemberId memberId, final VoteRequest request) {
    return sendAndReceive(context.voteSubject, request, memberId);
  }

  @Override
  public CompletableFuture<AppendResponse> append(
      final MemberId memberId, final AppendRequest request) {
    return sendAndReceive(context.appendSubject, request, memberId);
  }

  @Override
  public void registerTransferHandler(
      final Function<TransferRequest, CompletableFuture<TransferResponse>> handler) {
    clusterCommunicator.subscribe(
        context.transferSubject,
        serializer::decode,
        handler.<TransferRequest>compose(this::recordReceivedMetrics),
        serializer::encode);
  }

  @Override
  public void unregisterTransferHandler() {
    clusterCommunicator.unsubscribe(context.transferSubject);
  }

  @Override
  public void registerConfigureHandler(
      final Function<ConfigureRequest, CompletableFuture<ConfigureResponse>> handler) {
    clusterCommunicator.subscribe(
        context.configureSubject,
        serializer::decode,
        handler.<ConfigureRequest>compose(this::recordReceivedMetrics),
        serializer::encode);
  }

  @Override
  public void unregisterConfigureHandler() {
    clusterCommunicator.unsubscribe(context.configureSubject);
  }

  @Override
  public void registerReconfigureHandler(
      final Function<ReconfigureRequest, CompletableFuture<ReconfigureResponse>> handler) {
    clusterCommunicator.subscribe(
        context.reconfigureSubject,
        serializer::decode,
        handler.<ReconfigureRequest>compose(this::recordReceivedMetrics),
        serializer::encode);
  }

  @Override
  public void unregisterReconfigureHandler() {
    clusterCommunicator.unsubscribe(context.reconfigureSubject);
  }

  @Override
  public void registerInstallHandler(
      final Function<InstallRequest, CompletableFuture<InstallResponse>> handler) {
    clusterCommunicator.subscribe(
        context.installSubject,
        serializer::decode,
        handler.<InstallRequest>compose(this::recordReceivedMetrics),
        serializer::encode);
  }

  @Override
  public void unregisterInstallHandler() {
    clusterCommunicator.unsubscribe(context.installSubject);
  }

  @Override
  public void registerPollHandler(
      final Function<PollRequest, CompletableFuture<PollResponse>> handler) {
    clusterCommunicator.subscribe(
        context.pollSubject,
        serializer::decode,
        handler.<PollRequest>compose(this::recordReceivedMetrics),
        serializer::encode);
  }

  @Override
  public void unregisterPollHandler() {
    clusterCommunicator.unsubscribe(context.pollSubject);
  }

  @Override
  public void registerVoteHandler(
      final Function<VoteRequest, CompletableFuture<VoteResponse>> handler) {
    clusterCommunicator.subscribe(
        context.voteSubject,
        serializer::decode,
        handler.<VoteRequest>compose(this::recordReceivedMetrics),
        serializer::encode);
  }

  @Override
  public void unregisterVoteHandler() {
    clusterCommunicator.unsubscribe(context.voteSubject);
  }

  @Override
  public void registerAppendHandler(
      final Function<AppendRequest, CompletableFuture<AppendResponse>> handler) {
    clusterCommunicator.subscribe(
        context.appendSubject,
        serializer::decode,
        handler.<AppendRequest>compose(this::recordReceivedMetrics),
        serializer::encode);
  }

  @Override
  public void unregisterAppendHandler() {
    clusterCommunicator.unsubscribe(context.appendSubject);
  }

  private <T, U> CompletableFuture<U> sendAndReceive(
      final String subject, final T request, final MemberId memberId) {
    metrics.sendMessage(memberId.id(), request.getClass().getSimpleName());
    return clusterCommunicator.send(
        subject,
        request,
        serializer::encode,
        serializer::decode,
        MemberId.from(memberId.id()),
        requestTimeout);
  }

  private <T extends RaftMessage> T recordReceivedMetrics(final T m) {
    metrics.receivedMessage(m.getClass().getSimpleName());
    return m;
  }
}
