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
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package com.anyilanxin.kunpeng.cluster.config.topology;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.anyilanxin.kunpeng.cluster.cluster.ClusterMembershipEvent;
import com.anyilanxin.kunpeng.cluster.cluster.ClusterMembershipService;
import com.anyilanxin.kunpeng.cluster.cluster.Member;
import com.anyilanxin.kunpeng.cluster.cluster.MemberId;
import com.anyilanxin.kunpeng.cluster.cluster.PartitionId;
import com.anyilanxin.kunpeng.cluster.config.topology.cluster.DefaultClusterTopologyService;
import com.anyilanxin.kunpeng.cluster.utils.net.Address;
import com.anyilanxin.kunpeng.protocol.common.encoding.BrokerInfo;
import com.anyilanxin.kunpeng.protocol.common.member.CommPortType;
import com.anyilanxin.kunpeng.scheduler.ActorScheduler;
import java.util.Base64;
import java.util.Properties;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

/**
 * {@link DefaultClusterTopologyService} 多成员广播汇聚视图测试。
 *
 * @author zxuanhong
 * @since 2026.9.0
 */
class DefaultClusterTopologyServiceTest {

  private static final PartitionId PARTITION = PartitionId.from("raft-partition", 1);

  private ActorScheduler scheduler;
  private ClusterMembershipService membershipService;
  private Member localMember;

  @AfterEach
  void tearDown() {
    if (scheduler != null) {
      scheduler.close();
    }
  }

  @Test
  void shouldRetainAllMembersPerPartitionAndFindLeader() {
    final DefaultClusterTopologyService service = startedService();
    final Member leader = memberBroadcast("member-1", PartitionRole.LEADER, 2L);
    final Member follower = memberBroadcast("member-2", PartitionRole.FOLLOWER, 2L);

    service.event(new ClusterMembershipEvent(ClusterMembershipEvent.Type.MEMBER_ADDED, leader));
    service.event(new ClusterMembershipEvent(ClusterMembershipEvent.Type.MEMBER_ADDED, follower));
    awaitEventsProcessed(service);

    // 分区维度需保留全部成员状态（跨成员不得互相去重）
    assertThat(service.getPartitionMemberInfo(PARTITION)).hasSize(2);
    assertThat(service.getRaftGroup("raft-partition").get(PARTITION)).hasSize(2);
    assertThat(service.getPartitionLeader(PARTITION)).isEqualTo(MemberId.from("member-1"));
  }

  @Test
  void shouldPreferHighestTermLeaderClaim() {
    final DefaultClusterTopologyService service = startedService();
    // 选举窗口期：旧 leader 尚未退位广播（term 1），新 leader 已当选（term 2）
    service.event(
        new ClusterMembershipEvent(
            ClusterMembershipEvent.Type.MEMBER_ADDED,
            memberBroadcast("member-1", PartitionRole.LEADER, 1L)));
    service.event(
        new ClusterMembershipEvent(
            ClusterMembershipEvent.Type.MEMBER_ADDED,
            memberBroadcast("member-2", PartitionRole.LEADER, 2L)));
    awaitEventsProcessed(service);

    assertThat(service.getPartitionLeader(PARTITION)).isEqualTo(MemberId.from("member-2"));
  }

  @Test
  void shouldDropLeaderWhenLeaderMemberRemoved() {
    final DefaultClusterTopologyService service = startedService();
    final Member leader = memberBroadcast("member-1", PartitionRole.LEADER, 2L);
    final Member follower = memberBroadcast("member-2", PartitionRole.FOLLOWER, 2L);
    service.event(new ClusterMembershipEvent(ClusterMembershipEvent.Type.MEMBER_ADDED, leader));
    service.event(new ClusterMembershipEvent(ClusterMembershipEvent.Type.MEMBER_ADDED, follower));

    service.event(
        new ClusterMembershipEvent(ClusterMembershipEvent.Type.MEMBER_REMOVED, leader));
    awaitEventsProcessed(service);

    assertThat(service.getPartitionLeader(PARTITION)).isNull();
    assertThat(service.getPartitionMemberInfo(PARTITION)).hasSize(1);
  }

  @Test
  void shouldReturnNullForUnknownPartition() {
    assertThat(service().getPartitionLeader(PartitionId.from("unknown", 9))).isNull();
    assertThat(service().getPartitionMemberInfo(PartitionId.from("unknown", 9))).isEmpty();
    assertThat(service().getPartitionBusinessAddress(PartitionId.from("unknown", 9))).isNull();
  }

  @Test
  void shouldResolveRemoteLeaderBusinessAddressFromCollectedPorts() {
    final DefaultClusterTopologyService service = startedService();
    final Member leader =
        memberBroadcast("member-1", PartitionRole.LEADER, 2L, "10.0.0.2", 4567);
    service.event(new ClusterMembershipEvent(ClusterMembershipEvent.Type.MEMBER_ADDED, leader));
    awaitEventsProcessed(service);

    assertThat(service.getPartitionBusinessAddress(PARTITION))
        .isEqualTo(Address.from("10.0.0.2", 4567));
  }

