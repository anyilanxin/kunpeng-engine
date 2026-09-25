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
 * along from this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package com.anyilanxin.kunpeng.cluster.raft.protocol;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import com.anyilanxin.kunpeng.cluster.cluster.MemberId;
import com.anyilanxin.kunpeng.cluster.raft.LeadershipTransferResult;
import com.anyilanxin.kunpeng.cluster.raft.RaftError;
import com.anyilanxin.kunpeng.cluster.raft.cluster.RaftMember;
import com.anyilanxin.kunpeng.cluster.raft.cluster.impl.DefaultRaftMember;
import java.nio.ByteBuffer;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import org.junit.jupiter.api.Test;

/**
 * 全量协议报文的 SBE 编解码往返一致性测试。
 *
 * @author zxuanhong
 * @since 2026.9.0
 */
final class SbeRaftProtocolSerializerTest {

  private final SbeRaftProtocolSerializer serializer = new SbeRaftProtocolSerializer();

  @Test
  void versionedAppendRequestRoundTrip() {
    final List<ReplicatableJournalRecord> entries =
        IntStream.range(0, 5)
            .mapToObj(
                i ->
                    new ReplicatableJournalRecord(
                        7L, 100L + i, 0xC0FFEEL + i, ("record-" + i).getBytes()))
            .collect(Collectors.toList());
    final VersionedAppendRequest request =
        new VersionedAppendRequest(2, 7L, "leader-1", 99L, 6L, entries, 105L);

    final byte[] encoded = serializer.encode(request);
    System.out.printf("[debug] versionedAppend encoded=%d hex=%s%n", encoded.length,
        java.util.HexFormat.of().formatHex(encoded));
    final VersionedAppendRequest decoded = serializer.decode(serializer.encode(request));

    assertEquals(request, decoded);
  }

  @Test
  void appendRequestRoundTrip() {
    final List<PersistedRaftRecord> entries =
        IntStream.range(0, 3)
            .mapToObj(
                i ->
                    new PersistedRaftRecord(
                        5L + i, 200L + i, 3000L + i, 0xABCDL + i, ("entry-" + i).getBytes()))
            .collect(Collectors.toList());
    final AppendRequest request = new AppendRequest(5L, "leader-2", 199L, 4L, entries, 202L);

    final AppendRequest decoded = serializer.decode(serializer.encode(request));

    assertEquals(request.term(), decoded.term());
    assertEquals(request.leader(), decoded.leader());
    assertEquals(request.prevLogIndex(), decoded.prevLogIndex());
    assertEquals(request.prevLogTerm(), decoded.prevLogTerm());
    assertEquals(request.commitIndex(), decoded.commitIndex());
    assertEquals(request.entries().size(), decoded.entries().size());
    for (int i = 0; i < entries.size(); i++) {
      final PersistedRaftRecord expected = entries.get(i);
      final PersistedRaftRecord actual = decoded.entries().get(i);
      assertEquals(expected.term(), actual.term());
      assertEquals(expected.index(), actual.index());
      assertEquals(expected.asqn(), actual.asqn());
      assertEquals(expected.checksum(), actual.checksum());
      assertArrayEquals(toBytes(expected.data()), toBytes(actual.data()));
    }
  }

  @Test
  void electionMessagesRoundTrip() {
    assertEquals(
        new PollRequest(8L, "candidate-1", 400L, 7L),
        serializer.decode(serializer.encode(new PollRequest(8L, "candidate-1", 400L, 7L))));
    assertEquals(
        new VoteRequest(9L, "candidate-2", 401L, 8L),
        serializer.decode(serializer.encode(new VoteRequest(9L, "candidate-2", 401L, 8L))));

    final PollResponse okPoll = new PollResponse(RaftResponse.Status.OK, null, 9L, true);
    assertEquals(okPoll, serializer.decode(serializer.encode(okPoll)));

    final VoteResponse voted =
        new VoteResponse(RaftResponse.Status.OK, null, 10L, true);
    assertEquals(voted, serializer.decode(serializer.encode(voted)));

    final RaftError raftError = new RaftError(RaftError.Type.NO_LEADER, "leader 未就绪");
    final PollResponse failedPoll = new PollResponse(RaftResponse.Status.ERROR, raftError, 10L, false);
    final PollResponse decodedPoll = serializer.decode(serializer.encode(failedPoll));
    assertEquals(failedPoll.status(), decodedPoll.status());
    assertEquals(raftError.type(), decodedPoll.error().type());
    assertEquals(raftError.message(), decodedPoll.error().message());
  }

