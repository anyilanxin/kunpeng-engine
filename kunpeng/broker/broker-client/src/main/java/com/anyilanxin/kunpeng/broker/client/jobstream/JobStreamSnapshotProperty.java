package com.anyilanxin.kunpeng.broker.client.jobstream;

import com.anyilanxin.kunpeng.broker.client.jobstream.JobStreamMessages.SubscriptionSnapshot;
import com.anyilanxin.kunpeng.cluster.cluster.ClusterMembershipService;
import com.anyilanxin.kunpeng.cluster.cluster.Member;
import com.anyilanxin.kunpeng.protocol.common.encoding.JobSubscriptionInfo;
import java.util.ArrayList;
import java.util.List;

/**
 * 订阅快照在 SWIM 成员属性中的读写对称入口：底层统一走 {@link JobSubscriptionInfo} 实体（SBE + Base64，属性键见 {@link
 * JobSubscriptionInfo#PROPERTY_NAME}）。写侧（网关，job 域管理服务）与读侧（broker，job 域收集侧）。
 *
 * <p>不进线上核心数据：快照随 SWIM 元数据版本反熵 + owner 直拉传播，拉取合并完成前读取方拿不到新值；网关停机无需摘属性—— 成员移除监听自动清理订阅桶。
 *
 * @author zxuanhong
 * @since 2026.9.0
 */
public final class JobStreamSnapshotProperty {

  private JobStreamSnapshotProperty() {}

  /** 把订阅快照编码为实体写入本地成员属性（原地覆写，是否传播由 SWIM 底层自行差分决定）。 */
  public static void publish(
      final ClusterMembershipService membership, final SubscriptionSnapshot snapshot) {
    final JobSubscriptionInfo info = new JobSubscriptionInfo().setGeneration(snapshot.generation());
    for (final SubscriptionSnapshot.Aggregate aggregate : snapshot.aggregates()) {
      final JobSubscriptionInfo.Aggregate target = new JobSubscriptionInfo.Aggregate();
      target.jobType = aggregate.jobType();
      for (final SubscriptionSnapshot.Session session : aggregate.sessions()) {
        target.sessions.add(new JobSubscriptionInfo.Session(session.sessionId(), session.worker()));
      }
      info.addAggregate(target);
    }
    info.writeIntoProperties(membership.getLocalMember().properties());
  }

  /** 解析成员属性中的订阅快照；缺属性或非法帧返回 {@code null}（代次幂等由协调器保证）。 */
  public static SubscriptionSnapshot snapshotOf(final Member gateway) {
    final JobSubscriptionInfo info = JobSubscriptionInfo.fromProperties(gateway.properties());
    if (info == null) {
      return null;
    }
    final List<SubscriptionSnapshot.Aggregate> aggregates =
        new ArrayList<>(info.getAggregates().size());
    for (final JobSubscriptionInfo.Aggregate aggregate : info.getAggregates()) {
      final List<SubscriptionSnapshot.Session> sessions =
          new ArrayList<>(aggregate.sessions.size());
      for (final JobSubscriptionInfo.Session session : aggregate.sessions) {
        sessions.add(new SubscriptionSnapshot.Session(session.sessionId, session.worker));
      }
      aggregates.add(new SubscriptionSnapshot.Aggregate(aggregate.jobType, sessions));
    }
    return new SubscriptionSnapshot(info.getGeneration(), aggregates);
  }
}
