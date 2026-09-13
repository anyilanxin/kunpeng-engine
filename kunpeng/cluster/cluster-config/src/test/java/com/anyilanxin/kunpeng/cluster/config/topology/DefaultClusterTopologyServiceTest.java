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

import static com.anyilanxin.kunpeng.protocol.common.ClusterCommonConstant.TOPOLOGY_PROPERTY_KEY;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.anyilanxin.kunpeng.cluster.cluster.ClusterMembershipEvent;
import com.anyilanxin.kunpeng.cluster.cluster.ClusterMembershipService;
import com.anyilanxin.kunpeng.cluster.cluster.Member;
import com.anyilanxin.kunpeng.cluster.cluster.MemberId;
import com.anyilanxin.kunpeng.cluster.cluster.PartitionId;
import com.anyilanxin.kunpeng.cluster.config.ClusterAdminSerializer;
import com.anyilanxin.kunpeng.cluster.config.topology.cluster.DefaultClusterTopologyService;
import com.anyilanxin.kunpeng.scheduler.ActorScheduler;
import java.util.Base64;
import java.util.Properties;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

/** {@link DefaultClusterTopologyService} 多成员广播汇聚视图测试。 */
class DefaultClusterTopologyServiceTest {

  private static final PartitionId PARTITION = PartitionId.from("raft-partition", 1);

  private ActorScheduler scheduler;

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
  }

  /** 构造只读收集服务：本地成员无广播数据，视图仅来自事件注入 */
  private DefaultClusterTopologyService service() {
    final Member localMember = mock(Member.class);
    when(localMember.id()).thenReturn(MemberId.from("member-0"));
    when(localMember.properties()).thenReturn(new Properties());
    final ClusterMembershipService membershipService = mock(ClusterMembershipService.class);
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

  /** 模拟一个携带单分区 SWIM 广播属性的成员 */
  private Member memberBroadcast(
      final String memberId, final PartitionRole role, final long term) {
    final PartitionMemberInfo info = new PartitionMemberInfo();
    info.setMemberId(memberId);
    info.setPartitionId(PARTITION);
    info.setRole(role);
    info.setTerm(term);
    final byte[] bytes =
        ClusterAdminSerializer.SERIALIZER.encode(new SwimPartitionMemberInfo().add(info));
    final Properties properties = new Properties();
    properties.setProperty(TOPOLOGY_PROPERTY_KEY, Base64.getEncoder().encodeToString(bytes));
    final Member member = mock(Member.class);
    when(member.id()).thenReturn(MemberId.from(memberId));
    when(member.properties()).thenReturn(properties);
    return member;
  }
}