  @Test
  void appendResponseRoundTrip() {
    final AppendResponse response =
        new AppendResponse(RaftResponse.Status.OK, null, 11L, true, 500L, 490L, 488L);
    assertEquals(response, serializer.decode(serializer.encode(response)));
  }

  @Test
  void installMessagesRoundTrip() {
    final InstallRequest request =
        new InstallRequest(
            12L,
            MemberId.from("leader-3"),
            600L,
            11L,
            3,
            ByteBuffer.wrap("chunk-1".getBytes()),
            ByteBuffer.wrap("chunk-2".getBytes()),
            ByteBuffer.wrap(new byte[64]),
            true,
            false);
    assertEquals(request, serializer.decode(serializer.encode(request)));

    // 可空 chunk 字段
    final InstallRequest sparse =
        new InstallRequest(
            12L, MemberId.from("leader-3"), 600L, 11L, 3, null, null, null, false, true);
    final InstallRequest decodedSparse = serializer.decode(serializer.encode(sparse));
    assertEquals(sparse.currentTerm(), decodedSparse.currentTerm());
    assertNull(decodedSparse.chunkId());
    assertNull(decodedSparse.nextChunkId());
    assertNull(decodedSparse.data());

    final InstallResponse response =
        new InstallResponse(RaftResponse.Status.OK, null, 4096);
    final InstallResponse decoded = serializer.decode(serializer.encode(response));
    assertEquals(response.status(), decoded.status());
    assertEquals(response.preferredChunkSize(), decoded.preferredChunkSize());
  }

  @Test
  void configurationMessagesRoundTrip() {
    final Instant updated = Instant.ofEpochSecond(1_700_000_000L, 123_456_789);
    final List<RaftMember> members =
        List.of(
            new DefaultRaftMember(MemberId.from("node-1"), RaftMember.Type.ACTIVE, updated),
            new DefaultRaftMember(MemberId.from("node-2"), RaftMember.Type.PASSIVE, updated));

    final ConfigureRequest configure =
        new ConfigureRequest(13L, "leader-4", 700L, 1_700_000_000_500L, members, List.of());
    assertEquals(configure, serializer.decode(serializer.encode(configure)));

    final ReconfigureRequest reconfigure = new ReconfigureRequest(members, 701L, 13L, "node-1");
    assertEquals(reconfigure, serializer.decode(serializer.encode(reconfigure)));

    final ForceConfigureRequest force =
        new ForceConfigureRequest(14L, 702L, 1_700_000_001_000L, Set.copyOf(members), "node-1");
    assertEquals(force, serializer.decode(serializer.encode(force)));

    final ForceConfigureResponse response =
        new ForceConfigureResponse(RaftResponse.Status.OK, null, 702L, 14L);
    final ForceConfigureResponse decoded = serializer.decode(serializer.encode(response));
    assertEquals(response.status(), decoded.status());
    assertEquals(response.index(), decoded.index());
    assertEquals(response.term(), decoded.term());

    assertEquals(
        new ConfigureResponse(RaftResponse.Status.OK, null),
        serializer.decode(serializer.encode(new ConfigureResponse(RaftResponse.Status.OK, null))));
    final ReconfigureResponse reconfigureResponse =
        new ReconfigureResponse(RaftResponse.Status.ERROR, null, 3L, 2L, 1L, List.of());
    assertEquals(
        reconfigureResponse, serializer.decode(serializer.encode(reconfigureResponse)));
  }

