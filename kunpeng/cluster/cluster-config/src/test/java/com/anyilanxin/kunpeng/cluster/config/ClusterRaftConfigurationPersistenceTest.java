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
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
package com.anyilanxin.kunpeng.cluster.config;

import static org.assertj.core.api.Assertions.assertThat;

import com.anyilanxin.kunpeng.cluster.cluster.MemberId;
import com.anyilanxin.kunpeng.cluster.cluster.PartitionId;
import com.anyilanxin.kunpeng.cluster.raft.partition.PartitionMetadata;
import com.anyilanxin.kunpeng.protocol.admin.record.command.PartitionExecutionType;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * {@link ClusterRaftConfiguration} 含 {@link PartitionMetadata} 时的持久化往返测试：模拟调度执行时序
 * （addDispatch 落盘 → dispatchComplete 携带分区元数据落盘 → store 重启读取），守护「重启后分区元数据丢失」问题。
 *
 * @author zxuanhong
 * @since
 */
class ClusterRaftConfigurationPersistenceTest {

  private static final PartitionId PARTITION_ID = PartitionId.from("business", 1);

  @TempDir private Path dataDir;

  @Test
  void shouldRoundTripRaftConfigurationWithPartitionMetadataThroughKryo() {
    final ClusterConfiguration configuration = new ClusterConfiguration();
    final ClusterRaftConfiguration raftConfiguration = configuration.getRaftConfiguration();
    raftConfiguration.setVersion(1);
    raftConfiguration.haveDispatch(
        10L, 1L, PartitionExecutionType.BOOTSTRAP, new byte[] {1, 2, 3});
    raftConfiguration.setVersion(2);
    raftConfiguration.notDispatch();
    raftConfiguration.setVersion(3);
    raftConfiguration.addPartition(metadata());

    final byte[] bytes = ClusterAdminSerializer.SERIALIZER.encode(configuration);
    final ClusterConfiguration decoded = ClusterAdminSerializer.SERIALIZER.decode(bytes);

    final PartitionMetadata restored = decoded.getRaftConfiguration().getPartition(PARTITION_ID);
    assertThat(restored).isNotNull();
    assertThat(restored.id()).isEqualTo(PARTITION_ID);
    assertThat(restored.members()).containsExactlyInAnyOrderElementsOf(metadata().members());
    assertThat(restored.getPrimary()).contains(MemberId.from("member-0"));
    assertThat(restored.getPriority(MemberId.from("member-1"))).isEqualTo(2);
    assertThat(restored.getTargetPriority()).isEqualTo(3);
    assertThat(decoded.getRaftConfiguration().getVersion()).isEqualTo(3);
    assertThat(decoded.getRaftConfiguration().haveDispatch()).isFalse();
  }

  @Test
  void shouldPersistPartitionMetadataAcrossStoreRestart() {
    // 第一阶段：addDispatch 落盘（无分区元数据），随后 dispatchComplete 携带分区元数据再次落盘
    final DefaultClusterMetaStore store = new DefaultClusterMetaStore(dataDir);
    try {
      final ClusterRaftConfiguration config = store.getRaftConfiguration();
      config.setVersion(1);
      config.haveDispatch(10L, 1L, PartitionExecutionType.BOOTSTRAP, new byte[] {1, 2, 3});
      config.setVersion(2);
      store.updateRaftConfiguration(config);

      final ClusterRaftConfiguration completed = store.getRaftConfiguration();
      completed.addPartition(metadata());
      completed.notDispatch();
      completed.setVersion(completed.getVersion() + 1);
      store.updateRaftConfiguration(completed);
    } finally {
      store.close();
    }

    // 第二阶段：重启后读取
    final DefaultClusterMetaStore restarted = new DefaultClusterMetaStore(dataDir);
    try {
      final ClusterRaftConfiguration config = restarted.getRaftConfiguration();
      assertThat(config.isUninitialized()).isFalse();
      assertThat(config.getPartition(PARTITION_ID)).isEqualTo(metadata());
    } finally {
      restarted.close();
    }
  }

  @Test
  void shouldRoundTripJdkImmutableCollectionsInPartitionMetadata() {
    // JDK 不可变集合（Set.of/Map.of 及空集、null primary）曾因未注册导致 encode 抛异常且被 actor 回调吞掉、静默丢数据
    final ClusterConfiguration configuration = new ClusterConfiguration();
    final ClusterRaftConfiguration raftConfiguration = configuration.getRaftConfiguration();
    raftConfiguration.setVersion(1);
    raftConfiguration.addPartition(immutableMetadata(PartitionId.from("business", 1), 3));
    raftConfiguration.addPartition(immutableMetadata(PartitionId.from("business", 2), 1));
    raftConfiguration.addPartition(immutableMetadata(PartitionId.from("business", 3), 0));

    final byte[] bytes = ClusterAdminSerializer.SERIALIZER.encode(configuration);
    final ClusterConfiguration decoded = ClusterAdminSerializer.SERIALIZER.decode(bytes);

    final ClusterRaftConfiguration restoredConfig = decoded.getRaftConfiguration();
    assertThat(restoredConfig.getPartition(PartitionId.from("business", 1)))
        .isEqualTo(immutableMetadata(PartitionId.from("business", 1), 3));
    assertThat(restoredConfig.getPartition(PartitionId.from("business", 2)))
        .isEqualTo(immutableMetadata(PartitionId.from("business", 2), 1));
    assertThat(restoredConfig.getPartition(PartitionId.from("business", 3)))
        .isEqualTo(immutableMetadata(PartitionId.from("business", 3), 0));
  }

  /** memberCount 控制集合形态：3 → SetN/MapN，1 → Set12/Map1，0 → 空集 + null primary（JDK 15+ 空集与 N 类同实现）。 */
  private static PartitionMetadata immutableMetadata(
      final PartitionId partitionId, final int memberCount) {
    if (memberCount >= 3) {
      return new PartitionMetadata(
          partitionId,
          Set.of(
              MemberId.from("member-0"), MemberId.from("member-1"), MemberId.from("member-2")),
          Map.of(
              MemberId.from("member-0"), 3,
              MemberId.from("member-1"), 2,
              MemberId.from("member-2"), 1),
          3,
          MemberId.from("member-0"));
    }
    if (memberCount == 1) {
      return new PartitionMetadata(
          partitionId,
          Set.of(MemberId.from("member-0")),
          Map.of(MemberId.from("member-0"), 1),
          1,
          MemberId.from("member-0"));
    }
    return new PartitionMetadata(partitionId, Set.of(), Map.of(), 1, null);
  }

  /** 与调度落盘路径一致：HashSet/HashMap 标准集合 */
  private static PartitionMetadata metadata() {
    final Set<MemberId> members = new HashSet<>();
    members.add(MemberId.from("member-0"));
    members.add(MemberId.from("member-1"));
    members.add(MemberId.from("member-2"));
    final Map<MemberId, Integer> priorities = new HashMap<>();
    priorities.put(MemberId.from("member-0"), 3);
    priorities.put(MemberId.from("member-1"), 2);
    priorities.put(MemberId.from("member-2"), 1);
    return new PartitionMetadata(PARTITION_ID, members, priorities, 3, MemberId.from("member-0"));
  }
}
