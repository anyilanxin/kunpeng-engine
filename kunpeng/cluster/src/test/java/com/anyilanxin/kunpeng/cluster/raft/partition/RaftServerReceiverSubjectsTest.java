/*
 * Copyright © 2026 anyilanxin zxh (anyilanxin@aliyun.com)
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
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
package com.anyilanxin.kunpeng.cluster.raft.partition;

import static com.anyilanxin.kunpeng.cluster.raft.partition.RaftPartition.PARTITION_NAME_FORMAT;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import com.anyilanxin.kunpeng.cluster.cluster.ClusterMembershipService;
import com.anyilanxin.kunpeng.cluster.cluster.PartitionId;
import com.anyilanxin.kunpeng.cluster.cluster.MemberId;
import com.anyilanxin.kunpeng.cluster.cluster.PhysicalTenantIds;
import com.anyilanxin.kunpeng.cluster.cluster.messaging.ClusterCommunicationService;
import com.anyilanxin.kunpeng.cluster.raft.partition.PartitionMetadata;
import com.anyilanxin.kunpeng.cluster.raft.partition.impl.RaftPartitionServer;
import com.anyilanxin.kunpeng.cluster.raft.snapshot.ReceivableSnapshotStore;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;
import org.junit.jupiter.api.AutoClose;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * 验证 receiver 在不同分组（默认租户分组 / 自定义分组 / 旧版 subject）下，
 * 对全部协议动作完成 replyTo 注册，并在 stop 时逐个 unsubscribe。
 */
@ExtendWith(MockitoExtension.class)
public class RaftServerReceiverSubjectsTest {

  /** 全部协议动作在 subject 中的后缀名。 */
  private static final List<String> ACTION_SUFFIXES =
      List.of(
          "append",
          "append-versioned",
          "configure",
          "force-configure",
          "install",
          "join",
          "leave",
          "poll",
          "reconfigure",
          "vote",
          "transfer");

  private static final String TENANT_GROUP = "tenant-b";
  private static final String SYSTEM_GROUP = PhysicalTenantIds.DEFAULT_PHYSICAL_TENANT_ID;
  private static final String LEGACY_GROUP = "raft-partition";
  private static final MemberId NODE = new MemberId("0");
  private static final int PARTITION_NO = 1;

  @Mock private ClusterMembershipService membershipService;
  @Mock private ClusterCommunicationService communicationService;
  @Mock private ReceivableSnapshotStore snapshotStore;
  @AutoClose private MeterRegistry registry = new SimpleMeterRegistry();

  private RaftPartitionServer startServer(final String group, final boolean legacy, final Path dir) {
    final var cfg = new RaftPartitionConfig();
    cfg.setStorageConfig(new RaftStorageConfig());
    cfg.setReceiveOnLegacySubject(legacy);
    final var meta =
        new PartitionMetadata(
            new PartitionId(group, PARTITION_NO), Set.of(), Map.of(), 1, NODE);
    final var partition = new RaftPartition(meta, cfg, dir.toFile(), registry, null, null);
    return new RaftPartitionServer(
        partition,
        cfg,
        NODE,
        membershipService,
        communicationService,
        snapshotStore,
        meta,
        registry);
  }

  @ParameterizedTest(name = "group={0}, legacy={1}")
  @MethodSource("scenarioSource")
  void registersReplyHandlerPerSubject(
      final String group,
      final boolean legacyEnabled,
      final Map<String, Integer> expectedPerGroup,
      @TempDir final Path dir) {
    // given / when：创建 server，触发 subject 注册
    startServer(group, legacyEnabled, dir);

    // then：按分组断言每个动作的注册次数
    expectedPerGroup.forEach(
        (groupName, count) -> assertRegistered(groupName, count, ActionVerification.REPLY_TO));
  }

  @ParameterizedTest(name = "group={0}, legacy={1}")
  @MethodSource("scenarioSource")
  void unregistersEverySubjectOnStop(
      final String group,
      final boolean legacyEnabled,
      final Map<String, Integer> expectedPerGroup,
      @TempDir final Path dir) {
    // given
    final var server = startServer(group, legacyEnabled, dir);

    // when
    server.stop().join();

    // then：注册过的 subject 全部被注销
    expectedPerGroup.forEach(
        (groupName, count) -> assertRegistered(groupName, count, ActionVerification.UNSUBSCRIBE));
  }

  private enum ActionVerification {
    REPLY_TO {
      @Override
      void verifyOnce(
          final ClusterCommunicationService svc,
          final org.mockito.verification.VerificationMode mode,
          final String subject) {
        verify(svc, mode).replyTo(eq(subject), any(), any(), any());
      }
    },
    UNSUBSCRIBE {
      @Override
      void verifyOnce(
          final ClusterCommunicationService svc,
          final org.mockito.verification.VerificationMode mode,
          final String subject) {
        verify(svc, mode).unsubscribe(eq(subject));
      }
    };

    abstract void verifyOnce(
        ClusterCommunicationService svc,
        org.mockito.verification.VerificationMode mode,
        String subject);
  }

  private void assertRegistered(
      final String group, final int expected, final ActionVerification kind) {
    final var mode = expected > 0 ? times(expected) : never();
    final var template = PARTITION_NAME_FORMAT.formatted(group, PARTITION_NO) + "-%s";
    ACTION_SUFFIXES.forEach(
        suffix -> kind.verifyOnce(communicationService, mode, template.formatted(suffix)));
  }

  static Stream<Arguments> scenarioSource() {
    return Stream.of(
        // 默认租户分组 + 开启旧版兼容：新旧 subject 各注册一次
        Arguments.of(SYSTEM_GROUP, true, Map.of(LEGACY_GROUP, 1, SYSTEM_GROUP, 1)),
        // 默认租户分组 + 关闭旧版兼容：只注册自己的 subject
        Arguments.of(SYSTEM_GROUP, false, Map.of(LEGACY_GROUP, 0, SYSTEM_GROUP, 1)),
        // 非默认分组无论开关如何都不会监听旧版 subject
        Arguments.of(TENANT_GROUP, true, Map.of(LEGACY_GROUP, 0, TENANT_GROUP, 1)));
  }
}
