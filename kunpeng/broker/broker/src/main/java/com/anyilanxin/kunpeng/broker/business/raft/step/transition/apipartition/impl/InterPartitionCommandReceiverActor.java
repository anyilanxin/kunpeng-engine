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

import com.anyilanxin.kunpeng.broker.BrokerLoggers;
import com.anyilanxin.kunpeng.broker.monitoring.DiskSpaceUsageListener;
import com.anyilanxin.kunpeng.cluster.cluster.MemberId;
import com.anyilanxin.kunpeng.cluster.cluster.messaging.ClusterCommunicationService;
import com.anyilanxin.kunpeng.cluster.utils.serializer.serializers.DefaultSerializers;
import com.anyilanxin.kunpeng.eventlog.EventLogWriter;
import com.anyilanxin.kunpeng.protocol.business.record.RecordValueMapper;
import com.anyilanxin.kunpeng.scheduler.Actor;
import java.util.Map;
import org.slf4j.Logger;

/**
 * Receives messages send by @{@link InterPartitionCommandSenderImpl} and tries to write them as
 * commands to the partition's log stream. Failure to write to the log stream, for example because
 * no disk space is available, the logstream rejected the write operation or message decoding
 * failure, are ignored. The sender is responsible for recognizing failures and retrying.
 *
 * @author zxuanhong
 * @since 2026.9.0
 */
public final class InterPartitionCommandReceiverActor extends Actor
    implements DiskSpaceUsageListener {
  private static final Logger LOG = BrokerLoggers.TRANSPORT_LOGGER;
  private final String actorName;
  private final ClusterCommunicationService communicationService;
  private final int partitionId;
  private final InterPartitionCommandReceiverImpl receiver;

  public InterPartitionCommandReceiverActor(
      final int partitionId,
      final ClusterCommunicationService communicationService,
      final EventLogWriter logStreamWriter,
      final RecordValueMapper valueMapper) {
    this.partitionId = partitionId;
    this.communicationService = communicationService;
    receiver = new InterPartitionCommandReceiverImpl(logStreamWriter, valueMapper);
    actorName = buildActorName(getClass().getSimpleName(), partitionId);
  }

  @Override
  protected Map<String, String> createContext() {
    final var context = super.createContext();
    context.put("partitionId", Integer.toString(partitionId));
    return context;
  }

  @Override
  public String getName() {
    return actorName;
  }

  @Override
  protected void onActorStarting() {
    communicationService.consume(
        InterPartitionCommandSenderImpl.TOPIC_PREFIX + partitionId,
        DefaultSerializers.BASIC::decode,
        this::tryHandleMessage,
        actor::run);
  }

  @Override
  protected void onActorClosing() {
    communicationService.unsubscribe(InterPartitionCommandSenderImpl.TOPIC_PREFIX + partitionId);
  }

  @Override
  public void onDiskSpaceNotAvailable() {
    actor.run(() -> receiver.setDiskSpaceAvailable(false));
  }

  @Override
  public void onDiskSpaceAvailable() {
    actor.run(() -> receiver.setDiskSpaceAvailable(true));
  }

  private void tryHandleMessage(final MemberId memberId, final byte[] message) {
    try {
      receiver.handleMessage(memberId, message);
    } catch (final RuntimeException e) {
      LOG.error("Error while handling message", e);
    }
  }
}
