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

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.anyilanxin.kunpeng.broker.client.jobstream.JobStreamMessages.PushResult;
import com.anyilanxin.kunpeng.broker.client.jobstream.JobStreamMessages.StreamPush;
import com.anyilanxin.kunpeng.broker.client.jobstream.JobStreamMessages.SubscriptionSnapshot;
import com.anyilanxin.kunpeng.cluster.cluster.Member;
import com.anyilanxin.kunpeng.protocol.business.impl.record.command.job.JobRecord;
import com.anyilanxin.kunpeng.protocol.business.record.command.job.JobKindType;
import com.anyilanxin.kunpeng.protocol.business.record.command.job.JobState;
import com.anyilanxin.kunpeng.protocol.common.encoding.JobSubscriptionInfo;
import java.util.List;
import java.util.Properties;
import org.junit.jupiter.api.Test;

/**
 * wire 编解码回环：SBE 变长字段必须按声明序消费—— worker 名刻意以 {@code sd} 开头钉住「跳过 worker 直读 record」的线上故障形态
 * （worker 字节被当 structpack 帧，magic=0x7364）。
 *
 * @author zxuanhong
 * @since 2026.9.0
 */
class JobStreamWireCodecTest {

  @Test
  void pushRoundTripCarriesWorkerAndRecord() {
    final var record = new JobRecord();
    record
        .setJobId(42)
        .setJobType("order-process")
        .setJobKind(JobKindType.ACTIVITY)
        .setState(JobState.PENDING)
        .setRetries(3)
        .setLockOwner("sdk-worker-1")
        .setProcessDefinitionKey("order-process-v1")
        .setProcessDefinitionId(1)
        .setProcessInstanceId(10);
    final var push = new StreamPush(7, "sdk-worker-1", 99, 2, 123_456L, record);

    final var decoded = JobStreamWireCodec.decodePush(JobStreamWireCodec.encodePush(push));

    assertThat(decoded.sessionId()).isEqualTo(7);
    assertThat(decoded.worker()).isEqualTo("sdk-worker-1");
    assertThat(decoded.jobKey()).isEqualTo(99);
    assertThat(decoded.partitionId()).isEqualTo(2);
    assertThat(decoded.deadline()).isEqualTo(123_456L);
    assertThat(decoded.record().getJobId()).isEqualTo(42);
    assertThat(decoded.record().getJobType()).isEqualTo("order-process");
    assertThat(decoded.record().getRetries()).isEqualTo(3);
    assertThat(decoded.record().getLockOwner()).isEqualTo("sdk-worker-1");
  }

  @Test
  void resultRoundTripKeepsReason() {
    final var ok = JobStreamWireCodec.decodeResult(JobStreamWireCodec.encodeResult(PushResult.ok()));
    assertThat(ok.delivered()).isTrue();
    assertThat(ok.reason()).isNull();

    final var failed =
        JobStreamWireCodec.decodeResult(
            JobStreamWireCodec.encodeResult(PushResult.fail("no eligible stream")));
    assertThat(failed.delivered()).isFalse();
    assertThat(failed.reason()).isEqualTo("no eligible stream");
  }

  @Test
  void snapshotRoundTripKeepsSessions() {
    final var snapshot =
        new SubscriptionSnapshot(
            3,
            List.of(
                new SubscriptionSnapshot.Aggregate(
                    "order-process",
                    List.of(
                        new SubscriptionSnapshot.Session(11, "sdk-worker-1"),
                        new SubscriptionSnapshot.Session(12, "other-worker"))),
                new SubscriptionSnapshot.Aggregate("payment", List.of())));

    // 快照编解码已收编为 protocol 层 JobSubscriptionInfo 实体，经成员属性对称读写验证
    final Properties properties = new Properties();
    final JobSubscriptionInfo entity = new JobSubscriptionInfo().setGeneration(snapshot.generation());
    for (final SubscriptionSnapshot.Aggregate aggregate : snapshot.aggregates()) {
      final var target = new JobSubscriptionInfo.Aggregate();
      target.jobType = aggregate.jobType();
      for (final SubscriptionSnapshot.Session session : aggregate.sessions()) {
        target.sessions.add(
            new JobSubscriptionInfo.Session(session.sessionId(), session.worker()));
      }
      entity.addAggregate(target);
    }
    entity.writeIntoProperties(properties);
    final var decoded = JobStreamSnapshotProperty.snapshotOf(memberOf(properties));

    assertThat(decoded.generation()).isEqualTo(3);
    assertThat(decoded.aggregates())
        .extracting(SubscriptionSnapshot.Aggregate::jobType)
        .containsExactly("order-process", "payment");
    assertThat(decoded.aggregates().getFirst().sessions())
        .containsExactly(
            new SubscriptionSnapshot.Session(11, "sdk-worker-1"),
            new SubscriptionSnapshot.Session(12, "other-worker"));
  }

  private static Member memberOf(final Properties properties) {
    final Member member = mock(Member.class);
    when(member.properties()).thenReturn(properties);
    return member;
  }
}