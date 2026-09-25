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
package com.anyilanxin.kunpeng.cluster.raft.protocol;

import com.anyilanxin.kunpeng.cluster.cluster.MemberId;
import com.anyilanxin.kunpeng.cluster.raft.LeadershipTransferResult;
import com.anyilanxin.kunpeng.cluster.raft.RaftError;
import com.anyilanxin.kunpeng.cluster.raft.cluster.RaftMember;
import com.anyilanxin.kunpeng.cluster.raft.cluster.impl.DefaultRaftMember;
import com.anyilanxin.kunpeng.cluster.utils.serializer.Serializer;
import java.nio.ByteBuffer;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;
import org.agrona.DirectBuffer;
import org.agrona.ExpandableArrayBuffer;
import org.agrona.concurrent.UnsafeBuffer;

/**
 * Raft 协议报文的 SBE 编解码器，整体替换原 Fory 命名空间 {@code RaftNamespaces.RAFT_PROTOCOL}。
 *
 * <p>风格参考 Aeron：解码侧 {@code Decoder} flyweight 直接包裹入参 byte[]（{@link UnsafeBuffer}）原地读取，
 * 无反射、无反序列化器注册表；编码侧 ThreadLocal 复用 {@link ExpandableArrayBuffer}，仅最终一次定长拷贝交给
 * 发送任务持有。可空字段用哨兵值表达（int64 最小值 / -1 纳秒 / int32 最小值 / 枚举 nullValue）。
 *
 * <p>枚举按「名字」与生成代码映射，不依赖源枚举序数；源枚举增删值需同步 raft-protocol-schema.xml。
 *
 * @author zxuanhong
 * @since 2026.9.0
 */
public final class SbeRaftProtocolSerializer implements Serializer {

  private static final int SCHEMA_ID = 10;

  /** blockLength(2) + templateId(2) + schemaId(2) + version(2)。 */
  private static final int HEADER_LENGTH = 8;

  private static final long NULL_LONG = Long.MIN_VALUE;
  private static final long NULL_NANOS = -1L;
  private static final int NULL_INT = Integer.MIN_VALUE;
  private static final byte[] EMPTY_BYTES = new byte[0];

  private static final ThreadLocal<EncodeContext> ENCODE =
      ThreadLocal.withInitial(EncodeContext::new);
  private static final ThreadLocal<DecodeContext> DECODE =
      ThreadLocal.withInitial(DecodeContext::new);

  @Override
  public <T> byte[] encode(final T object) {
    final EncodeContext ctx = ENCODE.get();
    final ExpandableArrayBuffer buffer = ctx.buffer;
    ctx.header.wrap(buffer, 0);
    ctx.header.schemaId(SCHEMA_ID).version(0);
    final int limit = encode(ctx, buffer, object);
    return Arrays.copyOf(buffer.byteArray(), limit);
  }

