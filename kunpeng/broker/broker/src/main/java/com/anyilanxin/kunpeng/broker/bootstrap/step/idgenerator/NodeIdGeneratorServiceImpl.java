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
package com.anyilanxin.kunpeng.broker.bootstrap.step.idgenerator;

import static com.anyilanxin.kunpeng.cluster.config.ClusterAdminSerializer.SERIALIZER;
import static com.anyilanxin.kunpeng.protocol.admin.impl.record.command.common.ExecutionRecordSerialize.encode;
import static com.anyilanxin.kunpeng.protocol.common.ClusterCommonConstant.*;

import com.anyilanxin.kunpeng.broker.BrokerLoggers;
import com.anyilanxin.kunpeng.cluster.cluster.ClusterMembershipService;
import com.anyilanxin.kunpeng.cluster.cluster.Member;
import com.anyilanxin.kunpeng.cluster.cluster.leaderfound.ClusterLeaderChangeListener;
import com.anyilanxin.kunpeng.cluster.cluster.leaderfound.ClusterLeaderFoundService;
import com.anyilanxin.kunpeng.cluster.cluster.leaderfound.LeaderInfo;
import com.anyilanxin.kunpeng.cluster.cluster.messaging.MessagingService;
import com.anyilanxin.kunpeng.cluster.config.ClusterMetaStore;
import com.anyilanxin.kunpeng.cluster.config.ClusterNodeConfiguration;
import com.anyilanxin.kunpeng.protocol.admin.impl.record.command.common.NodeSourceApplyRecord;
import com.anyilanxin.kunpeng.scheduler.ConcurrencyControl;
import com.anyilanxin.kunpeng.scheduler.future.ActorFuture;
import java.time.Duration;
import java.util.concurrent.atomic.AtomicBoolean;
import org.agrona.concurrent.IdGenerator;
import org.agrona.concurrent.SnowflakeIdGenerator;
import org.agrona.concurrent.SystemEpochClock;
import org.slf4j.Logger;

/**
 * @author zxuanhong
 * @since
 */
public class NodeIdGeneratorServiceImpl
    implements NodeIdGeneratorService, ClusterLeaderChangeListener {
  private static final long TIMESTAMP_OFFSET_2023 = 1672531200000L;
  IdGenerator idGenerator;
  private final ClusterMetaStore clusterMetaStore;
  private final ConcurrencyControl concurrencyControl;
  private final MessagingService messagingService;
  private final ClusterLeaderFoundService leaderFoundService;
  private final ClusterMembershipService membershipService;
  private final AtomicBoolean initialized = new AtomicBoolean(false);
  protected static final Logger LOGGER = BrokerLoggers.BROKER_LOGGER;
  private final Member localMember;

  public NodeIdGeneratorServiceImpl(
      final ClusterMetaStore clusterMetaStore,
      final ConcurrencyControl concurrencyControl,
      final ClusterLeaderFoundService leaderFoundService,
      final ClusterMembershipService membershipService,
      final MessagingService messagingService) {
    this.clusterMetaStore = clusterMetaStore;
    this.leaderFoundService = leaderFoundService;
    this.membershipService = membershipService;
    localMember = membershipService.getLocalMember();
    this.concurrencyControl = concurrencyControl;
    this.messagingService = messagingService;
  }

  public ActorFuture<Void> start() {
    final ActorFuture<Void> future = concurrencyControl.createFuture();
    concurrencyControl.run(
        () -> {
          final ClusterNodeConfiguration nodeConfiguration =
              clusterMetaStore.getNodeConfiguration();
          if (!nodeConfiguration.isUninitialized()) {
            LOGGER.info(
                "\n\n----------------->Node configuration has been initialized<-----------------\n");
            initializeIdGenerator(nodeConfiguration.getNodeUniqueId());
          } else {
            LOGGER.info(
                "\n\n----------------->Node configuration has not been initialized<-----------------\n");
            registerInitializeIdGenerator();
          }
          leaderFoundService.addLeaderChangeListener(this);
          future.complete(null);
        });
    return future;
  }

  private void registerInitializeIdGenerator() {
    messagingService.registerHandler(
        NODE_SOURCE_TOPIC,
        (_, bytes) -> {
          concurrencyControl.run(
              () -> {
                final Integer nodeUniqueId = SERIALIZER.decode(bytes);
                LOGGER.info(
                    "\n\n----------------->Received node initial initialize,nodeUniqueId={}<-----------------\n",
                    nodeUniqueId);
                initializeIdGenerator(nodeUniqueId);
                final ClusterNodeConfiguration nodeConfiguration =
                    clusterMetaStore.getNodeConfiguration();
                nodeConfiguration.setNodeUniqueId(nodeUniqueId);
                nodeConfiguration.setVersion(1);
                clusterMetaStore.updateNodeConfiguration(nodeConfiguration);
                messagingService.unregisterHandler(NODE_SOURCE_TOPIC);
              });
        },
        concurrencyControl);
  }

  @Override
  public long nextId() {
    if (idGenerator == null) {
      throw new IllegalStateException("idGenerator has not initialize");
    }
    return idGenerator.nextId();
  }

  @Override
  public void foundLeader(final LeaderInfo memberId) {
    LOGGER.info("\n\n----------------->Cluster leader Found<-----------------\n");
    if (!initialized.get()) {
      concurrencyControl.run(
          () ->
              concurrencyControl.schedule(
                  Duration.ofSeconds(3),
                  () -> {
                    final NodeSourceApplyRecord record = new NodeSourceApplyRecord();
                    record.setMemberId(localMember.id().id());
                    LOGGER.info("\n\n----------------->Send Apply Node Source<-----------------\n");
                    final byte[] encode = encode(record);
                    messagingService.sendAndReceive(
                        memberId.address(), CLUSTER_NODE_SOURCE_TOPIC, encode);
                  }));
    }
  }

  @Override
  public void loseLeader(final LeaderInfo memberId) {}

  private void initializeIdGenerator(final int nodeUniqueId) {
    initialized.set(true);
    localMember.properties().setProperty(NODE_SOURCE_PROPERTY_KEY, String.valueOf(nodeUniqueId));
    idGenerator =
        new SnowflakeIdGenerator(
            SnowflakeIdGenerator.NODE_ID_BITS_DEFAULT,
            SnowflakeIdGenerator.SEQUENCE_BITS_DEFAULT,
            nodeUniqueId,
            TIMESTAMP_OFFSET_2023,
            SystemEpochClock.INSTANCE);
  }
}
