/*
 * Copyright 2016-present Open Networking Foundation
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
 * limitations under the License
 */
package com.anyilanxin.kunpeng.cluster.raft.roles;

import com.anyilanxin.kunpeng.cluster.raft.RaftError;
import com.anyilanxin.kunpeng.cluster.raft.RaftServer;
import com.anyilanxin.kunpeng.cluster.raft.protocol.AppendResponse;
import com.anyilanxin.kunpeng.cluster.raft.protocol.ConfigureRequest;
import com.anyilanxin.kunpeng.cluster.raft.protocol.ConfigureResponse;
import com.anyilanxin.kunpeng.cluster.raft.protocol.ForceConfigureRequest;
import com.anyilanxin.kunpeng.cluster.raft.protocol.ForceConfigureResponse;
import com.anyilanxin.kunpeng.cluster.raft.protocol.InstallRequest;
import com.anyilanxin.kunpeng.cluster.raft.protocol.InstallResponse;
import com.anyilanxin.kunpeng.cluster.raft.protocol.InternalAppendRequest;
import com.anyilanxin.kunpeng.cluster.raft.protocol.JoinRequest;
import com.anyilanxin.kunpeng.cluster.raft.protocol.JoinResponse;
import com.anyilanxin.kunpeng.cluster.raft.protocol.LeadershipTransferInitiateRequest;
import com.anyilanxin.kunpeng.cluster.raft.protocol.LeadershipTransferInitiateResponse;
import com.anyilanxin.kunpeng.cluster.raft.protocol.LeaveRequest;
import com.anyilanxin.kunpeng.cluster.raft.protocol.LeaveResponse;
import com.anyilanxin.kunpeng.cluster.raft.protocol.PollRequest;
import com.anyilanxin.kunpeng.cluster.raft.protocol.PollResponse;
import com.anyilanxin.kunpeng.cluster.raft.protocol.RaftRequest;
import com.anyilanxin.kunpeng.cluster.raft.protocol.ReconfigureRequest;
import com.anyilanxin.kunpeng.cluster.raft.protocol.ReconfigureResponse;
import com.anyilanxin.kunpeng.cluster.raft.protocol.TimeoutNowRequest;
import com.anyilanxin.kunpeng.cluster.raft.protocol.TimeoutNowResponse;
import com.anyilanxin.kunpeng.cluster.raft.protocol.TransferRequest;
import com.anyilanxin.kunpeng.cluster.raft.protocol.TransferResponse;
import com.anyilanxin.kunpeng.cluster.raft.protocol.VoteRequest;
import com.anyilanxin.kunpeng.cluster.raft.protocol.VoteResponse;
import com.anyilanxin.kunpeng.cluster.utils.Managed;
import com.anyilanxin.kunpeng.utils.Either;
import java.util.concurrent.CompletableFuture;

/** Raft role interface. */
public interface RaftRole extends Managed<RaftRole> {

  /**
   * Returns the server state type.
   *
   * @return The server state type.
   */
  RaftServer.Role role();

  /**
   * Handles a configure request.
   *
   * @param request The request to handle.
   * @return A completable future to be completed with the request response.
   */
  CompletableFuture<ConfigureResponse> onConfigure(ConfigureRequest request);

  /**
   * Handles an install request.
   *
   * @param request The request to handle.
   * @return A completable future to be completed with the request response.
   */
  CompletableFuture<InstallResponse> onInstall(InstallRequest request);

  /**
   * Handles a configure request.
   *
   * @param request The request to handle.
   * @return A completable future to be completed with the request response.
   */
  CompletableFuture<ReconfigureResponse> onReconfigure(ReconfigureRequest request);

  /**
   * Handles a force configure request. The request is never received from a leader. This is
   * typically requested to remove a set of (unavailable) members when a quorum is not possible.
   */
  CompletableFuture<ForceConfigureResponse> onForceConfigure(ForceConfigureRequest request);

  /** Handles a request to join the cluster. */
  CompletableFuture<JoinResponse> onJoin(JoinRequest request);

  /** Handles a request to leave the cluster. */
  CompletableFuture<LeaveResponse> onLeave(LeaveRequest request);

  /**
   * Handles a transfer request.
   *
   * @param request The request to handle.
   * @return A completable future to be completed with the request response.
   */
  CompletableFuture<TransferResponse> onTransfer(TransferRequest request);

  /**
   * Handles a timeout-now request: instructs this node to start an election immediately, bypassing
   * its election timeout. Only a follower (the intended successor) accepts it; all other roles
   * reject it.
   *
   * @param request The request to handle.
   * @return A completable future to be completed with the request response.
   */
  CompletableFuture<TimeoutNowResponse> onTimeoutNow(TimeoutNowRequest request);

  /**
   * Handles a coordinated-leadership-transfer initiate request. Only the leader drives the
   * transfer; every other role rejects it as misrouted.
   *
   * @param request The request to handle.
   * @return A completable future to be completed with the acknowledgement.
   */
  CompletableFuture<LeadershipTransferInitiateResponse> onLeadershipTransferInitiate(
      LeadershipTransferInitiateRequest request);

  /**
   * Handles an append request.
   *
   * @param request The request to handle.
   * @return A completable future to be completed with the request response.
   */
  CompletableFuture<AppendResponse> onAppend(InternalAppendRequest request);

  /**
   * Handles a poll request.
   *
   * @param request The request to handle.
   * @return A completable future to be completed with the request response.
   */
  CompletableFuture<PollResponse> onPoll(PollRequest request);

  /**
   * Handles a vote request.
   *
   * @param request The request to handle.
   * @return A completable future to be completed with the request response.
   */
  CompletableFuture<VoteResponse> onVote(VoteRequest request);

  Either<RaftError, Void> shouldAcceptRequest(RaftRequest request);
}