  private int encode(
      final EncodeContext ctx, final ExpandableArrayBuffer buffer, final Object object) {
    if (object instanceof final AppendRequest request) {
      final AppendRequestEncoder encoder = ctx.appendRequest;
      ctx.header.templateId(AppendRequestEncoder.TEMPLATE_ID).blockLength(encoder.sbeBlockLength());
      encoder.wrap(buffer, HEADER_LENGTH);
      encoder
          .term(request.term())
          .prevLogIndex(request.prevLogIndex())
          .prevLogTerm(request.prevLogTerm())
          .commitIndex(request.commitIndex());
      final AppendRequestEncoder.EntriesEncoder entries =
          encoder.entriesCount(request.entries().size());
      for (final PersistedRaftRecord record : request.entries()) {
        entries
            .next()
            .entryTerm(record.term())
            .entryIndex(record.index())
            .entryAsqn(record.asqn())
            .entryChecksum(record.checksum());
        final byte[] data = toArray(record.data());
        entries.putEntryData(data, 0, data.length);
      }
      encoder.leader(request.leader().id());
      return HEADER_LENGTH + encoder.encodedLength();
    }
    if (object instanceof final VersionedAppendRequest request) {
      final VersionedAppendRequestEncoder encoder = ctx.versionedAppendRequest;
      ctx.header
          .templateId(VersionedAppendRequestEncoder.TEMPLATE_ID)
          .blockLength(encoder.sbeBlockLength());
      encoder.wrap(buffer, HEADER_LENGTH);
      encoder
          .version(request.version())
          .term(request.term())
          .prevLogIndex(request.prevLogIndex())
          .prevLogTerm(request.prevLogTerm())
          .commitIndex(request.commitIndex());
      final VersionedAppendRequestEncoder.EntriesEncoder entries =
          encoder.entriesCount(request.entries().size());
      for (final ReplicatableJournalRecord record : request.entries()) {
        entries
            .next()
            .entryTerm(record.term())
            .entryIndex(record.index())
            .entryChecksum(record.checksum());
        final byte[] data = record.serializedJournalRecord();
        entries.putEntryData(data, 0, data == null ? 0 : data.length);
      }
      encoder.leader(request.leader().id());
      return HEADER_LENGTH + encoder.encodedLength();
    }
    if (object instanceof final AppendResponse response) {
      final AppendResponseEncoder encoder = ctx.appendResponse;
      ctx.header
          .templateId(AppendResponseEncoder.TEMPLATE_ID)
          .blockLength(encoder.sbeBlockLength());
      encoder.wrap(buffer, HEADER_LENGTH);
      setStatus(encoder::status, encoder::errorType, encoder::errorMessage, response);
      encoder
          .term(response.term())
          .succeeded((byte) (response.succeeded() ? 1 : 0))
          .lastLogIndex(response.lastLogIndex())
          .lastSnapshotIndex(response.lastSnapshotIndex())
          .configurationIndex(response.configurationIndex());
      return HEADER_LENGTH + encoder.encodedLength();
    }
    if (object instanceof final PollRequest request) {
      final PollRequestEncoder encoder = ctx.pollRequest;
      ctx.header.templateId(PollRequestEncoder.TEMPLATE_ID).blockLength(encoder.sbeBlockLength());
      encoder.wrap(buffer, HEADER_LENGTH);
      encoder
          .term(request.term())
          .lastLogIndex(request.lastLogIndex())
          .lastLogTerm(request.lastLogTerm())
          .candidate(request.candidate().id());
      return HEADER_LENGTH + encoder.encodedLength();
    }
    if (object instanceof final PollResponse response) {
      final PollResponseEncoder encoder = ctx.pollResponse;
      ctx.header.templateId(PollResponseEncoder.TEMPLATE_ID).blockLength(encoder.sbeBlockLength());
      encoder.wrap(buffer, HEADER_LENGTH);
      setStatus(encoder::status, encoder::errorType, encoder::errorMessage, response);
      encoder.term(response.term()).accepted((byte) (response.accepted() ? 1 : 0));
      return HEADER_LENGTH + encoder.encodedLength();
    }
    if (object instanceof final VoteRequest request) {
      final VoteRequestEncoder encoder = ctx.voteRequest;
      ctx.header.templateId(VoteRequestEncoder.TEMPLATE_ID).blockLength(encoder.sbeBlockLength());
      encoder.wrap(buffer, HEADER_LENGTH);
      encoder
          .term(request.term())
          .lastLogIndex(request.lastLogIndex())
          .lastLogTerm(request.lastLogTerm())
          .candidate(request.candidate().id());
      return HEADER_LENGTH + encoder.encodedLength();
    }
    if (object instanceof final VoteResponse response) {
      final VoteResponseEncoder encoder = ctx.voteResponse;
      ctx.header.templateId(VoteResponseEncoder.TEMPLATE_ID).blockLength(encoder.sbeBlockLength());
      encoder.wrap(buffer, HEADER_LENGTH);
      setStatus(encoder::status, encoder::errorType, encoder::errorMessage, response);
      encoder.term(response.term()).voted((byte) (response.voted() ? 1 : 0));
      return HEADER_LENGTH + encoder.encodedLength();
    }
    if (object instanceof final InstallRequest request) {
      final InstallRequestEncoder encoder = ctx.installRequest;
      ctx.header
          .templateId(InstallRequestEncoder.TEMPLATE_ID)
          .blockLength(encoder.sbeBlockLength());
      encoder.wrap(buffer, HEADER_LENGTH);
      encoder
          .currentTerm(request.currentTerm())
          .index(request.index())
          .term(request.term())
          .version(request.version())
          .initial((byte) (request.isInitial() ? 1 : 0))
          .complete((byte) (request.complete() ? 1 : 0))
          .leader(request.leader().id());
      putBlob(encoder::putChunkId, request.chunkId());
      putBlob(encoder::putNextChunkId, request.nextChunkId());
      putBlob(encoder::putData, request.data());
      return HEADER_LENGTH + encoder.encodedLength();
    }
    if (object instanceof final InstallResponse response) {
      final InstallResponseEncoder encoder = ctx.installResponse;
      ctx.header
          .templateId(InstallResponseEncoder.TEMPLATE_ID)
          .blockLength(encoder.sbeBlockLength());
      encoder.wrap(buffer, HEADER_LENGTH);
      setStatus(encoder::status, encoder::errorType, encoder::errorMessage, response);
      encoder.preferredChunkSize(response.preferredChunkSize());
      return HEADER_LENGTH + encoder.encodedLength();
    }
    if (object instanceof final ConfigureRequest request) {
      final ConfigureRequestEncoder encoder = ctx.configureRequest;
      ctx.header
          .templateId(ConfigureRequestEncoder.TEMPLATE_ID)
          .blockLength(encoder.sbeBlockLength());
      encoder.wrap(buffer, HEADER_LENGTH);
      encoder.term(request.term()).index(request.index()).timestamp(request.timestamp());
      final ConfigureRequestEncoder.MembersEncoder members =
          encoder.membersCount(request.newMembers().size());
      for (final RaftMember member : request.newMembers()) {
        final ConfigureRequestEncoder.MembersEncoder next = members.next();
        next.memberId(member.memberId().id());
        next.memberType(MemberType.valueOf(member.getType().name()));
        next.updatedEpochSecond(member.getLastUpdated().getEpochSecond());
        next.updatedNano(member.getLastUpdated().getNano());
      }
      final ConfigureRequestEncoder.OldMembersEncoder oldMembers =
          encoder.oldMembersCount(request.oldMembers().size());
      for (final RaftMember member : request.oldMembers()) {
        final ConfigureRequestEncoder.OldMembersEncoder next = oldMembers.next();
        next.memberId(member.memberId().id());
        next.memberType(MemberType.valueOf(member.getType().name()));
        next.updatedEpochSecond(member.getLastUpdated().getEpochSecond());
        next.updatedNano(member.getLastUpdated().getNano());
      }
      encoder.leader(request.leader().id());
      return HEADER_LENGTH + encoder.encodedLength();
    }
    if (object instanceof final ReconfigureRequest request) {
      final ReconfigureRequestEncoder encoder = ctx.reconfigureRequest;
      ctx.header
          .templateId(ReconfigureRequestEncoder.TEMPLATE_ID)
          .blockLength(encoder.sbeBlockLength());
      encoder.wrap(buffer, HEADER_LENGTH);
      encoder.index(request.index()).term(request.term());
      final ReconfigureRequestEncoder.MembersEncoder members =
          encoder.membersCount(request.members().size());
      for (final RaftMember member : request.members()) {
        final ReconfigureRequestEncoder.MembersEncoder next = members.next();
        next.memberId(member.memberId().id());
        next.memberType(MemberType.valueOf(member.getType().name()));
        next.updatedEpochSecond(member.getLastUpdated().getEpochSecond());
        next.updatedNano(member.getLastUpdated().getNano());
      }
      encoder.from(request.from().id());
      return HEADER_LENGTH + encoder.encodedLength();
    }
    if (object instanceof final ForceConfigureRequest request) {
      final ForceConfigureRequestEncoder encoder = ctx.forceConfigureRequest;
      ctx.header
          .templateId(ForceConfigureRequestEncoder.TEMPLATE_ID)
          .blockLength(encoder.sbeBlockLength());
      encoder.wrap(buffer, HEADER_LENGTH);
      encoder.term(request.term()).index(request.index()).timestamp(request.timestamp());
      final ForceConfigureRequestEncoder.MembersEncoder members =
          encoder.membersCount(request.newMembers().size());
      for (final RaftMember member : request.newMembers()) {
        final ForceConfigureRequestEncoder.MembersEncoder next = members.next();
        next.memberId(member.memberId().id());
        next.memberType(MemberType.valueOf(member.getType().name()));
        next.updatedEpochSecond(member.getLastUpdated().getEpochSecond());
        next.updatedNano(member.getLastUpdated().getNano());
      }
      encoder.from(request.from().id());
      return HEADER_LENGTH + encoder.encodedLength();
    }
    if (object instanceof final JoinRequest request) {
      final JoinRequestEncoder encoder = ctx.joinRequest;
      ctx.header.templateId(JoinRequestEncoder.TEMPLATE_ID).blockLength(encoder.sbeBlockLength());
      encoder.wrap(buffer, HEADER_LENGTH);
      putMember(
          encoder::memberId,
          encoder::memberType,
          encoder::updatedEpochSecond,
          encoder::updatedNano,
          request.joiningMember());
      return HEADER_LENGTH + encoder.encodedLength();
    }
    if (object instanceof final LeaveRequest request) {
      final LeaveRequestEncoder encoder = ctx.leaveRequest;
      ctx.header.templateId(LeaveRequestEncoder.TEMPLATE_ID).blockLength(encoder.sbeBlockLength());
      encoder.wrap(buffer, HEADER_LENGTH);
      putMember(
          encoder::memberId,
          encoder::memberType,
          encoder::updatedEpochSecond,
          encoder::updatedNano,
          request.leavingMember());
      return HEADER_LENGTH + encoder.encodedLength();
    }
    if (object instanceof final ConfigureResponse response) {
      final ConfigureResponseEncoder encoder = ctx.configureResponse;
      ctx.header
          .templateId(ConfigureResponseEncoder.TEMPLATE_ID)
          .blockLength(encoder.sbeBlockLength());
      encoder.wrap(buffer, HEADER_LENGTH);
      setStatus(encoder::status, encoder::errorType, encoder::errorMessage, response);
      return HEADER_LENGTH + encoder.encodedLength();
    }
    if (object instanceof final ReconfigureResponse response) {
      final ReconfigureResponseEncoder encoder = ctx.reconfigureResponse;
      ctx.header
          .templateId(ReconfigureResponseEncoder.TEMPLATE_ID)
          .blockLength(encoder.sbeBlockLength());
      encoder.wrap(buffer, HEADER_LENGTH);
      // 注意: 组在 errorMessage 之前, 变长字段必须最后写(SBE limit 顺序语义)
      encoder
          .status(status(response.status()))
          .errorType(
              response.error() == null
                  ? ErrorType.NULL_VAL
                  : ErrorType.valueOf(response.error().type().name()));
      encoder.index(response.index()).term(response.term()).timestamp(response.timestamp());
      final ReconfigureResponseEncoder.MembersEncoder members =
          encoder.membersCount(response.members().size());
      for (final RaftMember member : response.members()) {
        final ReconfigureResponseEncoder.MembersEncoder next = members.next();
        next.memberId(member.memberId().id());
        next.memberType(MemberType.valueOf(member.getType().name()));
        next.updatedEpochSecond(member.getLastUpdated().getEpochSecond());
        next.updatedNano(member.getLastUpdated().getNano());
      }
      encoder.errorMessage(
          response.error() == null || response.error().message() == null
              ? ""
              : response.error().message());
      return HEADER_LENGTH + encoder.encodedLength();
    }
    if (object instanceof final TransferRequest request) {
      final TransferRequestEncoder encoder = ctx.transferRequest;
      ctx.header
          .templateId(TransferRequestEncoder.TEMPLATE_ID)
          .blockLength(encoder.sbeBlockLength());
      encoder.wrap(buffer, HEADER_LENGTH);
      encoder.member(request.member().id());
      return HEADER_LENGTH + encoder.encodedLength();
    }
    if (object instanceof final TransferResponse response) {
      final TransferResponseEncoder encoder = ctx.transferResponse;
      ctx.header
          .templateId(TransferResponseEncoder.TEMPLATE_ID)
          .blockLength(encoder.sbeBlockLength());
      encoder.wrap(buffer, HEADER_LENGTH);
      setStatus(encoder::status, encoder::errorType, encoder::errorMessage, response);
      return HEADER_LENGTH + encoder.encodedLength();
    }
    if (object instanceof final JoinResponse response) {
      final JoinResponseEncoder encoder = ctx.joinResponse;
      ctx.header.templateId(JoinResponseEncoder.TEMPLATE_ID).blockLength(encoder.sbeBlockLength());
      encoder.wrap(buffer, HEADER_LENGTH);
      setStatus(encoder::status, encoder::errorType, encoder::errorMessage, response);
      return HEADER_LENGTH + encoder.encodedLength();
    }
    if (object instanceof final LeaveResponse response) {
      final LeaveResponseEncoder encoder = ctx.leaveResponse;
      ctx.header.templateId(LeaveResponseEncoder.TEMPLATE_ID).blockLength(encoder.sbeBlockLength());
      encoder.wrap(buffer, HEADER_LENGTH);
      setStatus(encoder::status, encoder::errorType, encoder::errorMessage, response);
      return HEADER_LENGTH + encoder.encodedLength();
    }
    if (object instanceof final ForceConfigureResponse response) {
      final ForceConfigureResponseEncoder encoder = ctx.forceConfigureResponse;
      ctx.header
          .templateId(ForceConfigureResponseEncoder.TEMPLATE_ID)
          .blockLength(encoder.sbeBlockLength());
      encoder.wrap(buffer, HEADER_LENGTH);
      setStatus(encoder::status, encoder::errorType, encoder::errorMessage, response);
      encoder.index(response.index()).term(response.term());
      return HEADER_LENGTH + encoder.encodedLength();
    }
    if (object instanceof final TimeoutNowRequest request) {
      final TimeoutNowRequestEncoder encoder = ctx.timeoutNowRequest;
      ctx.header
          .templateId(TimeoutNowRequestEncoder.TEMPLATE_ID)
          .blockLength(encoder.sbeBlockLength());
      encoder.wrap(buffer, HEADER_LENGTH);
      encoder.term(request.term()).leader(request.leader().id());
      return HEADER_LENGTH + encoder.encodedLength();
    }
    if (object instanceof final TimeoutNowResponse response) {
      final TimeoutNowResponseEncoder encoder = ctx.timeoutNowResponse;
      ctx.header
          .templateId(TimeoutNowResponseEncoder.TEMPLATE_ID)
          .blockLength(encoder.sbeBlockLength());
      encoder.wrap(buffer, HEADER_LENGTH);
      setStatus(encoder::status, encoder::errorType, encoder::errorMessage, response);
      return HEADER_LENGTH + encoder.encodedLength();
    }
    if (object instanceof final LeadershipTransferInitiateRequest request) {
      final LeadershipTransferInitiateRequestEncoder encoder =
          ctx.leadershipTransferInitiateRequest;
      ctx.header
          .templateId(LeadershipTransferInitiateRequestEncoder.TEMPLATE_ID)
          .blockLength(encoder.sbeBlockLength());
      encoder.wrap(buffer, HEADER_LENGTH);
      encoder
          .coordinatorConfigVersion(request.coordinatorConfigVersion())
          .correlationId(request.correlationId())
          .replicationLagThreshold(
              request.replicationLagThreshold() == null
                  ? NULL_LONG
                  : request.replicationLagThreshold())
          .replicationTimeoutNanos(
              request.replicationTimeout() == null
                  ? NULL_NANOS
                  : request.replicationTimeout().toNanos())
          .maxTransferAttempts(
              request.maxTransferAttempts() == null ? NULL_INT : request.maxTransferAttempts())
          .desiredLeader(request.desiredLeader().id())
          .coordinator(request.coordinator().id());
      return HEADER_LENGTH + encoder.encodedLength();
    }
    if (object instanceof final LeadershipTransferInitiateResponse response) {
      final LeadershipTransferInitiateResponseEncoder encoder =
          ctx.leadershipTransferInitiateResponse;
      ctx.header
          .templateId(LeadershipTransferInitiateResponseEncoder.TEMPLATE_ID)
          .blockLength(encoder.sbeBlockLength());
      encoder.wrap(buffer, HEADER_LENGTH);
      setStatus(encoder::status, encoder::errorType, encoder::errorMessage, response);
      encoder.rejectionReason(
          response.rejectionReason() == null
              ? TransferResultCode.NULL_VAL
              : TransferResultCode.valueOf(response.rejectionReason().name()));
      return HEADER_LENGTH + encoder.encodedLength();
    }
    if (object instanceof final LeadershipTransferResultRequest request) {
      final LeadershipTransferResultRequestEncoder encoder = ctx.leadershipTransferResultRequest;
      ctx.header
          .templateId(LeadershipTransferResultRequestEncoder.TEMPLATE_ID)
          .blockLength(encoder.sbeBlockLength());
      encoder.wrap(buffer, HEADER_LENGTH);
      encoder
          .correlationId(request.correlationId())
          .result(TransferResultCode.valueOf(request.result().name()))
          .leader(request.leader().id())
          .desiredLeader(request.desiredLeader().id());
      return HEADER_LENGTH + encoder.encodedLength();
    }
    if (object instanceof final LeadershipTransferResultResponse response) {
      final LeadershipTransferResultResponseEncoder encoder = ctx.leadershipTransferResultResponse;
      ctx.header
          .templateId(LeadershipTransferResultResponseEncoder.TEMPLATE_ID)
          .blockLength(encoder.sbeBlockLength());
      encoder.wrap(buffer, HEADER_LENGTH);
      setStatus(encoder::status, encoder::errorType, encoder::errorMessage, response);
      return HEADER_LENGTH + encoder.encodedLength();
    }
    throw new IllegalArgumentException("未注册的 Raft 协议报文类型: " + object.getClass().getName());
  }

