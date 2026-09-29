/*
 * Copyright © 2026 anyilanxin zxh(anyilanxin.com)
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
package com.anyilanxin.kunpeng.protocol.common.encoding;

import com.anyilanxin.kunpeng.protocol.common.ClusterCommonConstant;
import com.anyilanxin.kunpeng.protocol.common.member.JobSubscriptionInfoDecoder;
import com.anyilanxin.kunpeng.protocol.common.member.JobSubscriptionInfoEncoder;
import com.anyilanxin.kunpeng.protocol.common.member.MessageHeaderDecoder;
import com.anyilanxin.kunpeng.protocol.common.member.MessageHeaderEncoder;
import com.anyilanxin.kunpeng.structpack.buffer.BufferReader;
import com.anyilanxin.kunpeng.structpack.buffer.BufferWriter;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Properties;
import org.agrona.DirectBuffer;
import org.agrona.ExpandableArrayBuffer;
import org.agrona.MutableDirectBuffer;
import org.agrona.concurrent.UnsafeBuffer;

/**
 * job 订阅信息实体：网关侧的 job 流订阅快照（generation 代次 + 按 jobType 聚合的稳定会话集合），一个实体一个属性键， 存放于网关成员属性（值为 Base64(SBE
 * 帧)，满足成员属性只能是字符串的约束）。
 *
 * <p>管理侧（网关）原地改实体后 {@link #writeIntoProperties(Properties)}，收集侧（broker）{@link
 * #fromProperties(Properties)}；实体只管把数据放进 properties——是否传播、何时传播由 SWIM 底层自行决定。
 *
 * @author zxuanhong
 * @since 2026.9.0
 */
public final class JobSubscriptionInfo implements BufferReader, BufferWriter {
  /** 成员属性键（统一定义于 {@link ClusterCommonConstant}）。 */
  public static final String PROPERTY_NAME = ClusterCommonConstant.JOB_SUBSCRIPTION_INFO_PROPERTY;

  private static final Base64.Encoder BASE_64_ENCODER = Base64.getEncoder();
  private static final Base64.Decoder BASE_64_DECODER = Base64.getDecoder();

  private final MessageHeaderEncoder headerEncoder = new MessageHeaderEncoder();
  private final MessageHeaderDecoder headerDecoder = new MessageHeaderDecoder();

  private long generation;
  private final List<Aggregate> aggregates = new ArrayList<>();

  /** 单个 jobType 的聚合注册。 */
  public static final class Aggregate {
    public String jobType = "";
    public final List<Session> sessions = new ArrayList<>();
  }

  /** 单条流会话：稳定会话 ID + 归属 worker。 */
  public static final class Session {
    public long sessionId;
    public String worker = "";

    public Session() {}

    public Session(final long sessionId, final String worker) {
      this.sessionId = sessionId;
      this.worker = worker == null ? "" : worker;
    }
  }

  public long getGeneration() {
    return generation;
  }

  public JobSubscriptionInfo setGeneration(final long generation) {
    this.generation = generation;
    return this;
  }

  public List<Aggregate> getAggregates() {
    return aggregates;
  }

  public JobSubscriptionInfo addAggregate(final Aggregate aggregate) {
    aggregates.add(aggregate);
    return this;
  }

  public JobSubscriptionInfo reset() {
    generation = 0L;
    aggregates.clear();
    return this;
  }

  /** 读取成员属性中的实体；缺属性或非法帧返回 {@code null}。 */
  public static JobSubscriptionInfo fromProperties(final Properties properties) {
    final String encoded = properties.getProperty(PROPERTY_NAME);
    if (encoded == null || encoded.isEmpty()) {
      return null;
    }
    try {
      final byte[] bytes = BASE_64_DECODER.decode(encoded);
      final JobSubscriptionInfo info = new JobSubscriptionInfo();
      info.wrap(new UnsafeBuffer(bytes), 0, bytes.length);
      return info;
    } catch (final RuntimeException e) {
      return null;
    }
  }

  public void writeIntoProperties(final Properties properties) {
    properties.setProperty(PROPERTY_NAME, BASE_64_ENCODER.encodeToString(encodeFrame()));
  }

  @Override
  public void wrap(final DirectBuffer buffer, int offset, final int length) {
    reset();
    headerDecoder.wrap(buffer, offset);
    offset += headerDecoder.encodedLength();
    final JobSubscriptionInfoDecoder bodyDecoder = new JobSubscriptionInfoDecoder();
    bodyDecoder.wrap(buffer, offset, headerDecoder.blockLength(), headerDecoder.version());
    generation = bodyDecoder.generation();

    final JobSubscriptionInfoDecoder.AggregatesDecoder aggregatesDecoder = bodyDecoder.aggregates();
    while (aggregatesDecoder.hasNext()) {
      aggregatesDecoder.next();
      final Aggregate aggregate = new Aggregate();
      final JobSubscriptionInfoDecoder.AggregatesDecoder.SessionsDecoder sessionsDecoder =
          aggregatesDecoder.sessions();
      while (sessionsDecoder.hasNext()) {
        sessionsDecoder.next();
        aggregate.sessions.add(new Session(sessionsDecoder.sessionId(), sessionsDecoder.worker()));
      }
      aggregate.jobType = aggregatesDecoder.jobType();
      aggregates.add(aggregate);
    }
  }

  @Override
  public int getLength() {
    return encodeFrame().length;
  }

  @Override
  public void write(final MutableDirectBuffer buffer, final int offset) {
    buffer.putBytes(offset, encodeFrame());
  }

  /** 编码为自扩展缓冲后拷出（对齐 wire codec 的确定性写法，免手工长度预算）。 */
  private byte[] encodeFrame() {
    final ExpandableArrayBuffer buffer = new ExpandableArrayBuffer(256);
    final JobSubscriptionInfoEncoder encoder = new JobSubscriptionInfoEncoder();
    encoder.wrapAndApplyHeader(buffer, 0, headerEncoder);
    encoder.generation(generation);
    final JobSubscriptionInfoEncoder.AggregatesEncoder aggregatesEncoder =
        encoder.aggregatesCount(aggregates.size());
    for (final Aggregate aggregate : aggregates) {
      final JobSubscriptionInfoEncoder.AggregatesEncoder.SessionsEncoder sessionsEncoder =
          aggregatesEncoder.sessionsCount(aggregate.sessions.size());
      for (final Session session : aggregate.sessions) {
        sessionsEncoder.next().sessionId(session.sessionId).worker(session.worker);
      }
      aggregatesEncoder.jobType(aggregate.jobType);
      aggregatesEncoder.next();
    }
    // encodedLength 为 body 长（相对消息体偏移），帧长需补上消息头
    final int frameLength = MessageHeaderEncoder.ENCODED_LENGTH + encoder.encodedLength();
    final byte[] bytes = new byte[frameLength];
    buffer.getBytes(0, bytes);
    return bytes;
  }
}
