/*
 * Copyright 2015-present Open Networking Foundation
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
package com.anyilanxin.kunpeng.cluster.raft.roles;

import com.anyilanxin.kunpeng.cluster.raft.RaftError;
import com.anyilanxin.kunpeng.cluster.raft.RaftServer;
import com.anyilanxin.kunpeng.cluster.raft.impl.RaftContext;
import com.anyilanxin.kunpeng.cluster.raft.protocol.AppendRequest;
import com.anyilanxin.kunpeng.cluster.raft.protocol.AppendResponse;
import com.anyilanxin.kunpeng.cluster.raft.protocol.ConfigureRequest;
import com.anyilanxin.kunpeng.cluster.raft.protocol.ConfigureResponse;
import com.anyilanxin.kunpeng.cluster.raft.protocol.InstallRequest;
import com.anyilanxin.kunpeng.cluster.raft.protocol.InstallResponse;
import com.anyilanxin.kunpeng.cluster.raft.protocol.PollRequest;
import com.anyilanxin.kunpeng.cluster.raft.protocol.PollResponse;
import com.anyilanxin.kunpeng.cluster.raft.protocol.RaftResponse;
import com.anyilanxin.kunpeng.cluster.raft.protocol.RaftResponse.Status;
import com.anyilanxin.kunpeng.cluster.raft.protocol.ReconfigureRequest;
import com.anyilanxin.kunpeng.cluster.raft.protocol.ReconfigureResponse;
import com.anyilanxin.kunpeng.cluster.raft.protocol.TransferRequest;
import com.anyilanxin.kunpeng.cluster.raft.protocol.TransferResponse;
import com.anyilanxin.kunpeng.cluster.raft.protocol.VoteRequest;
import com.anyilanxin.kunpeng.cluster.raft.protocol.VoteResponse;
import com.anyilanxin.kunpeng.cluster.raft.storage.system.Configuration;
import com.anyilanxin.kunpeng.cluster.utils.concurrent.Futures;
import java.util.concurrent.CompletableFuture;

/** Inactive state. */
public class InactiveRole extends AbstractRole {

  public InactiveRole(final RaftContext context) {
    super(context);
  }

  @Override
  public RaftServer.Role role() {
    return RaftServer.Role.INACTIVE;
  }

  @Override
  public CompletableFuture<ConfigureResponse> onConfigure(final ConfigureRequest request) {
    raft.checkThread();
    logRequest(request);
    updateTermAndLeader(request.term(), request.leader());

    final Configuration configuration =
        new Configuration(request.index(), request.term(), request.timestamp(), request.members());

    // Configure the cluster membership. This will cause this server to transition to the
    // appropriate state if its type has changed.
    raft.getCluster().configure(configuration);

    // If the configuration is already committed, commit it to disk.
    // Check against the actual cluster Configuration rather than the received configuration in
    // case the received configuration was an older configuration that was not applied.
    if (raft.getCommitIndex() >= raft.getCluster().getConfiguration().index()) {
      raft.getCluster().commit();
    }

    return CompletableFuture.completedFuture(
        logResponse(ConfigureResponse.builder().withStatus(RaftResponse.Status.OK).build()));
  }

  @Override
  public CompletableFuture<InstallResponse> onInstall(final InstallRequest request) {
    logRequest(request);
    return Futures.completedFuture(
        logResponse(
            InstallResponse.builder()
                .withStatus(Status.ERROR)
                .withError(RaftError.Type.UNAVAILABLE)
                .build()));
  }

  @Override
  public CompletableFuture<ReconfigureResponse> onReconfigure(final ReconfigureRequest request) {
    logRequest(request);
    return Futures.completedFuture(
        logResponse(
            ReconfigureResponse.builder()
                .withStatus(Status.ERROR)
                .withError(RaftError.Type.UNAVAILABLE)
                .build()));
  }

  @Override
  public CompletableFuture<TransferResponse> onTransfer(final TransferRequest request) {
    logRequest(request);
    return Futures.completedFuture(
        logResponse(
            TransferResponse.builder()
                .withStatus(Status.ERROR)
                .withError(RaftError.Type.UNAVAILABLE)
                .build()));
  }

  @Override
  public CompletableFuture<AppendResponse> onAppend(final AppendRequest request) {
    logRequest(request);
    return Futures.completedFuture(
        logResponse(
            AppendResponse.builder()
                .withStatus(Status.ERROR)
                .withError(RaftError.Type.UNAVAILABLE)
                .build()));
  }

  @Override
  public CompletableFuture<PollResponse> onPoll(final PollRequest request) {
    logRequest(request);
    return Futures.completedFuture(
        logResponse(
            PollResponse.builder()
                .withStatus(Status.ERROR)
                .withError(RaftError.Type.UNAVAILABLE)
                .build()));
  }

  @Override
  public CompletableFuture<VoteResponse> onVote(final VoteRequest request) {
    logRequest(request);
    return Futures.completedFuture(
        logResponse(
            VoteResponse.builder()
                .withStatus(Status.ERROR)
                .withError(RaftError.Type.UNAVAILABLE)
                .build()));
  }
}