  @Override
  @SuppressWarnings("unchecked")
  public <T> T decode(final byte[] bytes) {
    final DecodeContext ctx = DECODE.get();
    ctx.buffer.wrap(bytes);
    final UnsafeBuffer buffer = ctx.buffer;
    ctx.header.wrap(buffer, 0);
    final int blockLength = ctx.header.blockLength();
    final int templateId = ctx.header.templateId();
    final Object decoded =
        switch (templateId) {
          case AppendRequestEncoder.TEMPLATE_ID -> decodeAppendRequest(ctx, buffer, blockLength);
          case VersionedAppendRequestEncoder.TEMPLATE_ID ->
              decodeVersionedAppendRequest(ctx, buffer, blockLength);
          case AppendResponseEncoder.TEMPLATE_ID -> {
            final AppendResponseDecoder decoder =
                ctx.appendResponse.wrap(buffer, HEADER_LENGTH, blockLength, 0);
            yield new AppendResponse(
                status(decoder.status()),
                error(decoder.errorType(), decoder.errorMessage()),
                decoder.term(),
                decoder.succeeded() == 1,
                decoder.lastLogIndex(),
                decoder.lastSnapshotIndex(),
                decoder.configurationIndex());
          }
          case PollRequestEncoder.TEMPLATE_ID -> {
            final PollRequestDecoder decoder =
                ctx.pollRequest.wrap(buffer, HEADER_LENGTH, blockLength, 0);
            yield new PollRequest(
                decoder.term(), decoder.candidate(), decoder.lastLogIndex(), decoder.lastLogTerm());
          }
          case PollResponseEncoder.TEMPLATE_ID -> {
            final PollResponseDecoder decoder =
                ctx.pollResponse.wrap(buffer, HEADER_LENGTH, blockLength, 0);
            yield new PollResponse(
                status(decoder.status()),
                error(decoder.errorType(), decoder.errorMessage()),
                decoder.term(),
                decoder.accepted() == 1);
          }
          case VoteRequestEncoder.TEMPLATE_ID -> {
            final VoteRequestDecoder decoder =
                ctx.voteRequest.wrap(buffer, HEADER_LENGTH, blockLength, 0);
            yield new VoteRequest(
                decoder.term(), decoder.candidate(), decoder.lastLogIndex(), decoder.lastLogTerm());
          }
          case VoteResponseEncoder.TEMPLATE_ID -> {
            final VoteResponseDecoder decoder =
                ctx.voteResponse.wrap(buffer, HEADER_LENGTH, blockLength, 0);
            yield new VoteResponse(
                status(decoder.status()),
                error(decoder.errorType(), decoder.errorMessage()),
                decoder.term(),
                decoder.voted() == 1);
          }
          case InstallRequestEncoder.TEMPLATE_ID -> {
            final InstallRequestDecoder decoder =
                ctx.installRequest.wrap(buffer, HEADER_LENGTH, blockLength, 0);
            yield new InstallRequest(
                decoder.currentTerm(),
                MemberId.from(decoder.leader()),
                decoder.index(),
                decoder.term(),
                decoder.version(),
                wrapOrNull(decoder.chunkIdLength(), decoder::wrapChunkId),
                wrapOrNull(decoder.nextChunkIdLength(), decoder::wrapNextChunkId),
                wrapOrNull(decoder.dataLength(), decoder::wrapData),
                decoder.initial() == 1,
                decoder.complete() == 1);
          }
          case InstallResponseEncoder.TEMPLATE_ID -> {
            final InstallResponseDecoder decoder =
                ctx.installResponse.wrap(buffer, HEADER_LENGTH, blockLength, 0);
            yield new InstallResponse(
                status(decoder.status()),
                error(decoder.errorType(), decoder.errorMessage()),
                decoder.preferredChunkSize());
          }
          case ConfigureRequestEncoder.TEMPLATE_ID -> {
            final ConfigureRequestDecoder decoder =
                ctx.configureRequest.wrap(buffer, HEADER_LENGTH, blockLength, 0);
            final Collection<RaftMember> members = new ArrayList<>();
            for (final ConfigureRequestDecoder.MembersDecoder member : decoder.members()) {
              members.add(
                  toMember(
                      member.memberId(),
                      member.memberType(),
                      member.updatedEpochSecond(),
                      member.updatedNano()));
            }
            final Collection<RaftMember> oldMembers = new ArrayList<>();
            for (final ConfigureRequestDecoder.OldMembersDecoder member : decoder.oldMembers()) {
              oldMembers.add(
                  toMember(
                      member.memberId(),
                      member.memberType(),
                      member.updatedEpochSecond(),
                      member.updatedNano()));
            }
            yield new ConfigureRequest(
                decoder.term(),
                decoder.leader(),
                decoder.index(),
                decoder.timestamp(),
                members,
                oldMembers);
          }
          case ConfigureResponseEncoder.TEMPLATE_ID -> {
            final ConfigureResponseDecoder decoder =
                ctx.configureResponse.wrap(buffer, HEADER_LENGTH, blockLength, 0);
            yield new ConfigureResponse(
                status(decoder.status()), error(decoder.errorType(), decoder.errorMessage()));
          }
          case ReconfigureRequestEncoder.TEMPLATE_ID -> {
            final ReconfigureRequestDecoder decoder =
                ctx.reconfigureRequest.wrap(buffer, HEADER_LENGTH, blockLength, 0);
            final Collection<RaftMember> members = new ArrayList<>();
            for (final ReconfigureRequestDecoder.MembersDecoder member : decoder.members()) {
              members.add(
                  toMember(
                      member.memberId(),
                      member.memberType(),
                      member.updatedEpochSecond(),
                      member.updatedNano()));
            }
            yield new ReconfigureRequest(members, decoder.index(), decoder.term(), decoder.from());
          }
          case ReconfigureResponseEncoder.TEMPLATE_ID -> {
            final ReconfigureResponseDecoder decoder =
                ctx.reconfigureResponse.wrap(buffer, HEADER_LENGTH, blockLength, 0);
            final RaftResponse.Status responseStatus = status(decoder.status());
            final long responseIndex = decoder.index();
            final long responseTerm = decoder.term();
            final long responseTimestamp = decoder.timestamp();
            final Collection<RaftMember> members = new ArrayList<>();
            for (final ReconfigureResponseDecoder.MembersDecoder member : decoder.members()) {
              members.add(
                  toMember(
                      member.memberId(),
                      member.memberType(),
                      member.updatedEpochSecond(),
                      member.updatedNano()));
            }
            yield new ReconfigureResponse(
                responseStatus,
                error(decoder.errorType(), decoder.errorMessage()),
                responseIndex,
                responseTerm,
                responseTimestamp,
                members);
          }
          case TransferRequestEncoder.TEMPLATE_ID -> {
            final TransferRequestDecoder decoder =
                ctx.transferRequest.wrap(buffer, HEADER_LENGTH, blockLength, 0);
            yield new TransferRequest(MemberId.from(decoder.member()));
          }
          case TransferResponseEncoder.TEMPLATE_ID -> {
            final TransferResponseDecoder decoder =
                ctx.transferResponse.wrap(buffer, HEADER_LENGTH, blockLength, 0);
            yield new TransferResponse(
                status(decoder.status()), error(decoder.errorType(), decoder.errorMessage()));
          }
          case JoinRequestEncoder.TEMPLATE_ID -> {
            final JoinRequestDecoder decoder =
                ctx.joinRequest.wrap(buffer, HEADER_LENGTH, blockLength, 0);
            yield new JoinRequest(
                toMember(
                    decoder.memberId(),
                    decoder.memberType(),
                    decoder.updatedEpochSecond(),
                    decoder.updatedNano()));
          }
          case JoinResponseEncoder.TEMPLATE_ID -> {
            final JoinResponseDecoder decoder =
                ctx.joinResponse.wrap(buffer, HEADER_LENGTH, blockLength, 0);
            final JoinResponse.Builder joinBuilder =
                JoinResponse.builder().withStatus(status(decoder.status()));
            final RaftError joinError = error(decoder.errorType(), decoder.errorMessage());
            if (joinError != null) {
              joinBuilder.withError(joinError);
            }
            yield joinBuilder.build();
          }
          case LeaveRequestEncoder.TEMPLATE_ID -> {
            final LeaveRequestDecoder decoder =
                ctx.leaveRequest.wrap(buffer, HEADER_LENGTH, blockLength, 0);
            yield new LeaveRequest(
                toMember(
                    decoder.memberId(),
                    decoder.memberType(),
                    decoder.updatedEpochSecond(),
                    decoder.updatedNano()));
          }
          case LeaveResponseEncoder.TEMPLATE_ID -> {
            final LeaveResponseDecoder decoder =
                ctx.leaveResponse.wrap(buffer, HEADER_LENGTH, blockLength, 0);
            final LeaveResponse.Builder leaveBuilder =
                LeaveResponse.builder().withStatus(status(decoder.status()));
            final RaftError leaveError = error(decoder.errorType(), decoder.errorMessage());
            if (leaveError != null) {
              leaveBuilder.withError(leaveError);
            }
            yield leaveBuilder.build();
          }
          case ForceConfigureRequestEncoder.TEMPLATE_ID -> {
            final ForceConfigureRequestDecoder decoder =
                ctx.forceConfigureRequest.wrap(buffer, HEADER_LENGTH, blockLength, 0);
            final Set<RaftMember> members = new LinkedHashSet<>();
            for (final ForceConfigureRequestDecoder.MembersDecoder member : decoder.members()) {
              members.add(
                  toMember(
                      member.memberId(),
                      member.memberType(),
                      member.updatedEpochSecond(),
                      member.updatedNano()));
            }
            yield new ForceConfigureRequest(
                decoder.term(), decoder.index(), decoder.timestamp(), members, decoder.from());
          }
          case ForceConfigureResponseEncoder.TEMPLATE_ID -> {
            final ForceConfigureResponseDecoder decoder =
                ctx.forceConfigureResponse.wrap(buffer, HEADER_LENGTH, blockLength, 0);
            yield new ForceConfigureResponse(
                status(decoder.status()),
                error(decoder.errorType(), decoder.errorMessage()),
                decoder.index(),
                decoder.term());
          }
          case TimeoutNowRequestEncoder.TEMPLATE_ID -> {
            final TimeoutNowRequestDecoder decoder =
                ctx.timeoutNowRequest.wrap(buffer, HEADER_LENGTH, blockLength, 0);
            yield new TimeoutNowRequest(decoder.term(), MemberId.from(decoder.leader()));
          }
          case TimeoutNowResponseEncoder.TEMPLATE_ID -> {
            final TimeoutNowResponseDecoder decoder =
                ctx.timeoutNowResponse.wrap(buffer, HEADER_LENGTH, blockLength, 0);
            yield new TimeoutNowResponse(
                status(decoder.status()), error(decoder.errorType(), decoder.errorMessage()));
          }
          case LeadershipTransferInitiateRequestEncoder.TEMPLATE_ID -> {
            final LeadershipTransferInitiateRequestDecoder decoder =
                ctx.leadershipTransferInitiateRequest.wrap(buffer, HEADER_LENGTH, blockLength, 0);
            final long lag = decoder.replicationLagThreshold();
            final long timeoutNanos = decoder.replicationTimeoutNanos();
            final int attempts = decoder.maxTransferAttempts();
            yield new LeadershipTransferInitiateRequest(
                MemberId.from(decoder.desiredLeader()),
                MemberId.from(decoder.coordinator()),
                decoder.coordinatorConfigVersion(),
                decoder.correlationId(),
                lag == NULL_LONG ? null : lag,
                timeoutNanos == NULL_NANOS ? null : Duration.ofNanos(timeoutNanos),
                attempts == NULL_INT ? null : attempts);
          }
          case LeadershipTransferInitiateResponseEncoder.TEMPLATE_ID -> {
            final LeadershipTransferInitiateResponseDecoder decoder =
                ctx.leadershipTransferInitiateResponse.wrap(buffer, HEADER_LENGTH, blockLength, 0);
            yield new LeadershipTransferInitiateResponse(
                status(decoder.status()),
                error(decoder.errorType(), decoder.errorMessage()),
                decoder.rejectionReason() == TransferResultCode.NULL_VAL
                    ? null
                    : LeadershipTransferResult.valueOf(decoder.rejectionReason().name()));
          }
          case LeadershipTransferResultRequestEncoder.TEMPLATE_ID -> {
            final LeadershipTransferResultRequestDecoder decoder =
                ctx.leadershipTransferResultRequest.wrap(buffer, HEADER_LENGTH, blockLength, 0);
            yield new LeadershipTransferResultRequest(
                MemberId.from(decoder.leader()),
                MemberId.from(decoder.desiredLeader()),
                LeadershipTransferResult.valueOf(decoder.result().name()),
                decoder.correlationId());
          }
          case LeadershipTransferResultResponseEncoder.TEMPLATE_ID -> {
            final LeadershipTransferResultResponseDecoder decoder =
                ctx.leadershipTransferResultResponse.wrap(buffer, HEADER_LENGTH, blockLength, 0);
            yield new LeadershipTransferResultResponse(
                status(decoder.status()), error(decoder.errorType(), decoder.errorMessage()));
          }
          default -> throw new IllegalArgumentException("未知的 Raft 协议模板 id: " + templateId);
        };
    return (T) decoded;
  }

