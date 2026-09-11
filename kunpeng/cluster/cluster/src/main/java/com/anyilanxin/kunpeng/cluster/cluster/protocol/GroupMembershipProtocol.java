/*
 * Copyright 2018-present Open Networking Foundation
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
package com.anyilanxin.kunpeng.cluster.cluster.protocol;

import com.anyilanxin.kunpeng.cluster.cluster.BootstrapService;
import com.anyilanxin.kunpeng.cluster.cluster.Member;
import com.anyilanxin.kunpeng.cluster.cluster.MemberId;
import com.anyilanxin.kunpeng.cluster.cluster.discovery.NodeDiscoveryService;
import com.anyilanxin.kunpeng.cluster.utils.NamedType;
import com.anyilanxin.kunpeng.cluster.utils.config.Configured;
import com.anyilanxin.kunpeng.cluster.utils.event.ListenerService;
import io.micrometer.core.instrument.MeterRegistry;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

/** Group membership protocol. */
public interface GroupMembershipProtocol
    extends ListenerService<GroupMembershipEvent, GroupMembershipEventListener>,
        Configured<GroupMembershipProtocolConfig> {

  /**
   * Returns the set of current cluster members.
   *
   * @return set of cluster members
   */
  Set<Member> getMembers();

  /**
   * Returns the specified member.
   *
   * @param memberId the member identifier
   * @return the member or {@code null} if no node with the given identifier exists
   */
  Member getMember(MemberId memberId);

  /**
   * Joins the cluster.
   *
   * @param bootstrap the bootstrap service
   * @param discovery the discovery service
   * @param localMember the local member info
   * @return a future to be completed once the join is complete
   */
  CompletableFuture<Void> join(
      BootstrapService bootstrap, NodeDiscoveryService discovery, Member localMember);

  /**
   * Leaves the cluster.
   *
   * @param localMember the local member info
   * @return a future to be completed once the leave is complete
   */
  CompletableFuture<Void> leave(Member localMember);

  /** Group membership protocol type. */
  interface Type<C extends GroupMembershipProtocolConfig> extends NamedType {

    /**
     * Creates a new instance of the protocol.
     *
     * @param config the protocol configuration
     * @param actorSchedulerName the value for actor-scheduler to be set on MDC
     * @return the protocol instance
     */
    GroupMembershipProtocol newProtocol(
        C config, final String actorSchedulerName, final MeterRegistry registry);
  }
}
