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
package com.anyilanxin.kunpeng.protocol.admin.impl.record.command.common;

import static com.anyilanxin.kunpeng.structpack.util.BufferUtil.bufferAsString;
import static com.anyilanxin.kunpeng.structpack.util.BufferUtil.copyInto;
import static com.anyilanxin.kunpeng.structpack.util.BufferUtil.wrapString;

import com.anyilanxin.kunpeng.cluster.cluster.MemberId;
import com.anyilanxin.kunpeng.cluster.cluster.PartitionId;
import com.anyilanxin.kunpeng.cluster.raft.partition.PartitionMetadata;
import com.anyilanxin.kunpeng.protocol.admin.record.command.PartitionExecutionType;
import com.anyilanxin.kunpeng.protocol.admin.record.command.PartitionType;
import com.anyilanxin.kunpeng.protocol.admin.record.command.common.PartitionConfigChangeRecordValue;
import com.anyilanxin.kunpeng.protocol.common.UnifiedRecordValue;
import com.anyilanxin.kunpeng.structpack.AutoDeclareProperties;
import com.anyilanxin.kunpeng.structpack.property.*;
import com.anyilanxin.kunpeng.structpack.value.StringValue;
import com.fasterxml.jackson.annotation.JsonIgnore;
import java.util.*;
import java.util.stream.Collectors;
import org.agrona.DirectBuffer;

/**
 * 分区配置变更命令记录，携带分区类型、执行类型、发起执行的节点 ID、目标分区组与分区 ID、 变更后的成员列表及计划制定后的分区完整最终拓扑元数据。
 *
 * @author zxuanhong
 * @since
 */
