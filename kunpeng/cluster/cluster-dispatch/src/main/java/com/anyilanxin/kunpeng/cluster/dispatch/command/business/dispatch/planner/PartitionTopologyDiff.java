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

import com.anyilanxin.kunpeng.cluster.cluster.MemberId;
import com.anyilanxin.kunpeng.cluster.cluster.PartitionId;
import com.anyilanxin.kunpeng.protocol.admin.impl.record.command.common.PartitionInfoMetaRecord;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 分区拓扑 diff 工具：对比当前拓扑与目标拓扑，统一推导 BOOTSTRAP/JOIN/LEAVE 操作序列。
 *
 * <p>对齐 zeebe 的「状态对比推导」方式：各调度策略只负责计算目标拓扑， 操作序列由本工具按新旧差异统一翻译， 避免各策略手写各自的推导规则。
 * 业务面（{@code AbstractDispatchPlanGenerator#appendDiff}）与管理面（{@code
 * AdminDispatchPlanMaker#appendDiff}） 将操作翻译为对应分区类型（BUSINESS/ADMIN）的执行明细负载，管理面不产生引导操作。
 *
 * <p>整体推导规则（当前拓扑取自计划记录 oldMeta，目标拓扑由各生成器计算）：
 *
 * <ul>
 *   <li>分区增加（目标有、当前无）：主成员 BOOTSTRAP，其余副本按成员 ID 升序 JOIN——先引导、后补齐副本；
 *   <li>副本增加（分区两侧均存在，目标成员更多）：目标独有成员按成员 ID 升序直接 JOIN，不涉及数据迁移；
 *   <li>副本减少（分区两侧均存在，当前成员更多）：当前独有成员按成员 ID 升序 LEAVE；
 *   <li>分区删除（当前有、目标无）：全部剩余成员按成员 ID 升序 LEAVE，且整体先于其他操作输出；
 *   <li>两侧完全一致：不产生任何操作，调用方据此得到空计划。
 * </ul>
 *
 * <p>操作输出顺序：先输出分区删除的 LEAVE，再按分区 ID 升序处理目标分区——新分区 BOOTSTRAP+JOIN、 已有分区先 JOIN 后 LEAVE（先扩后缩），
 * 成员操作一律按成员 ID 升序，保证计划确定性。
 *
 * <p>分区删除若涉及数据搬迁（CHANGE_PARTITION 缩容），由生成器多阶段编排，不直接依赖本工具的删除推导： 被缩容分区非 Leader 成员离开（分区收敛为
 * Leader 单副本）→ 数据合并至保留分区 → Leader 停止销毁分区 → 来源标识转移至保留分区； 保留分区全程保持完整副本，无需缩成单副本再扩容——合并后其
 * follower 的镜像安装由 raft 侧完成（leader 合并收尾即通知 follower 拉取安装最新镜像）； 已是单副本的分区无非 Leader 离开步骤，仅数据合并、来源标识转移后由
 * Leader 停止销毁。
 *
 * <p>BOOTSTRAP 负载携带 bootstrapSnapshot 标识：当前拓扑为空（全量初始化，无既有数据）时为 false， 否则（扩容新增分区）为 true，
 * 执行端据此决定是否从既有分区引导数据；该标识仅对分区增加的引导操作有意义。
 *
 * @author zxuanhong
 * @since
 */
public final class PartitionTopologyDiff {

  private PartitionTopologyDiff() {}

  /** 拓扑差异操作，由各面计划生成基类翻译为对应面的执行明细负载 */
  public sealed interface Operation permits BootstrapOperation, JoinOperation, LeaveOperation {}

  /** 分区在当前拓扑中不存在：由主成员引导新分区 */
  public record BootstrapOperation(PartitionInfoMetaRecord partition, String primaryMemberId)
      implements Operation {}

  /** 目标成员加入分区（新建分区的其余副本与已有分区的补充成员） */
  public record JoinOperation(PartitionInfoMetaRecord partition, String memberId)
      implements Operation {}

  /** 成员从分区离开（整分区移除时为全部剩余成员） */
  public record LeaveOperation(PartitionId partitionId, String memberId) implements Operation {}

  /**
   * 对比当前拓扑与目标拓扑，按序推导操作：
   *
   * <ul>
   *   <li>当前存在而目标缺失的分区视为移除分区：剩余成员按成员 ID 升序 LEAVE，整体先于其他操作输出；
   *   <li>目标分区不在当前拓扑：主成员 BOOTSTRAP，其余副本按成员 ID 升序 JOIN；
   *   <li>目标分区已在当前拓扑：先 JOIN 目标独有成员再 LEAVE 当前独有成员（先扩后缩），各自按成员 ID 升序；
   *   <li>两份拓扑完全一致：不产生任何操作（调用方据此得到空计划）。
   * </ul>
   */
  public static List<Operation> diff(
      final List<PartitionInfoMetaRecord> current, final List<PartitionInfoMetaRecord> target) {
    final Map<String, PartitionInfoMetaRecord> currentById = byPartitionId(current);
    final Map<String, PartitionInfoMetaRecord> targetById = byPartitionId(target);
    final List<Operation> operations = new ArrayList<>();
    for (final PartitionInfoMetaRecord partition : currentById.values()) {
      if (!targetById.containsKey(partitionKey(partition))) {
        for (final MemberId member : sortedMembers(partition)) {
          operations.add(new LeaveOperation(partitionIdOf(partition), member.id()));
        }
      }
    }
    for (final PartitionInfoMetaRecord targetPartition : targetById.values()) {
      final PartitionInfoMetaRecord existing = currentById.get(partitionKey(targetPartition));
      if (existing == null) {
        final String primary = primaryMemberId(targetPartition);
        operations.add(new BootstrapOperation(targetPartition, primary));
        for (final MemberId member : sortedMembers(targetPartition)) {
          if (!member.id().equals(primary)) {
            operations.add(new JoinOperation(targetPartition, member.id()));
          }
        }
        continue;
      }
      final Set<String> currentMemberIds = memberIds(existing);
      final Set<String> targetMemberIds = memberIds(targetPartition);
      for (final MemberId member : sortedMembers(targetPartition)) {
        if (!currentMemberIds.contains(member.id())) {
          operations.add(new JoinOperation(targetPartition, member.id()));
        }
      }
      for (final MemberId member : sortedMembers(existing)) {
        if (!targetMemberIds.contains(member.id())) {
          operations.add(new LeaveOperation(partitionIdOf(targetPartition), member.id()));
        }
      }
    }
    return operations;
  }

  /** 构建移除指定成员后的分区拓扑记录副本（分区标识、目标优先级与主成员保持不变） */
  public static PartitionInfoMetaRecord removeMembers(
      final PartitionInfoMetaRecord source, final Set<String> removedMemberIds) {
    final PartitionInfoMetaRecord removed =
        new PartitionInfoMetaRecord()
            .setPartitionGroup(source.getPartitionGroup())
            .setPartitionId(source.getPartitionId())
            .setTargetPriority(source.getTargetPriority())
            .setPrimaryMemberId(source.getPrimaryMemberId());
    source
        .members()
        .forEach(
            member -> {
              if (!removedMemberIds.contains(member.getMemberId())) {
                removed
                    .members()
                    .add()
                    .setMemberId(member.getMemberId())
                    .setPriority(member.getPriority());
              }
            });
    return removed;
  }

  /** 按分区 ID 升序建立 分区键 → 拓扑记录 的有序视图 */
  private static Map<String, PartitionInfoMetaRecord> byPartitionId(
      final List<PartitionInfoMetaRecord> partitions) {
    final List<PartitionInfoMetaRecord> sorted = new ArrayList<>(partitions);
    sorted.sort(Comparator.comparingInt(PartitionInfoMetaRecord::getPartitionId));
    final Map<String, PartitionInfoMetaRecord> byId = new LinkedHashMap<>();
    for (final PartitionInfoMetaRecord partition : sorted) {
      byId.put(partitionKey(partition), partition);
    }
    return byId;
  }

  /** 分区唯一键（分组 + 分区号） */
  private static String partitionKey(final PartitionInfoMetaRecord partition) {
    return partition.getPartitionGroup() + "/" + partition.getPartitionId();
  }

  /** 分区成员按成员 ID 升序 */
  private static List<MemberId> sortedMembers(final PartitionInfoMetaRecord partition) {
    return partition.members().stream()
        .map(member -> MemberId.from(member.getMemberId()))
        .sorted(Comparator.comparing(MemberId::id))
        .toList();
  }

  private static Set<String> memberIds(final PartitionInfoMetaRecord partition) {
    final Set<String> ids = new HashSet<>();
    partition.members().forEach(member -> ids.add(member.getMemberId()));
    return ids;
  }

  /** 主成员 ID，缺失时回退首个成员 */
  private static String primaryMemberId(final PartitionInfoMetaRecord partition) {
    final String primary = partition.getPrimaryMemberId();
    if (primary == null || primary.isEmpty()) {
      return partition.members().stream().findFirst().orElseThrow().getMemberId();
    }
    return primary;
  }

  private static PartitionId partitionIdOf(final PartitionInfoMetaRecord partition) {
    return PartitionId.from(partition.getPartitionGroup(), partition.getPartitionId());
  }
}
