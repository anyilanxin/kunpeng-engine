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
import static com.anyilanxin.kunpeng.structpack.util.BufferUtil.wrapString;

import com.anyilanxin.kunpeng.cluster.cluster.MemberId;
import com.anyilanxin.kunpeng.cluster.cluster.PartitionId;
import com.anyilanxin.kunpeng.cluster.raft.partition.PartitionMetadata;
import com.anyilanxin.kunpeng.protocol.admin.record.command.PartitionInfoMetaRecordValue;
import com.anyilanxin.kunpeng.protocol.admin.record.command.PartitionMemberMetaRecordValue;
import com.anyilanxin.kunpeng.protocol.common.UnifiedRecordValue;
import com.anyilanxin.kunpeng.structpack.AutoDeclareProperties;
import com.anyilanxin.kunpeng.structpack.property.ArrayProperty;
import com.anyilanxin.kunpeng.structpack.property.IntegerProperty;
import com.anyilanxin.kunpeng.structpack.property.StringProperty;
import com.fasterxml.jackson.annotation.JsonIgnore;
import java.util.*;
import org.agrona.DirectBuffer;

/**
 * 分区组元数据记录，描述分区组标识、分区 ID、目标优先级、当前主成员及组内成员列表。
 *
 * @author zxuanhong
 * @since
 */
@AutoDeclareProperties
public class PartitionInfoMetaRecord extends UnifiedRecordValue<PartitionInfoMetaRecord>
    implements PartitionInfoMetaRecordValue {
  // structpack-ids[PartitionInfoMetaRecord]: 1,2,3,4,5,6,7
  // structpack-ids[PartitionLeaveRecord]: 1,2,3,4,5
  private final StringProperty partitionGroupProp = new StringProperty(1, "PARTITION_GROUP", "");
  private final IntegerProperty partitionIdProp = new IntegerProperty(2, "PARTITION_ID", -1);
  private final IntegerProperty targetPriorityProp = new IntegerProperty(3, "TARGET_PRIORITY", -1);
  private final StringProperty primaryMemberIdProp = new StringProperty(4, "PRIMARY_MEMBER_ID", "");
  private final ArrayProperty<PartitionMemberMetaRecord> membersProp =
      new ArrayProperty<>(5, "MEMBERS", PartitionMemberMetaRecord::new);

  public PartitionInfoMetaRecord() {
    super(5);
    // formatting:off
    declareProperty(partitionGroupProp)
      .declareProperty(partitionIdProp)
      .declareProperty(targetPriorityProp)
      .declareProperty(primaryMemberIdProp)
      .declareProperty(membersProp);
    // formatting:on
  }

  @Override
  public String getPartitionGroup() {
    return bufferAsString(partitionGroupProp.getValue());
  }

  @JsonIgnore
  public DirectBuffer getPartitionGroupBuffer() {
    return partitionGroupProp.getValue();
  }

  public PartitionInfoMetaRecord setPartitionGroup(final String partitionGroup) {
    if (partitionGroup != null) {
      partitionGroupProp.setValue(wrapString(partitionGroup));
    }
    return this;
  }

  public PartitionInfoMetaRecord setPartitionGroup(final DirectBuffer partitionGroup) {
    partitionGroupProp.setValue(partitionGroup);
    return this;
  }

  @Override
  public int getPartitionId() {
    return partitionIdProp.getValue();
  }

  public PartitionInfoMetaRecord setPartitionId(final int partitionId) {
    partitionIdProp.setValue(partitionId);
    return this;
  }

  @Override
  public int getTargetPriority() {
    return targetPriorityProp.getValue();
  }

  public PartitionInfoMetaRecord setTargetPriority(final int targetPriority) {
    targetPriorityProp.setValue(targetPriority);
    return this;
  }

  @Override
  public String getPrimaryMemberId() {
    return bufferAsString(primaryMemberIdProp.getValue());
  }

  @JsonIgnore
  public DirectBuffer getPrimaryMemberIdBuffer() {
    return primaryMemberIdProp.getValue();
  }

  public PartitionInfoMetaRecord setPrimaryMemberId(final String primaryMemberId) {
    if (primaryMemberId != null) {
      primaryMemberIdProp.setValue(wrapString(primaryMemberId));
    }
    return this;
  }

  public PartitionInfoMetaRecord setPrimaryMemberId(final DirectBuffer primaryMemberId) {
    primaryMemberIdProp.setValue(primaryMemberId);
    return this;
  }

  /** 成员列表(原始数组访问,调度计划生成逻辑使用) */
  public ArrayProperty<PartitionMemberMetaRecord> members() {
    return membersProp;
  }

  @Override
  public List<PartitionMemberMetaRecordValue> getPartitionMembers() {
    final List<PartitionMemberMetaRecordValue> list = new ArrayList<>(membersProp.size());
    for (final PartitionMemberMetaRecord member : membersProp) {
      list.add(member);
    }
    return list;
  }

  @Override
  protected PartitionInfoMetaRecord newRecord() {
    return new PartitionInfoMetaRecord();
  }

  public PartitionInfoMetaRecord fromMetadata(final PartitionMetadata metadata) {
    final PartitionId id = metadata.id();
    partitionGroupProp.setValue(wrapString(id.group()));
    partitionIdProp.setValue(id.id());
    targetPriorityProp.setValue(metadata.getTargetPriority());
    if (metadata.getPrimary().isPresent()) {
      primaryMemberIdProp.setValue(wrapString(metadata.getPrimary().get().id()));
    }
    for (final MemberId member : metadata.members()) {
      members()
          .add()
          .setMemberId(wrapString(member.id()))
          .setPriority(metadata.getPriority(member));
    }
    return this;
  }

  public PartitionInfoMetaRecord fromMetadata(final PartitionInfoMetaRecord metadata) {
    partitionGroupProp.setValue(metadata.getPartitionGroupBuffer());
    partitionIdProp.setValue(metadata.getPartitionId());
    targetPriorityProp.setValue(metadata.getTargetPriority());
    primaryMemberIdProp.setValue(metadata.getPrimaryMemberIdBuffer());
    final ArrayProperty<PartitionMemberMetaRecord> newMembers = metadata.members();
    final ArrayProperty<PartitionMemberMetaRecord> members = members();
    for (final PartitionMemberMetaRecord member : newMembers) {
      members.add().setMemberId(member.getMemberIdBuffer()).setPriority(member.getPriority());
    }
    return this;
  }

  public PartitionMetadata toMetadata() {
    final PartitionId partitionId = PartitionId.from(getPartitionGroup(), getPartitionId());
    final MemberId primary = MemberId.from(getPrimaryMemberId());
    final int targetPriority = getTargetPriority();
    final Set<MemberId> members = new HashSet<>();
    final Map<MemberId, Integer> priorities = new HashMap<>();
    for (final PartitionMemberMetaRecord metaRecord : members()) {
      final MemberId memberId = MemberId.from(metaRecord.getMemberId());
      final int priority = metaRecord.getPriority();
      priorities.put(memberId, priority);
      members.add(memberId);
    }
    return new PartitionMetadata(partitionId, members, priorities, targetPriority, primary);
  }
}
