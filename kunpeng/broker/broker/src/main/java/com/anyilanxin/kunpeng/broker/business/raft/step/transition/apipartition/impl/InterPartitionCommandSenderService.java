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
package com.anyilanxin.kunpeng.broker.business.raft.step.transition.apipartition.impl;

import com.anyilanxin.kunpeng.broker.topology.TopologyPartitionListener;
import com.anyilanxin.kunpeng.cluster.cluster.messaging.ClusterCommunicationService;
import com.anyilanxin.kunpeng.cluster.config.topology.PartitionMemberInfo;
import com.anyilanxin.kunpeng.engine.bpmn.InterPartitionCommandSender;
import com.anyilanxin.kunpeng.protocol.business.ValueLifeCycle;
import com.anyilanxin.kunpeng.protocol.common.PartitionSourceMetadata;
import com.anyilanxin.kunpeng.protocol.common.UnifiedRecordValue;
import com.anyilanxin.kunpeng.scheduler.Actor;
import java.util.Set;

/**
 * 跨分区命令发送服务。
 *
 * @author zxuanhong
 * @since 2026.9.0
 */
public final class InterPartitionCommandSenderService extends Actor
    implements InterPartitionCommandSender, TopologyPartitionListener {

  final InterPartitionCommandSenderImpl commandSender;
  final int partitionId;

  public InterPartitionCommandSenderService(
      final ClusterCommunicationService communicationService, final int partitionId) {
    commandSender = new InterPartitionCommandSenderImpl(communicationService);
    this.partitionId = partitionId;
  }

  @Override
  public void sendCommand(
      final int receiverPartitionId,
      final ValueLifeCycle lifeCycle,
      final UnifiedRecordValue command) {
    actor.submit(() -> commandSender.sendCommand(receiverPartitionId, lifeCycle, command));
  }

  @Override
  public void sendCommand(
      final int receiverPartitionId,
      final ValueLifeCycle lifeCycle,
      final Long recordKey,
      final UnifiedRecordValue command) {
    actor.submit(
        () -> commandSender.sendCommand(receiverPartitionId, lifeCycle, recordKey, command));
  }

  @Override
  public void sendCommand(
      final int receiverResourceId,
      final ValueLifeCycle lifeCycle,
      final Long recordKey,
      final Long operationReference,
      final UnifiedRecordValue command) {
    actor.submit(
        () ->
            commandSender.sendCommand(
                receiverResourceId, lifeCycle, recordKey, operationReference, command));
  }

  @Override
  public void onPartitionLeaderUpdated(
      final PartitionSourceMetadata sourceMetadata, final PartitionMemberInfo member) {
    actor.submit(
        () ->
            commandSender.setCurrentLeader(
                sourceMetadata.partitionId(),
                sourceMetadata.sourceId(),
                sourceMetadata.agentSourceIds(),
                member.getMemberId()));
  }

  @Override
  public Set<Integer> getActivitySourceIds() {
    return commandSender.getActivitySourceIds();
  }
}
