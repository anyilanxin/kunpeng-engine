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
package com.anyilanxin.kunpeng.cluster.dispatch.command.business.dispatch.planner;

import com.anyilanxin.kunpeng.cluster.cluster.ClusterMembershipService;
import com.anyilanxin.kunpeng.cluster.cluster.MemberId;
import com.anyilanxin.kunpeng.cluster.config.topology.cluster.ClusterTopologyService;
import com.anyilanxin.kunpeng.cluster.dispatch.ClusterDispatchLoggers;
import com.anyilanxin.kunpeng.cluster.dispatch.LogEventWriter;
import com.anyilanxin.kunpeng.cluster.dispatch.command.business.dispatch.planner.generator.ChangePartitionDispatchPlanGenerator;
import com.anyilanxin.kunpeng.cluster.dispatch.command.business.dispatch.planner.generator.ChangeReplicationDispatchPlanGenerator;
import com.anyilanxin.kunpeng.cluster.dispatch.command.business.dispatch.planner.generator.ClusterBalanceDispatchPlanGenerator;
import com.anyilanxin.kunpeng.cluster.dispatch.command.delayed.DelayedDelayChecker;
import com.anyilanxin.kunpeng.protocol.admin.impl.record.command.business.BusinessDispatchPlanExecutionRecord;
import com.anyilanxin.kunpeng.protocol.admin.impl.record.command.business.BusinessDispatchPlanRecord;
import com.anyilanxin.kunpeng.protocol.admin.impl.record.command.common.PartitionInfoMetaRecord;
import com.anyilanxin.kunpeng.protocol.admin.impl.record.command.delayed.DelayedRecord;
import com.anyilanxin.kunpeng.protocol.admin.record.command.business.BusinessDispatchType;
import com.anyilanxin.kunpeng.repository.admin.AdminImmutableRepository;
import com.anyilanxin.kunpeng.repository.admin.modules.key.ImmutableRepositoryKey;
import com.anyilanxin.kunpeng.repository.admin.modules.source.ImmutableRepositorySource;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.slf4j.Logger;

/**
 * 调度计划制定统一入口（策略上下文），按计划记录中的 {@link BusinessDispatchType} 路由到对应策略实现。
 *
 * <p>三类调度：业务负载平衡（{@link BusinessDispatchType#CLUSTER_BALANCE}，分区数与副本数不变，成员重分布）、 调整分区数（{@link
 * BusinessDispatchType#CHANGE_PARTITION}，副本数不变）、 调整副本数（{@link
 * BusinessDispatchType#CHANGE_REPLICATION}，分区数不变）。 历史拓扑与历史分区数/副本数由计划记录携带（oldMeta 等字段），
 * 增减方向由历史值与期望值比较得出； 成员池由调用方计算传入，调用方保证满足副本分配需要，本类不做校验。 未注册策略的调度类型不支持在线制定计划，直接抛出异常。
 *
 * @author zxuanhong
 * @since
 */
public final class BusinessDispatchPlanMaker {
  private final Map<BusinessDispatchType, DispatchPlanGenerator> generators =
      new EnumMap<>(BusinessDispatchType.class);
  private final LogEventWriter writer;
  public static final Logger LOGGER = ClusterDispatchLoggers.CLUSTER_DISPATCH;
  private final DelayedDelayChecker delayChecker;
  private final ClusterMembershipService membershipService;
  private final DelayedRecord delayedRecord = new DelayedRecord();

  public BusinessDispatchPlanMaker(final LogEventWriter writer) {
    this.writer = writer;
    final AdminImmutableRepository repository = writer.getRepository();
    final ImmutableRepositoryKey repositoryKey = repository.repositoryKey();
    final ImmutableRepositorySource repositorySource = repository.repositorySource();
    delayChecker = writer.getDelayChecker();
    membershipService = writer.getMembershipService();
    final ClusterTopologyService clusterTopologyService = writer.getClusterTopologyService();
    register(
        new ClusterBalanceDispatchPlanGenerator(
            repositoryKey, clusterTopologyService, repositorySource));
    register(
        new ChangePartitionDispatchPlanGenerator(
            repositoryKey, clusterTopologyService, repositorySource));
    register(
        new ChangeReplicationDispatchPlanGenerator(
            repositoryKey, clusterTopologyService, repositorySource));
  }

  /**
   * 制定调度计划，结果直接补全到入参计划记录（executionPlan 串行执行明细 + meta 最终全量拓扑）。
   *
   * <p>计划 ID、调度类型、历史拓扑（oldMeta/oldPartitionsCount/oldReplicationFactor）、期望值
   * （expectPartitionsCount/expectReplicationFactor） 均取自入参计划记录；成员池由调用方计算传入。
   *
   * @param plan 携带调度类型、历史拓扑与期望值的计划记录，制定结果直接补全到该记录
   * @param memberIds 参与分配的成员池，由调用方保证满足副本分配需要
   */
  public void createPlan(final BusinessDispatchPlanRecord plan, final Set<MemberId> memberIds) {
    final BusinessDispatchType dispatchType = plan.getDispatchPlanType();
    final DispatchPlanGenerator generator =
        dispatchType == null ? null : generators.get(dispatchType);
    if (generator == null) {
      throw new IllegalArgumentException("不支持的调度类型，无法制定调度计划: " + dispatchType);
    }
    final BusinessDispatchPlanRecord result = generator.createPlan(plan, memberIds);
    // 遍历原始 ArrayProperty 桥接拷贝，避免走 getXxx() 多分配中间列表
    final List<PartitionInfoMetaRecord> metas = new ArrayList<>();
    for (final PartitionInfoMetaRecord meta : result.meta()) {
      metas.add(meta);
    }
    final List<BusinessDispatchPlanExecutionRecord> details = new ArrayList<>();
    for (final BusinessDispatchPlanExecutionRecord detail : result.executionPlan()) {
      details.add(detail);
    }
    plan.setMeta(metas).setExecutionPlan(details);
  }

  /** 注册策略，同类型重复注册视为编程错误 */
  private void register(final DispatchPlanGenerator generator) {
    final DispatchPlanGenerator previous = generators.put(generator.dispatchType(), generator);
    if (previous != null) {
      throw new IllegalStateException("调度类型策略重复注册: " + generator.dispatchType());
    }
  }
}
