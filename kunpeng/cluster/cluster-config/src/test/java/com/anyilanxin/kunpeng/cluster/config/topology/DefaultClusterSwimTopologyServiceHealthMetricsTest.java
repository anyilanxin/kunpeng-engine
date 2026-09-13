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

import com.anyilanxin.kunpeng.cluster.cluster.ClusterMembershipService;
import com.anyilanxin.kunpeng.cluster.cluster.Member;
import com.anyilanxin.kunpeng.cluster.cluster.MemberId;
import com.anyilanxin.kunpeng.cluster.cluster.PartitionId;
import com.anyilanxin.kunpeng.cluster.config.topology.broker.DefaultClusterSwimTopologyService;
import com.anyilanxin.kunpeng.cluster.raft.RaftServer;
import com.anyilanxin.kunpeng.cluster.utils.health.HealthReport;
import com.anyilanxin.kunpeng.scheduler.ActorScheduler;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.util.Properties;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

/** 本地分区健康 Micrometer gauge 集成测试：分区首次出现时注册，后续更新自动反映。 */
class DefaultClusterSwimTopologyServiceHealthMetricsTest {
  private static final String METRIC = "kunpeng.engine.partition.health";

  private ActorScheduler scheduler;

  @AfterEach
  void tearDown() {
    if (scheduler != null) {
      scheduler.close();
    }
  }

  @Test
  void shouldRegisterGaugeOnFirstCallbackAndReflectUpdates() {
    final MeterRegistry registry = new SimpleMeterRegistry();
    final DefaultClusterSwimTopologyService service = startedService(registry);

    service.onPartitionRoleChanged(
        PartitionId.from("raft-partition", 1), RaftServer.Role.LEADER, 3L);
    awaitEventsProcessed(service);

    final Gauge gauge =
        registry
            .get(METRIC)
            .tag("group", "raft-partition")
            .tag("partition", "1")
            .tag("member", "member-0")
            .gauge();
    assertThat(gauge.value()).isEqualTo(PartitionHealth.HEALTHY.getValue());

    final HealthReport unhealthy = HealthReport.unhealthy("test");
    service.onPartitionHealthChanged(PartitionId.from("raft-partition", 1), unhealthy);
    awaitEventsProcessed(service);
    assertThat(gauge.value()).isEqualTo(PartitionHealth.UNHEALTHY.getValue());

    service.onPartitionRoleChanged(
        PartitionId.from("raft-partition", 1), RaftServer.Role.INACTIVE, 4L);
    awaitEventsProcessed(service);
    assertThat(gauge.value()).isEqualTo(PartitionHealth.UNHEALTHY.getValue());
  }

  @Test
  void shouldExposeSeparateGaugesPerPartition() {
    final MeterRegistry registry = new SimpleMeterRegistry();
    final DefaultClusterSwimTopologyService service = startedService(registry);

    service.onPartitionRoleChanged(
        PartitionId.from("raft-partition", 1), RaftServer.Role.LEADER, 1L);
    service.onPartitionRoleChanged(
        PartitionId.from("raft-partition", 2), RaftServer.Role.INACTIVE, 1L);
    awaitEventsProcessed(service);

    assertThat(
            registry
                .get(METRIC)
                .tag("partition", "1")
                .gauge()
                .value())
        .isEqualTo(PartitionHealth.HEALTHY.getValue());
    assertThat(
            registry
                .get(METRIC)
                .tag("partition", "2")
                .gauge()
                .value())
        .isEqualTo(PartitionHealth.UNHEALTHY.getValue());
  }

  private ClusterMembershipService membershipService() {
    final Member member = mock(Member.class);
    when(member.id()).thenReturn(MemberId.from("member-0"));
    when(member.properties()).thenReturn(new Properties());
    final ClusterMembershipService membershipService = mock(ClusterMembershipService.class);
    when(membershipService.getLocalMember()).thenReturn(member);
    return membershipService;
  }

  /** 构造并注册到调度器的服务：回调处理经 actor 线程异步执行，未注册时任务只入队不运行 */
  private DefaultClusterSwimTopologyService startedService(final MeterRegistry registry) {
    final DefaultClusterSwimTopologyService service =
        new DefaultClusterSwimTopologyService(membershipService(), registry);
    scheduler = ActorScheduler.newActorScheduler().setSchedulerName("topology-metrics-test").build();
    scheduler.start();
    scheduler.submitActor(service).join(5, TimeUnit.SECONDS);
    return service;
  }

  /** 投递 no-op 并阻塞其完成：FIFO 队列保证此前提交的回调处理均已执行 */
  private void awaitEventsProcessed(final DefaultClusterSwimTopologyService service) {
    service.submit(() -> {}).join(5, TimeUnit.SECONDS);
  }
}