  @Test
  void shouldResolveLocalLeaderBusinessAddressFromEventView() {
    final DefaultClusterTopologyService service = startedService();
    // 本节点与远端走同一事件通道（协议对本地属性变更同样 post 快照事件），视图统一存解析结果，查询无特殊分支
    when(localMember.address()).thenReturn(Address.from("10.0.0.1", 1234));
    final BrokerInfo localBroker = new BrokerInfo();
    localBroker.addPort(CommPortType.BUSINESS, 4566);
    final PartitionMemberInfo info = new PartitionMemberInfo();
    info.setMemberId(MemberId.from("member-0"));
    info.setPartitionId(PARTITION);
    info.setRole(PartitionRole.LEADER);
    info.setTerm(2L);
    final BrokerInfo.PartitionEntry entry = new BrokerInfo.PartitionEntry();
    entry.partitionId = PARTITION.id();
    entry.groupName = PARTITION.group();
    entry.role = com.anyilanxin.kunpeng.protocol.common.member.PartitionRole.LEADER;
    entry.term = 2L;
    entry.sourceId = -1;
    localBroker.addPartition(entry);
    localBroker.writeIntoProperties(localMember.properties());

    // 先经成员事件让视图认出本节点为 leader（模拟拓扑管理服务写属性后的 SWIM 通知）
    service.event(
        new ClusterMembershipEvent(ClusterMembershipEvent.Type.MEMBER_ADDED, localMember));
    awaitEventsProcessed(service);

    assertThat(service.getPartitionBusinessAddress(PARTITION))
        .isEqualTo(Address.from("10.0.0.1", 4566));
  }

  /** 构造只读收集服务：本地成员无广播数据，视图仅来自事件注入 */
  private DefaultClusterTopologyService service() {
    localMember = mock(Member.class);
    when(localMember.id()).thenReturn(MemberId.from("member-0"));
    when(localMember.properties()).thenReturn(new Properties());
    membershipService = mock(ClusterMembershipService.class);
    when(membershipService.getLocalMember()).thenReturn(localMember);
    return new DefaultClusterTopologyService(membershipService);
  }

  /** 构造并注册到调度器的服务：事件处理经 actor 线程异步执行，未注册时任务只入队不运行 */
  private DefaultClusterTopologyService startedService() {
    final DefaultClusterTopologyService service = service();
    scheduler = ActorScheduler.newActorScheduler().setSchedulerName("topology-test").build();
    scheduler.start();
    scheduler.submitActor(service).join(5, TimeUnit.SECONDS);
    return service;
  }

  /** 投递 no-op 并阻塞其完成：FIFO 队列保证此前提交的事件处理均已执行 */
  private void awaitEventsProcessed(final DefaultClusterTopologyService service) {
    service.submit(() -> {}).join(5, TimeUnit.SECONDS);
  }

  /** 模拟一个携带单分区 SWIM 广播属性的成员（不带主机与端口） */
  private Member memberBroadcast(
      final String memberId, final PartitionRole role, final long term) {
    return memberBroadcast(memberId, role, term, null, -1);
  }

  /** 模拟一个携带单分区 SWIM 广播属性的成员；host 与 businessPort（-1 表示不广播端口）用于寻址断言 */
  private Member memberBroadcast(
      final String memberId,
      final PartitionRole role,
      final long term,
      final String host,
      final int businessPort) {
    final PartitionMemberInfo info = new PartitionMemberInfo();
    info.setMemberId(MemberId.from(memberId));
    info.setPartitionId(PARTITION);
    info.setRole(role);
    info.setTerm(term);
    final BrokerInfo brokerInfo = new BrokerInfo();
    final BrokerInfo.PartitionEntry entry = new BrokerInfo.PartitionEntry();
    entry.partitionId = info.getPartitionId().id();
    entry.groupName = info.getPartitionId().group();
    entry.role = com.anyilanxin.kunpeng.protocol.common.member.PartitionRole.valueOf(role.name());
    entry.health = com.anyilanxin.kunpeng.protocol.common.member.PartitionHealth.UNHEALTHY;
    entry.term = term;
    entry.sourceId = -1;
    brokerInfo.addPartition(entry);
    if (businessPort > 0) {
      brokerInfo.addPort(CommPortType.BUSINESS, businessPort);
    }
    final Properties properties = new Properties();
    brokerInfo.writeIntoProperties(properties);
    final Member member = mock(Member.class);
    when(member.id()).thenReturn(MemberId.from(memberId));
    when(member.properties()).thenReturn(properties);
    if (host != null) {
      when(member.address()).thenReturn(Address.from(host, 1234));
    }
    return member;
  }
}
