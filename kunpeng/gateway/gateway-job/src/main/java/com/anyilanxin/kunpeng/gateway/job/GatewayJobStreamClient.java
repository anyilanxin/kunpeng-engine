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
package com.anyilanxin.kunpeng.gateway.job;

import com.anyilanxin.kunpeng.broker.client.jobstream.JobStreamMessages.PushResult;
import com.anyilanxin.kunpeng.broker.client.jobstream.JobStreamMessages.StreamPush;
import com.anyilanxin.kunpeng.broker.client.jobstream.JobStreamSubjects;
import com.anyilanxin.kunpeng.broker.client.jobstream.JobStreamWireCodec;
import com.anyilanxin.kunpeng.cluster.cluster.ClusterMembershipService;
import com.anyilanxin.kunpeng.cluster.cluster.MemberId;
import com.anyilanxin.kunpeng.cluster.cluster.messaging.ClusterCommunicationService;
import com.anyilanxin.kunpeng.protocol.gateway.GatewayLoggers;
import com.anyilanxin.kunpeng.scheduler.Actor;
import java.util.Base64;
import org.slf4j.Logger;

/**
 * 网关侧 job 流传输：受理 broker 推送（请求-应答，应答即送达确认）；订阅快照搭车 SWIM 元数据通道扩散——写入本地成员属性（SBE 帧 base64）， 集群按
 * metadataVersion 版本反熵传播，broker 侧成员监听方解析合并，无需任何 job 专属注册报文。
 *
 * <p>SWIM 的 checkMetadata 周期性 diff 本地属性并 bump 版本（变更后 ≤250ms 打戳）；broker 收到新版本经拉取通道从本网关直取全量属性。 网关停机无需
 * bye——broker 侧成员移除监听自动摘桶。
 *
 * @author zxuanhong
 * @since 2026.9.0
 */
public final class GatewayJobStreamClient extends Actor {
  private static final Logger LOG = GatewayLoggers.GATEWAY_LOGGER_JOB;

  private final ClusterCommunicationService messaging;
  private final GatewayJobHub hub;
  private final ClusterMembershipService membership;

  public GatewayJobStreamClient(
      final ClusterCommunicationService messaging,
      final ClusterMembershipService membership,
      final GatewayJobHub hub) {
    this.messaging = messaging;
    this.membership = membership;
    this.hub = hub;
  }

  @Override
  public String getName() {
    return "gateway-job-stream-client";
  }

  @Override
  protected void onActorStarted() {
    messaging.replyTo(
        JobStreamSubjects.PUSH,
        JobStreamWireCodec::decodePush,
        (from, push) -> onPush(from, push),
        JobStreamWireCodec::encodeResult,
        actor);
    hub.setSnapshotChanged(() -> actor.run(this::publishSnapshotProperty));
    publishSnapshotProperty();
  }

  @Override
  protected void onActorClosed() {
    messaging.unsubscribe(JobStreamSubjects.PUSH);
  }

  private PushResult onPush(final MemberId from, final StreamPush push) {
    return hub.deliverPush(push)
        ? PushResult.ok()
        : PushResult.fail(
            "no eligible stream [type: %s, worker: %s, session: %d]"
                .formatted(push.record().getJobType(), push.worker(), push.sessionId()));
  }

  /** 快照写入本地成员属性：SWIM checkMetadata 检出 diff 后 bump metadataVersion 随 gossip 扩散，broker 按版本经拉取通道直取 */
  private void publishSnapshotProperty() {
    final String encoded;
    try {
      encoded =
          Base64.getEncoder().encodeToString(JobStreamWireCodec.encodeSnapshot(hub.snapshot()));
    } catch (final RuntimeException e) {
      LOG.warn("Job stream snapshot encode failed, skip publish", e);
      return;
    }
    membership
        .getLocalMember()
        .properties()
        .setProperty(JobStreamSubjects.SNAPSHOT_PROPERTY, encoded);
  }
}
