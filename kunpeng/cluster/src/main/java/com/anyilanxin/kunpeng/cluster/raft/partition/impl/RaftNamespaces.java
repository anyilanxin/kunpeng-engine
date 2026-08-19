/*
 * Copyright 2017-present Open Networking Foundation
 * Copyright © 2020 camunda services GmbH (info@camunda.com)
 * Copyright © 2026 anyilanxin zxh(anyilanxin@aliyun.com)
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
package com.anyilanxin.kunpeng.cluster.raft.partition.impl;

import com.anyilanxin.kunpeng.cluster.cluster.MemberId;
import com.anyilanxin.kunpeng.cluster.raft.RaftError;
import com.anyilanxin.kunpeng.cluster.raft.cluster.RaftMember;
import com.anyilanxin.kunpeng.cluster.raft.cluster.impl.DefaultRaftMember;
import com.anyilanxin.kunpeng.cluster.raft.protocol.AppendRequest;
import com.anyilanxin.kunpeng.cluster.raft.protocol.AppendResponse;
import com.anyilanxin.kunpeng.cluster.raft.protocol.ConfigureRequest;
import com.anyilanxin.kunpeng.cluster.raft.protocol.ConfigureResponse;
import com.anyilanxin.kunpeng.cluster.raft.protocol.InstallRequest;
import com.anyilanxin.kunpeng.cluster.raft.protocol.InstallResponse;
import com.anyilanxin.kunpeng.cluster.raft.protocol.PollRequest;
import com.anyilanxin.kunpeng.cluster.raft.protocol.PollResponse;
import com.anyilanxin.kunpeng.cluster.raft.protocol.RaftResponse;
import com.anyilanxin.kunpeng.cluster.raft.protocol.ReconfigureRequest;
import com.anyilanxin.kunpeng.cluster.raft.protocol.ReconfigureResponse;
import com.anyilanxin.kunpeng.cluster.raft.protocol.TransferRequest;
import com.anyilanxin.kunpeng.cluster.raft.protocol.TransferResponse;
import com.anyilanxin.kunpeng.cluster.raft.protocol.VoteRequest;
import com.anyilanxin.kunpeng.cluster.raft.protocol.VoteResponse;
import com.anyilanxin.kunpeng.cluster.raft.storage.log.PersistedRaftRecord;
import com.anyilanxin.kunpeng.cluster.raft.storage.log.entry.ApplicationEntry;
import com.anyilanxin.kunpeng.cluster.raft.storage.log.entry.ConfigurationEntry;
import com.anyilanxin.kunpeng.cluster.raft.storage.log.entry.InitialEntry;
import com.anyilanxin.kunpeng.cluster.raft.storage.log.entry.RaftLogEntry;
import com.anyilanxin.kunpeng.cluster.raft.storage.system.Configuration;
import com.anyilanxin.kunpeng.cluster.utils.serializer.Namespace;
import com.anyilanxin.kunpeng.cluster.utils.serializer.Namespace.Builder;
import com.anyilanxin.kunpeng.cluster.utils.serializer.Namespaces;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedList;

/** Storage serializer namespaces. */
public final class RaftNamespaces {

  /** Raft protocol namespace. */
  public static final Namespace RAFT_PROTOCOL =
      new Builder()
          .register(Namespaces.BASIC)
          .nextId(Namespaces.BEGIN_USER_CUSTOM_ID)
          .register(ConfigureRequest.class)
          .register(ConfigureResponse.class)
          .register(ReconfigureRequest.class)
          .register(ReconfigureResponse.class)
          .register(InstallRequest.class)
          .register(InstallResponse.class)
          .register(PollRequest.class)
          .register(PollResponse.class)
          .register(VoteRequest.class)
          .register(VoteResponse.class)
          .register(AppendRequest.class)
          .register(AppendResponse.class)
          .register(RaftResponse.Status.class)
          .register(RaftError.class)
          .register(RaftError.Type.class)
          .register(ConfigurationEntry.class)
          .register(InitialEntry.class)
          .register(ArrayList.class)
          .register(LinkedList.class)
          .register(Collections.emptyList().getClass())
          .register(HashSet.class)
          .register(DefaultRaftMember.class)
          .register(MemberId.class)
          .register(RaftMember.Type.class)
          .register(Instant.class)
          .register(Configuration.class)
          .register(ApplicationEntry.class)
          .register(RaftLogEntry.class)
          .register(PersistedRaftRecord.class)
          .register(TransferRequest.class)
          .register(TransferResponse.class)
          .name("RaftProtocol")
          .build();

  private RaftNamespaces() {}
}