@AutoDeclareProperties
public class PartitionConfigChangeRecord extends UnifiedRecordValue<PartitionConfigChangeRecord>
    implements PartitionConfigChangeRecordValue {
  // structpack-ids[PartitionConfigChangeRecord]: 1,2,3,4,5,6,7,8,9
  private final EnumProperty<PartitionType> partitionTypeProp =
      new EnumProperty<>(1, "PARTITION_TYPE", PartitionType.class, PartitionType.ADMIN);
  private final EnumProperty<PartitionExecutionType> executionTypeProp =
      new EnumProperty<>(
          2, "EXECUTION_TYPE", PartitionExecutionType.class, PartitionExecutionType.CONFIG_CHANGE);
  private final StringProperty executionMemberIdProp =
      new StringProperty(3, "EXECUTION_MEMBER_ID", "");
  private final ArrayProperty<StringValue> membersProp =
      new ArrayProperty<>(4, "MEMBERS", StringValue::new);
  private final StringProperty partitionGroupProp = new StringProperty(5, "PARTITION_GROUP", "");
  private final IntegerProperty partitionIdProp = new IntegerProperty(6, "PARTITION_ID", -1);
  private final LongProperty dispatchPlanIdProp = new LongProperty(7, "DISPATCH_PLAN_ID", -1);
  private final LongProperty dispatchPlanExecutionIdProp =
      new LongProperty(8, "DISPATCH_PLAN_EXECUTION_ID", -1);
  private final ObjectProperty<PartitionInfoMetaRecord> targetMetaProp =
      new ObjectProperty<>(9, "TARGET_META", new PartitionInfoMetaRecord());

  public PartitionConfigChangeRecord() {
    super(9);
    // formatting:off
    declareProperty(partitionTypeProp)
      .declareProperty(executionTypeProp)
      .declareProperty(executionMemberIdProp)
      .declareProperty(membersProp)
      .declareProperty(partitionGroupProp)
      .declareProperty(partitionIdProp)
      .declareProperty(dispatchPlanIdProp)
      .declareProperty(dispatchPlanExecutionIdProp)
      .declareProperty(targetMetaProp);
    // formatting:on
  }

  /** 调度计划 id */
  @Override
  public long getDispatchPlanId() {
    return dispatchPlanIdProp.getValue();
  }

  public PartitionConfigChangeRecord setDispatchPlanId(final long dispatchPlanId) {
    dispatchPlanIdProp.setValue(dispatchPlanId);
    return this;
  }

  /** 调度计划执行 id */
  @Override
  public long getDispatchPlanExecutionId() {
    return dispatchPlanExecutionIdProp.getValue();
  }

  public PartitionConfigChangeRecord setDispatchPlanExecutionId(
      final long dispatchPlanExecutionId) {
    dispatchPlanExecutionIdProp.setValue(dispatchPlanExecutionId);
    return this;
  }

  @Override
  public PartitionType getPartitionType() {
    return partitionTypeProp.getValue();
  }

  public PartitionConfigChangeRecord setPartitionType(final PartitionType partitionType) {
    partitionTypeProp.setValue(partitionType);
    return this;
  }

  @Override
  public PartitionExecutionType getExecutionType() {
    return executionTypeProp.getValue();
  }

  public PartitionConfigChangeRecord setExecutionType(final PartitionExecutionType executionType) {
    executionTypeProp.setValue(executionType);
    return this;
  }

  /** 发起该执行动作的节点 id */
  @Override
  public String executionMemberId() {
    return bufferAsString(executionMemberIdProp.getValue());
  }

  @JsonIgnore
  public DirectBuffer executionMemberIdBuffer() {
    return executionMemberIdProp.getValue();
  }

  public PartitionConfigChangeRecord setExecutionMemberId(final String executionMemberId) {
    if (executionMemberId != null) {
      executionMemberIdProp.setValue(wrapString(executionMemberId));
    }
    return this;
  }

  public PartitionConfigChangeRecord setExecutionMemberId(final DirectBuffer executionMemberId) {
    executionMemberIdProp.setValue(executionMemberId);
    return this;
  }

  /** 变更后目标分区的成员列表 */
  @Override
  public List<String> getMembers() {
    final List<String> list = new ArrayList<>(membersProp.size());
    for (final StringValue member : membersProp) {
      list.add(bufferAsString(member.getValue()));
    }
    return list;
  }

  public Set<MemberId> getMemberIds() {
    return getMembers().stream().map(MemberId::from).collect(Collectors.toSet());
  }

  public PartitionConfigChangeRecord setMembers(final List<String> members) {
    membersProp.reset();
    if (members == null) {
      return this;
    }
    for (final String member : members) {
      membersProp.add().wrap(wrapString(member));
    }
    return this;
  }

  /** 分区组 */
  @Override
  public String getPartitionGroup() {
    return bufferAsString(partitionGroupProp.getValue());
  }

  @JsonIgnore
  public DirectBuffer getPartitionGroupBuffer() {
    return partitionGroupProp.getValue();
  }

  public PartitionConfigChangeRecord setPartitionGroup(final String partitionGroup) {
    if (partitionGroup != null) {
      partitionGroupProp.setValue(wrapString(partitionGroup));
    }
    return this;
  }

  public PartitionConfigChangeRecord setPartitionGroup(final DirectBuffer partitionGroup) {
    partitionGroupProp.setValue(partitionGroup);
    return this;
  }

  /** 分区 id */
  @Override
  public int getPartitionId() {
    return partitionIdProp.getValue();
  }

  public PartitionId getPartition() {
    return PartitionId.from(getPartitionGroup(), getPartitionId());
  }

  public PartitionConfigChangeRecord setPartitionId(final int partitionId) {
    partitionIdProp.setValue(partitionId);
    return this;
  }

  /** 计划制定后该分区的完整最终拓扑元数据（全量成员），执行端据此落地分区元数据 */
  @Override
  public PartitionInfoMetaRecord getTargetMeta() {
    return targetMetaProp.getValue();
  }

  public PartitionConfigChangeRecord setTargetMeta(final PartitionInfoMetaRecord meta) {
    copyInto(meta, targetMetaProp);
    return this;
  }

  @Override
  protected PartitionConfigChangeRecord newRecord() {
    return new PartitionConfigChangeRecord();
  }

  public PartitionMetadata toNewMetadata(final PartitionMetadata oldMetadata) {
    final Set<MemberId> memberIds = getMemberIds();
    final Map<MemberId, Integer> priority = new HashMap<>(memberIds.size());
    for (final MemberId memberId : memberIds) {
      priority.put(memberId, oldMetadata.getPriority(memberId));
    }
    MemberId memberId = null;
    if (oldMetadata.getPrimary().isPresent()) {
      memberId = oldMetadata.getPrimary().get();
    }
    return new PartitionMetadata(
        getPartition(), memberIds, priority, oldMetadata.getTargetPriority(), memberId);
  }
}