  // ==================== 解码辅助 ====================

  private AppendRequest decodeAppendRequest(
      final DecodeContext ctx, final UnsafeBuffer buffer, final int blockLength) {
    final AppendRequestDecoder decoder =
        ctx.appendRequest.wrap(buffer, HEADER_LENGTH, blockLength, 0);
    final AppendRequestDecoder.EntriesDecoder group = decoder.entries();
    final List<PersistedRaftRecord> entries = new ArrayList<>(group.count());
    while (group.hasNext()) {
      group.next();
      entries.add(
          new PersistedRaftRecord(
              group.entryTerm(),
              group.entryIndex(),
              group.entryAsqn(),
              group.entryChecksum(),
              copyOf(group.entryDataLength(), group::wrapEntryData)));
    }
    return new AppendRequest(
        decoder.term(),
        decoder.leader(),
        decoder.prevLogIndex(),
        decoder.prevLogTerm(),
        entries,
        decoder.commitIndex());
  }

  private VersionedAppendRequest decodeVersionedAppendRequest(
      final DecodeContext ctx, final UnsafeBuffer buffer, final int blockLength) {
    final VersionedAppendRequestDecoder decoder =
        ctx.versionedAppendRequest.wrap(buffer, HEADER_LENGTH, blockLength, 0);
    final VersionedAppendRequestDecoder.EntriesDecoder group = decoder.entries();
    final List<ReplicatableJournalRecord> entries = new ArrayList<>(group.count());
    while (group.hasNext()) {
      group.next();
      entries.add(
          new ReplicatableJournalRecord(
              group.entryTerm(),
              group.entryIndex(),
              group.entryChecksum(),
              copyOf(group.entryDataLength(), group::wrapEntryData)));
    }
    return new VersionedAppendRequest(
        decoder.version(),
        decoder.term(),
        decoder.leader(),
        decoder.prevLogIndex(),
        decoder.prevLogTerm(),
        entries,
        decoder.commitIndex());
  }

