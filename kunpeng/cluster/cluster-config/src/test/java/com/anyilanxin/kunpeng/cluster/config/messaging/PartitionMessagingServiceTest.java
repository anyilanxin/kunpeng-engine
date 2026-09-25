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
package com.anyilanxin.kunpeng.cluster.config.messaging;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.anyilanxin.kunpeng.cluster.cluster.MemberId;
import com.anyilanxin.kunpeng.cluster.cluster.PartitionId;
import com.anyilanxin.kunpeng.cluster.cluster.messaging.ClusterCommunicationService;
import com.anyilanxin.kunpeng.cluster.config.topology.PartitionMemberInfo;
import com.anyilanxin.kunpeng.cluster.config.topology.broker.ClusterSwimTopologyService;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Executor;
import java.util.function.Consumer;
import java.util.function.Function;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

/**
 * PartitionMessagingService 的单元测试：topic 分区命名空间隔离与广播范围限定
 *
 * @author zxuanhong
 * @since 2026.9.0
 */
@SuppressWarnings("unchecked")
final class PartitionMessagingServiceTest {

  private static final PartitionId PARTITION_ID = PartitionId.from("broker", 1);
  private static final String SUBJECT = "positions-broker-1";

  private final ClusterCommunicationService communicationService =
      mock(ClusterCommunicationService.class);
  private final ClusterSwimTopologyService topologyService =
      mock(ClusterSwimTopologyService.class);

  private PartitionMessagingService messagingService;

  @BeforeEach
  void setUp() {
    messagingService =
        new PartitionMessagingService(
            communicationService, topologyService, PARTITION_ID, MemberId.from("node-1"));
  }

  @Test
  void broadcastMulticastsToPartitionMembersExceptSelf() {
    givenTopologyMembers("node-1", "node-2", "node-3");
    final var payload = ByteBuffer.wrap(new byte[] {1, 2, 3});

    messagingService.broadcast("positions", payload);

    final var membersCaptor = ArgumentCaptor.forClass(Set.class);
    verify(communicationService)
        .multicast(
            eq(SUBJECT),
            eq(payload),
            any(),
            membersCaptor.capture(),
            eq(false));
    assertThat(membersCaptor.getValue())
        .containsExactlyInAnyOrder(MemberId.from("node-2"), MemberId.from("node-3"));
  }

  @Test
  void broadcastEncodesRemainingBytesOfPayload() {
    givenTopologyMembers("node-2");
    final var payload = ByteBuffer.wrap(new byte[] {1, 2, 3, 4});
    payload.position(1).limit(3);

    messagingService.broadcast("positions", payload);

    final var encoderCaptor = ArgumentCaptor.forClass(Function.class);
    verify(communicationService)
        .multicast(eq(SUBJECT), eq(payload), encoderCaptor.capture(), any(), eq(false));
    final var encoder = (Function<ByteBuffer, byte[]>) encoderCaptor.getValue();
    assertThat(encoder.apply(payload.duplicate())).containsExactly(2, 3);
  }

  @Test
  void broadcastSkipsWhenNoOtherMembers() {
    givenTopologyMembers("node-1");

    messagingService.broadcast("positions", ByteBuffer.wrap(new byte[] {1}));

    verifyNoInteractions(communicationService);
  }

  @Test
  void broadcastSkipsWhenTopologyHasNoPartitionInfo() {
    when(topologyService.getPartitionMemberInfo(PARTITION_ID)).thenReturn(List.of());

    messagingService.broadcast("positions", ByteBuffer.wrap(new byte[] {1}));

    verifyNoInteractions(communicationService);
  }

  @Test
  void subscribeRegistersHandlerOnPartitionScopedSubject() {
    final Consumer<ByteBuffer> handler = buffer -> {};
    final Executor executor = Runnable::run;

    messagingService.subscribe("positions", handler, executor);

    final var decoderCaptor = ArgumentCaptor.forClass(Function.class);
    verify(communicationService)
        .consume(eq(SUBJECT), decoderCaptor.capture(), eq(handler), eq(executor));
    final var decoded =
        ((Function<byte[], ByteBuffer>) decoderCaptor.getValue()).apply(new byte[] {7, 8});
    assertThat(decoded.remaining()).isEqualTo(2);
    assertThat(decoded.get()).isEqualTo((byte) 7);
  }

  @Test
  void unsubscribeRemovesPartitionScopedSubject() {
    messagingService.unsubscribe("positions");

    verify(communicationService).unsubscribe(SUBJECT);
  }

  private void givenTopologyMembers(final String... memberIds) {
    final var infos =
        Arrays.stream(memberIds)
            .map(
                memberId -> {
                  final var info = new PartitionMemberInfo();
                  info.setMemberId(memberId);
                  return info;
                })
            .toList();
    when(topologyService.getPartitionMemberInfo(PARTITION_ID)).thenReturn(infos);
  }
}
