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
package com.anyilanxin.kunpeng.broker.client.jobstream;

import com.anyilanxin.kunpeng.broker.client.jobstream.JobStreamMessages.PushResult;
import com.anyilanxin.kunpeng.broker.client.jobstream.JobStreamMessages.StreamPush;
import com.anyilanxin.kunpeng.broker.client.jobstream.JobStreamMessages.SubscriptionSnapshot;
import com.anyilanxin.kunpeng.broker.client.jobstream.protocol.MessageHeaderDecoder;
import com.anyilanxin.kunpeng.broker.client.jobstream.protocol.MessageHeaderEncoder;
import com.anyilanxin.kunpeng.broker.client.jobstream.protocol.PushResultDecoder;
import com.anyilanxin.kunpeng.broker.client.jobstream.protocol.PushResultEncoder;
import com.anyilanxin.kunpeng.broker.client.jobstream.protocol.StreamPushDecoder;
import com.anyilanxin.kunpeng.broker.client.jobstream.protocol.StreamPushEncoder;
import com.anyilanxin.kunpeng.broker.client.jobstream.protocol.SubscriptionSnapshotDecoder;
import com.anyilanxin.kunpeng.broker.client.jobstream.protocol.SubscriptionSnapshotEncoder;
import com.anyilanxin.kunpeng.protocol.business.impl.record.command.job.JobRecord;
import java.util.ArrayList;
import java.util.List;
import org.agrona.ExpandableArrayBuffer;
import org.agrona.concurrent.UnsafeBuffer;

/**
 * job 流推送 wire 编解码（SBE，schema id=11）：字段契约由 jobstream-schema.xml 显式声明，flyweight 直读直写。
 *
 * <p>消息对象 ↔ SBE 帧的适配层——对外方法签名与消费方（messaging codec 引用）解耦于编码细节； JobRecord 以 structpack 帧作为 blob
 * 段随行（wrap/put 直转 buffer，无中间 byte[] 拷贝）。
 *
 * @author zxuanhong
 * @since 2026.9.0
 */
public final class JobStreamWireCodec {
  private static final int HEADER_LENGTH = MessageHeaderEncoder.ENCODED_LENGTH;

  private JobStreamWireCodec() {}

  // —— SubscriptionSnapshot ——

  public static byte[] encodeSnapshot(final SubscriptionSnapshot snap) {
    final var buffer = new ExpandableArrayBuffer(256);
    final var encoder = new SubscriptionSnapshotEncoder();
    encoder.wrapAndApplyHeader(buffer, 0, new MessageHeaderEncoder());
    encoder.generation(snap.generation());
    final var aggregates = encoder.aggregatesCount(snap.aggregates().size());
    for (final SubscriptionSnapshot.Aggregate aggregate : snap.aggregates()) {
      final var sessions = aggregates.sessionsCount(aggregate.sessions().size());
      for (final SubscriptionSnapshot.Session session : aggregate.sessions()) {
        sessions.next().sessionId(session.sessionId()).worker(session.worker());
      }
      aggregates.jobType(aggregate.jobType());
      aggregates.next();
    }
    return copyOut(buffer, encoder.encodedLength());
  }

  public static SubscriptionSnapshot decodeSnapshot(final byte[] bytes) {
    final var buffer = new UnsafeBuffer(bytes);
    final var decoder = new SubscriptionSnapshotDecoder();
    decoder.wrapAndApplyHeader(buffer, 0, new MessageHeaderDecoder());
    final var aggregates = decoder.aggregates();
    final List<SubscriptionSnapshot.Aggregate> out = new ArrayList<>(aggregates.count());
    while (aggregates.hasNext()) {
      aggregates.next();
      final var sessions = aggregates.sessions();
      final List<SubscriptionSnapshot.Session> members = new ArrayList<>(sessions.count());
      while (sessions.hasNext()) {
        sessions.next();
        members.add(new SubscriptionSnapshot.Session(sessions.sessionId(), sessions.worker()));
      }
      out.add(new SubscriptionSnapshot.Aggregate(aggregates.jobType(), members));
    }
    return new SubscriptionSnapshot(decoder.generation(), out);
  }

  // —— StreamPush / PushResult ——

  public static byte[] encodePush(final StreamPush push) {
    final JobRecord record = push.record();
    final int recordLength = record.getLength();
    final var recordBuffer = new UnsafeBuffer(new byte[recordLength]);
    record.write(recordBuffer, 0);

    final var buffer = new ExpandableArrayBuffer(HEADER_LENGTH + 64 + recordLength);
    final var encoder = new StreamPushEncoder();
    encoder.wrapAndApplyHeader(buffer, 0, new MessageHeaderEncoder());
    encoder
        .sessionId(push.sessionId())
        .jobKey(push.jobKey())
        .partitionId(push.partitionId())
        .deadline(push.deadline())
        .worker(push.worker() == null ? "" : push.worker())
        .putRecord(recordBuffer, 0, recordLength);
    return copyOut(buffer, encoder.encodedLength());
  }

  public static StreamPush decodePush(final byte[] bytes) {
    final var buffer = new UnsafeBuffer(bytes);
    final var decoder = new StreamPushDecoder();
    decoder.wrapAndApplyHeader(buffer, 0, new MessageHeaderDecoder());
    final var recordView = new UnsafeBuffer();
    decoder.wrapRecord(recordView);
    final var record = new JobRecord();
    record.wrap(recordView, 0, recordView.capacity());
    return new StreamPush(
        decoder.sessionId(),
        decoder.worker(),
        decoder.jobKey(),
        decoder.partitionId(),
        decoder.deadline(),
        record);
  }

  public static byte[] encodeResult(final PushResult result) {
    final var buffer = new ExpandableArrayBuffer(64);
    final var encoder = new PushResultEncoder();
    encoder.wrapAndApplyHeader(buffer, 0, new MessageHeaderEncoder());
    encoder.delivered(result.delivered() ? (byte) 1 : 0);
    encoder.reason(result.reason() == null ? "" : result.reason());
    return copyOut(buffer, encoder.encodedLength());
  }

  public static PushResult decodeResult(final byte[] bytes) {
    final var decoder = new PushResultDecoder();
    decoder.wrapAndApplyHeader(new UnsafeBuffer(bytes), 0, new MessageHeaderDecoder());
    return new PushResult(
        decoder.delivered() == 1, decoder.reason().isEmpty() ? null : decoder.reason());
  }

  private static byte[] copyOut(final ExpandableArrayBuffer buffer, final int encodedLength) {
    final byte[] out = new byte[HEADER_LENGTH + encodedLength];
    buffer.getBytes(0, out, 0, out.length);
    return out;
  }
}