  // ==================== 通用小工具 ====================

  private static void setStatus(
      final Consumer<ResponseStatus> statusSetter,
      final Consumer<ErrorType> errorSetter,
      final Consumer<String> messageSetter,
      final AbstractRaftResponse response) {
    statusSetter.accept(status(response.status()));
    errorSetter.accept(
        response.error() == null
            ? ErrorType.NULL_VAL
            : ErrorType.valueOf(response.error().type().name()));
    messageSetter.accept(
        response.error() == null || response.error().message() == null
            ? ""
            : response.error().message());
  }

  private static void putMember(
      final Consumer<String> idSetter,
      final Consumer<MemberType> typeSetter,
      final java.util.function.LongConsumer epochSetter,
      final java.util.function.IntConsumer nanoSetter,
      final RaftMember member) {
    idSetter.accept(member.memberId().id());
    typeSetter.accept(MemberType.valueOf(member.getType().name()));
    epochSetter.accept(member.getLastUpdated().getEpochSecond());
    nanoSetter.accept(member.getLastUpdated().getNano());
  }

  private static void putBlob(final BlobSetter setter, final ByteBuffer buffer) {
    if (buffer == null) {
      setter.put(EMPTY_BYTES, 0, 0);
      return;
    }
    final byte[] copy = new byte[buffer.remaining()];
    buffer.duplicate().get(copy);
    setter.put(copy, 0, copy.length);
  }

