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
package com.anyilanxin.kunpeng.cluster.business;

import static com.anyilanxin.kunpeng.protocol.common.ClusterCommonConstant.PARTITIONS_DIRECTORY;
import static com.anyilanxin.kunpeng.protocol.common.ClusterCommonConstant.RUNTIME_DIRECTORY;

import com.anyilanxin.kunpeng.cluster.raft.logentry.EntryValidator;
import com.anyilanxin.kunpeng.cluster.raft.partition.*;
import com.anyilanxin.kunpeng.cluster.raft.snapshot.constructable.RaftSnapshotProvider;
import com.anyilanxin.kunpeng.cluster.raft.storage.log.DelayedFlusher;
import com.anyilanxin.kunpeng.cluster.raft.storage.log.RaftLogFlusher;
import com.anyilanxin.kunpeng.configuration.broker.BrokerCfg;
import com.anyilanxin.kunpeng.configuration.broker.RaftCfg;
import com.anyilanxin.kunpeng.rocksdb.RocksdbSnapshotFileInfoProvider;
import com.anyilanxin.kunpeng.scheduler.ActorSchedulingService;
import com.anyilanxin.kunpeng.utils.FileUtil;
import io.micrometer.core.instrument.MeterRegistry;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import org.slf4j.Logger;

/** Raft 分区工厂，基于 broker 配置构建 RaftPartition 及其存储与分区运行配置。 */
public final class RaftPartitionFactory {
  private static final Logger LOG = ClusterRaftLoggers.CLUSTER_RAFT;
  private final BrokerCfg brokerCfg;
  private final PartitionManagementService managementService;
  private final ActorSchedulingService actorSchedulingService;

  public RaftPartitionFactory(
      final BrokerCfg brokerCfg,
      final PartitionManagementService managementService,
      final ActorSchedulingService actorSchedulingService) {
    this.brokerCfg = brokerCfg;
    this.managementService = managementService;
    this.actorSchedulingService = actorSchedulingService;
  }

  public RaftPartition createRaftPartition(
      final PartitionMetadata partitionMetadata,
      final RaftSnapshotProvider snapshotProvider,
      final EntryValidator entryValidator,
      final MeterRegistry meterRegistry) {
    final var partitionDirectory =
        Paths.get(brokerCfg.getData().getDirectory())
            .resolve(partitionMetadata.id().group())
            .resolve(PARTITIONS_DIRECTORY)
            .resolve(String.valueOf(partitionMetadata.id().id()));
    try {
      if (FileUtil.isEmpty(partitionDirectory)) {
        LOG.info(
            "Root directory {} for partition {} is empty or does not exist. The partition {} is starting with no pre-existing data.",
            partitionDirectory,
            partitionMetadata.id(),
            partitionMetadata.id());
      }
      FileUtil.ensureDirectory(partitionDirectory);
    } catch (final IOException e) {
      throw new UncheckedIOException(e);
    }
    final var partitionRuntimeDirectory =
        Paths.get(brokerCfg.getData().getDirectory())
            .resolve(partitionMetadata.id().group())
            .resolve(PARTITIONS_DIRECTORY)
            .resolve(String.valueOf(partitionMetadata.id().id()))
            .resolve(RUNTIME_DIRECTORY);
    snapshotProvider.setPartitionDirectory(partitionDirectory);
    snapshotProvider.setRuntimeDirectory(partitionRuntimeDirectory);

    return createRaftPartition(
        partitionMetadata,
        partitionDirectory,
        partitionDirectory,
        entryValidator,
        snapshotProvider,
        meterRegistry);
  }

  private RaftPartition createRaftPartition(
      final PartitionMetadata partitionMetadata,
      final Path partitionDirectory,
      final Path runtimeDirectory,
      final EntryValidator entryValidator,
      final RaftSnapshotProvider snapshotProvider,
      final MeterRegistry meterRegistry) {
    final var storageConfig = new RaftStorageConfig();
    final var partitionConfig = new RaftPartitionConfig();

    final var maxMessageSize = brokerCfg.getRaft().getMaxMessageSizeInBytes();
    final var segmentSize = brokerCfg.getData().getLogSegmentSizeInBytes();
    if (segmentSize < maxMessageSize) {
      throw new IllegalArgumentException(
          String.format(
              "Expected the raft segment size greater than the max message size of %s, but was %s.",
              maxMessageSize, segmentSize));
    }
    storageConfig.setSegmentSize(segmentSize);

    storageConfig.setFlusherFactory(createFlusherFactory(brokerCfg.getRaft().getFlush()));
    storageConfig.setFreeDiskSpace(
        brokerCfg.getData().getDisk().getFreeSpace().getReplication().toBytes());
    storageConfig.setJournalIndexDensity(brokerCfg.getData().getLogIndexDensity());

    partitionConfig.setStorageConfig(storageConfig);
    partitionConfig.setEntryValidator(entryValidator);
    partitionConfig.setMaxAppendBatchSize((int) brokerCfg.getRaft().getMaxAppendBatchSizeInBytes());
    partitionConfig.setMaxAppendsPerFollower(brokerCfg.getRaft().getMaxAppendsPerFollower());
    partitionConfig.setPriorityElectionEnabled(brokerCfg.getRaft().isEnablePriorityElection());
    partitionConfig.setElectionTimeout(brokerCfg.getRaft().getElectionTimeout());
    partitionConfig.setHeartbeatInterval(brokerCfg.getRaft().getHeartbeatInterval());
    partitionConfig.setRequestTimeout(brokerCfg.getRaft().getRequestTimeout());
    partitionConfig.setSnapshotRequestTimeout(brokerCfg.getRaft().getSnapshotRequestTimeout());
    partitionConfig.setSnapshotChunkSize(
        (int) brokerCfg.getRaft().getSnapshotChunkSize().toBytes());
    partitionConfig.setConfigurationChangeTimeout(
        brokerCfg.getRaft().getConfigurationChangeTimeout());
    partitionConfig.setMaxQuorumResponseTimeout(brokerCfg.getRaft().getMaxQuorumResponseTimeout());
    partitionConfig.setMinStepDownFailureCount(brokerCfg.getRaft().getMinStepDownFailureCount());
    partitionConfig.setPreferSnapshotReplicationThreshold(
        brokerCfg.getRaft().getPreferSnapshotReplicationThreshold());
    return new RaftPartition(
        partitionMetadata,
        partitionConfig,
        partitionDirectory,
        runtimeDirectory,
        meterRegistry,
        managementService,
        actorSchedulingService,
        snapshotProvider,
        new RocksdbSnapshotFileInfoProvider());
  }

  private RaftLogFlusher.Factory createFlusherFactory(final RaftCfg.FlushConfig config) {
    if (config.enabled()) {
      final Duration delayTime = config.delayTime();
      if (delayTime.isZero()) {
        return RaftLogFlusher.Factory::direct;
      }
      return threadFactory -> new DelayedFlusher(threadFactory.createContext(), delayTime);
    }
    LOG.warn(
        """
        Explicit Raft flush is disabled. Data will be flushed to disk only before a snapshot is
        taken. This is generally unsafe and could lead to data loss or corruption. Make sure to
        read the documentation regarding this feature.""");

    return RaftLogFlusher.Factory::noop;
  }
}