  @Test
  void membershipMessagesRoundTrip() {
    final DefaultRaftMember joining =
        new DefaultRaftMember(
            MemberId.from("node-3"), RaftMember.Type.PROMOTABLE, Instant.ofEpochSecond(5L));
    assertEquals(
        new JoinRequest(joining), serializer.decode(serializer.encode(new JoinRequest(joining))));
    assertEquals(
        new LeaveRequest(joining),
        serializer.decode(serializer.encode(new LeaveRequest(joining))));
    final JoinResponse joinResponse =
        JoinResponse.builder().withStatus(RaftResponse.Status.OK).build();
    assertEquals(joinResponse, serializer.decode(serializer.encode(joinResponse)));
    final LeaveResponse leaveResponse =
        LeaveResponse.builder().withStatus(RaftResponse.Status.ERROR).build();
    assertEquals(leaveResponse, serializer.decode(serializer.encode(leaveResponse)));
  }

  @Test
  void leadershipTransferMessagesRoundTrip() {
    final TransferRequest transfer = new TransferRequest(MemberId.from("node-2"));
    final TransferRequest decodedTransfer = serializer.decode(serializer.encode(transfer));
    assertEquals(transfer.member(), decodedTransfer.member());

    assertEquals(
        new TransferResponse(RaftResponse.Status.OK, null),
        serializer.decode(serializer.encode(new TransferResponse(RaftResponse.Status.OK, null))));

    final TimeoutNowRequest timeoutNow = new TimeoutNowRequest(20L, MemberId.from("node-4"));
    assertEquals(timeoutNow, serializer.decode(serializer.encode(timeoutNow)));
    assertEquals(
        new TimeoutNowResponse(RaftResponse.Status.OK, null),
        serializer.decode(
            serializer.encode(new TimeoutNowResponse(RaftResponse.Status.OK, null))));

    final LeadershipTransferInitiateRequest full =
        new LeadershipTransferInitiateRequest(
            MemberId.from("node-2"),
            MemberId.from("node-1"),
            42L,
            77L,
            8L * 1024 * 1024,
            Duration.ofSeconds(30),
            3);
    assertEquals(full, serializer.decode(serializer.encode(full)));

    // 可空参数走哨兵值
    final LeadershipTransferInitiateRequest sparse =
        new LeadershipTransferInitiateRequest(
            MemberId.from("node-2"), MemberId.from("node-1"), 42L, 78L, null, null, null);
    assertEquals(sparse, serializer.decode(serializer.encode(sparse)));

    final LeadershipTransferInitiateResponse accepted =
        new LeadershipTransferInitiateResponse(RaftResponse.Status.OK, null, null);
    final LeadershipTransferInitiateResponse decodedAccepted =
        serializer.decode(serializer.encode(accepted));
    assertEquals(accepted.status(), decodedAccepted.status());
    assertNull(decodedAccepted.rejectionReason());

    final LeadershipTransferInitiateResponse rejected =
        new LeadershipTransferInitiateResponse(
            RaftResponse.Status.ERROR, null, LeadershipTransferResult.LAG_TOO_HIGH);
    final LeadershipTransferInitiateResponse decodedRejected =
        serializer.decode(serializer.encode(rejected));
    assertEquals(rejected.rejectionReason(), decodedRejected.rejectionReason());

    final LeadershipTransferResultRequest result =
        new LeadershipTransferResultRequest(
            MemberId.from("node-1"), MemberId.from("node-2"),
            LeadershipTransferResult.TRANSFERRED, 77L);
    assertEquals(result, serializer.decode(serializer.encode(result)));

    assertEquals(
        new LeadershipTransferResultResponse(RaftResponse.Status.OK, null),
        serializer.decode(
            serializer.encode(new LeadershipTransferResultResponse(RaftResponse.Status.OK, null))));
  }

  @Test
  void wireFormatIsCompactAndStable() {
    final byte[] pollBytes = serializer.encode(new PollRequest(8L, "candidate-1", 400L, 7L));
    // 头(8) + 块(24) + 字符串长度(2) + 候选人(11)
    assertEquals(8 + 24 + 2 + "candidate-1".length(), pollBytes.length);
    assertNotNull(pollBytes);
  }

  private static byte[] toBytes(final org.agrona.DirectBuffer buffer) {
    final byte[] copy = new byte[buffer.capacity()];
    buffer.getBytes(0, copy);
    return copy;
  }
}