  /** 变长字节块写入函数面（生成类的 putXxx(byte[], int, int) 签名一致）。 */
  @FunctionalInterface
  private interface BlobSetter {
    void put(byte[] src, int offset, int length);
  }

  private static RaftMember toMember(
      final String memberId, final MemberType type, final long epochSecond, final int nano) {
    return new DefaultRaftMember(
        MemberId.from(memberId),
        type == null ? null : RaftMember.Type.valueOf(type.name()),
        Instant.ofEpochSecond(epochSecond, nano));
  }

  private static RaftResponse.Status status(final ResponseStatus status) {
    return status == null ? null : RaftResponse.Status.valueOf(status.name());
  }

  private static ResponseStatus status(final RaftResponse.Status status) {
    return ResponseStatus.valueOf(status.name());
  }

  private static RaftError error(final ErrorType type, final String message) {
    if (type == null || type == ErrorType.NULL_VAL) {
      return null;
    }
    return new RaftError(RaftError.Type.valueOf(type.name()), message == null ? "" : message);
  }

  private static byte[] toArray(final DirectBuffer buffer) {
    if (buffer == null) {
      return new byte[0];
    }
    final byte[] copy = new byte[buffer.capacity()];
    buffer.getBytes(0, copy);
    return copy;
  }

  /** 包裹变长数据视图并拷贝出定长字节（flyweight 原地读取，仅此处一次载荷拷贝）。 */
  private static byte[] copyOf(final int length, final Consumer<DirectBuffer> wrapper) {
    if (length <= 0) {
      return new byte[0];
    }
    final UnsafeBuffer view = new UnsafeBuffer(new byte[0]);
    wrapper.accept(view);
    final byte[] copy = new byte[length];
    view.getBytes(0, copy);
    return copy;
  }

