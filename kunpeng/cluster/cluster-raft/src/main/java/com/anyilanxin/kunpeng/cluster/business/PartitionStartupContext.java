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

import com.anyilanxin.kunpeng.cluster.business.step.transition.PartitionTransition;
import com.anyilanxin.kunpeng.cluster.business.step.transition.TransitionContent;
import com.anyilanxin.kunpeng.cluster.raft.logentry.EntryValidator;
import com.anyilanxin.kunpeng.cluster.raft.partition.PartitionManagementService;
import com.anyilanxin.kunpeng.cluster.raft.partition.PartitionMetadata;
import com.anyilanxin.kunpeng.cluster.raft.partition.RaftPartition;
import com.anyilanxin.kunpeng.cluster.raft.snapshot.constructable.RaftSnapshotProvider;
import com.anyilanxin.kunpeng.scheduler.ActorSchedulingService;
import com.anyilanxin.kunpeng.scheduler.ConcurrencyControl;
import io.micrometer.core.instrument.MeterRegistry;
import java.nio.file.Path;

/**
 * 分区启动上下文，聚合创建 Raft 分区、执行启动引导（bootstrap/join）以及角色切换所需的各类依赖组件。
 *
 * @author zxuanhong
 * @since
 */
public interface PartitionStartupContext<CONTENT extends TransitionContent> {

  RaftPartitionFactory getRaftPartitionFactory();

  PartitionMetadata getPartitionMetadata();

  RaftSnapshotProvider getSnapshotProvider();

  EntryValidator getEntryValidator();

  Path getPartitionDirectory();

  PartitionManagementService getPartitionManagementService();

  MeterRegistry getMeterRegistry();

  ConcurrencyControl getConcurrencyControl();

  RaftPartition getRaftPartition();

  void setRaftPartition(RaftPartition raftPartition);

  ActorSchedulingService getActorSchedulingService();

  PartitionTransition<CONTENT> getPartitionTransition();

  void setPartitionTransition(PartitionTransition<CONTENT> partitionTransition);
}
