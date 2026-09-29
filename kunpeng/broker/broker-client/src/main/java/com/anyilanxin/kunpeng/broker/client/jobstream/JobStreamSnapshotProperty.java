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
package com.anyilanxin.kunpeng.broker.client.jobstream;

import com.anyilanxin.kunpeng.broker.client.jobstream.JobStreamMessages.SubscriptionSnapshot;
import com.anyilanxin.kunpeng.cluster.cluster.ClusterMembershipService;
import com.anyilanxin.kunpeng.cluster.cluster.Member;
import java.util.Base64;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 订阅快照在 SWIM 成员属性中的单一载体（{@link JobStreamSubjects#SNAPSHOT_PROPERTY} = Base64(SBE
 * subscriptionSnapshot 帧)），写侧（网关）与读侧（broker）的对称入口。
 *
 * <p>不进内联白名单：快照随元数据版本反熵 + owner 直拉传播，拉取合并完成前读取方拿不到新值；网关停机无需摘属性——成员移除监听自动清理订阅桶。
 *
 * @author zxuanhong
 * @since 2026.9.0
 */
public final class JobStreamSnapshotProperty {
  private static final Logger LOG = LoggerFactory.getLogger(JobStreamSnapshotProperty.class);

  private JobStreamSnapshotProperty() {}

  /**
   * 编码并写入本地成员属性（原地覆写，SWIM checkMetadata 检出 diff 后 bump 版本扩散）。
   *
   * <p>编码失败记 warn 并跳过本次发布，保留上一版本快照。
   */
  public static void publish(
      final ClusterMembershipService membership, final SubscriptionSnapshot snapshot) {
    final String encoded;
    try {
      encoded = Base64.getEncoder().encodeToString(JobStreamWireCodec.encodeSnapshot(snapshot));
    } catch (final RuntimeException e) {
      LOG.warn("Job stream snapshot encode failed, skip publish", e);
      return;
    }
    membership
        .getLocalMember()
        .properties()
        .setProperty(JobStreamSubjects.SNAPSHOT_PROPERTY, encoded);
  }

  /** 解析成员属性中的订阅快照；缺属性返回 {@code null}，非法帧记 warn 丢弃并返回 {@code null}（代次幂等由协调器保证）。 */
  public static SubscriptionSnapshot snapshotOf(final Member gateway) {
    final String encoded = gateway.properties().getProperty(JobStreamSubjects.SNAPSHOT_PROPERTY);
    if (encoded == null || encoded.isEmpty()) {
      return null;
    }
    try {
      return JobStreamWireCodec.decodeSnapshot(Base64.getDecoder().decode(encoded));
    } catch (final RuntimeException e) {
      LOG.warn("网关 {} 订阅快照属性解析失败, 忽略", gateway.id(), e);
      return null;
    }
  }
}