  private static ByteBuffer wrapOrNull(final int length, final Consumer<DirectBuffer> wrapper) {
    if (length <= 0) {
      return null;
    }
    return ByteBuffer.wrap(copyOf(length, wrapper));
  }

  private static final class EncodeContext {
    final ExpandableArrayBuffer buffer = new ExpandableArrayBuffer(512);
    final MessageHeaderEncoder header = new MessageHeaderEncoder();
    final AppendRequestEncoder appendRequest = new AppendRequestEncoder();
    final VersionedAppendRequestEncoder versionedAppendRequest =
        new VersionedAppendRequestEncoder();
    final AppendResponseEncoder appendResponse = new AppendResponseEncoder();
    final PollRequestEncoder pollRequest = new PollRequestEncoder();
    final PollResponseEncoder pollResponse = new PollResponseEncoder();
    final VoteRequestEncoder voteRequest = new VoteRequestEncoder();
    final VoteResponseEncoder voteResponse = new VoteResponseEncoder();
    final InstallRequestEncoder installRequest = new InstallRequestEncoder();
    final InstallResponseEncoder installResponse = new InstallResponseEncoder();
    final ConfigureRequestEncoder configureRequest = new ConfigureRequestEncoder();
    final ConfigureResponseEncoder configureResponse = new ConfigureResponseEncoder();
    final ReconfigureRequestEncoder reconfigureRequest = new ReconfigureRequestEncoder();
    final ReconfigureResponseEncoder reconfigureResponse = new ReconfigureResponseEncoder();
    final TransferRequestEncoder transferRequest = new TransferRequestEncoder();
    final TransferResponseEncoder transferResponse = new TransferResponseEncoder();
    final JoinRequestEncoder joinRequest = new JoinRequestEncoder();
    final JoinResponseEncoder joinResponse = new JoinResponseEncoder();
    final LeaveRequestEncoder leaveRequest = new LeaveRequestEncoder();
    final LeaveResponseEncoder leaveResponse = new LeaveResponseEncoder();
    final ForceConfigureRequestEncoder forceConfigureRequest = new ForceConfigureRequestEncoder();
    final ForceConfigureResponseEncoder forceConfigureResponse =
        new ForceConfigureResponseEncoder();
    final TimeoutNowRequestEncoder timeoutNowRequest = new TimeoutNowRequestEncoder();
    final TimeoutNowResponseEncoder timeoutNowResponse = new TimeoutNowResponseEncoder();
    final LeadershipTransferInitiateRequestEncoder leadershipTransferInitiateRequest =
        new LeadershipTransferInitiateRequestEncoder();
    final LeadershipTransferInitiateResponseEncoder leadershipTransferInitiateResponse =
        new LeadershipTransferInitiateResponseEncoder();
    final LeadershipTransferResultRequestEncoder leadershipTransferResultRequest =
        new LeadershipTransferResultRequestEncoder();
    final LeadershipTransferResultResponseEncoder leadershipTransferResultResponse =
        new LeadershipTransferResultResponseEncoder();
  }

  private static final class DecodeContext {
    final UnsafeBuffer buffer = new UnsafeBuffer();
    final MessageHeaderDecoder header = new MessageHeaderDecoder();
    final AppendRequestDecoder appendRequest = new AppendRequestDecoder();
    final VersionedAppendRequestDecoder versionedAppendRequest =
        new VersionedAppendRequestDecoder();
    final AppendResponseDecoder appendResponse = new AppendResponseDecoder();
    final PollRequestDecoder pollRequest = new PollRequestDecoder();
    final PollResponseDecoder pollResponse = new PollResponseDecoder();
    final VoteRequestDecoder voteRequest = new VoteRequestDecoder();
    final VoteResponseDecoder voteResponse = new VoteResponseDecoder();
    final InstallRequestDecoder installRequest = new InstallRequestDecoder();
    final InstallResponseDecoder installResponse = new InstallResponseDecoder();
    final ConfigureRequestDecoder configureRequest = new ConfigureRequestDecoder();
    final ConfigureResponseDecoder configureResponse = new ConfigureResponseDecoder();
    final ReconfigureRequestDecoder reconfigureRequest = new ReconfigureRequestDecoder();
    final ReconfigureResponseDecoder reconfigureResponse = new ReconfigureResponseDecoder();
    final TransferRequestDecoder transferRequest = new TransferRequestDecoder();
    final TransferResponseDecoder transferResponse = new TransferResponseDecoder();
    final JoinRequestDecoder joinRequest = new JoinRequestDecoder();
    final JoinResponseDecoder joinResponse = new JoinResponseDecoder();
    final LeaveRequestDecoder leaveRequest = new LeaveRequestDecoder();
    final LeaveResponseDecoder leaveResponse = new LeaveResponseDecoder();
    final ForceConfigureRequestDecoder forceConfigureRequest = new ForceConfigureRequestDecoder();
    final ForceConfigureResponseDecoder forceConfigureResponse =
        new ForceConfigureResponseDecoder();
    final TimeoutNowRequestDecoder timeoutNowRequest = new TimeoutNowRequestDecoder();
    final TimeoutNowResponseDecoder timeoutNowResponse = new TimeoutNowResponseDecoder();
    final LeadershipTransferInitiateRequestDecoder leadershipTransferInitiateRequest =
        new LeadershipTransferInitiateRequestDecoder();
    final LeadershipTransferInitiateResponseDecoder leadershipTransferInitiateResponse =
        new LeadershipTransferInitiateResponseDecoder();
    final LeadershipTransferResultRequestDecoder leadershipTransferResultRequest =
        new LeadershipTransferResultRequestDecoder();
    final LeadershipTransferResultResponseDecoder leadershipTransferResultResponse =
        new LeadershipTransferResultResponseDecoder();
  }
}
