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
package com.anyilanxin.kunpeng.protocol.admin.impl.record.command.source;

import static com.anyilanxin.kunpeng.structpack.util.BufferUtil.bufferAsString;
import static com.anyilanxin.kunpeng.structpack.util.BufferUtil.wrapString;

import com.anyilanxin.kunpeng.protocol.admin.record.command.source.PartitionSourceRecordValue;
import com.anyilanxin.kunpeng.protocol.common.UnifiedRecordValue;
import com.anyilanxin.kunpeng.structpack.AutoDeclareProperties;
import com.anyilanxin.kunpeng.structpack.property.ArrayProperty;
import com.anyilanxin.kunpeng.structpack.property.IntegerProperty;
import com.anyilanxin.kunpeng.structpack.property.StringProperty;
import com.anyilanxin.kunpeng.structpack.value.IntegerValue;
import com.fasterxml.jackson.annotation.JsonIgnore;
import java.util.LinkedHashSet;
import java.util.Set;
import org.agrona.DirectBuffer;

/**
 * 分区来源记录，描述分区组、分区 ID、来源 ID、代理来源 ID 集合与目标分区组、目标分区 ID。
 *
 * @author zxuanhong
 * @since
 */
@AutoDeclareProperties
public class PartitionSourceRecord extends UnifiedRecordValue<PartitionSourceRecord>
    implements PartitionSourceRecordValue {
  // structpack-ids[PartitionSourceRecord]: 1,2,3,4,5,6
  private final StringProperty partitionGroupProp = new StringProperty(1, "PARTITION_GROUP", "");
  private final IntegerProperty partitionIdProp = new IntegerProperty(2, "PARTITION_ID", -1);
  private final IntegerProperty sourceIdProp = new IntegerProperty(3, "SOURCE_ID", -1);
  private final ArrayProperty<IntegerValue> agentSourceIdsProp =
      new ArrayProperty<>(4, "AGENT_SOURCE_IDS", IntegerValue::new);
  private final StringProperty sourcePartitionGroupProp =
      new StringProperty(5, "SOURCE_PARTITION_GROUP", "");
  private final IntegerProperty sourcePartitionIdProp =
      new IntegerProperty(6, "SOURCE_PARTITION_ID", -1);

  public PartitionSourceRecord() {
    super(6);
    // formatting:off
    declareProperty(partitionGroupProp)
      .declareProperty(partitionIdProp)
      .declareProperty(sourceIdProp)
      .declareProperty(agentSourceIdsProp)
      .declareProperty(sourcePartitionGroupProp)
      .declareProperty(sourcePartitionIdProp);
    // formatting:on
  }

  /** 所属分区组 */
  @Override
  public String getPartitionGroup() {
    return bufferAsString(partitionGroupProp.getValue());
  }

  @JsonIgnore
  public DirectBuffer getPartitionGroupBuffer() {
    return partitionGroupProp.getValue();
  }

  public PartitionSourceRecord setPartitionGroup(final String partitionGroup) {
    if (partitionGroup != null) {
      partitionGroupProp.setValue(wrapString(partitionGroup));
    }
    return this;
  }

  public PartitionSourceRecord setPartitionGroup(final DirectBuffer partitionGroup) {
    partitionGroupProp.setValue(partitionGroup);
    return this;
  }

  /** 所属分区 ID */
  @Override
  public int getPartitionId() {
    return partitionIdProp.getValue();
  }

  public PartitionSourceRecord setPartitionId(final int partitionId) {
    partitionIdProp.setValue(partitionId);
    return this;
  }

  /** 来源 ID */
  @Override
  public int getSourceId() {
    return sourceIdProp.getValue();
  }

  public PartitionSourceRecord setSourceId(final int sourceId) {
    sourceIdProp.setValue(sourceId);
    return this;
  }

  /** 代理来源 ID 集合(原始数组访问) */
  public ArrayProperty<IntegerValue> agentSourceIds() {
    return agentSourceIdsProp;
  }

  @Override
  public Set<Integer> getAgentSourceIds() {
    final Set<Integer> set = new LinkedHashSet<>(agentSourceIdsProp.size());
    for (final IntegerValue agentSourceId : agentSourceIdsProp) {
      set.add(agentSourceId.getValue());
    }
    return set;
  }

  /** 目标分区组 */
  @Override
  public String getSourcePartitionGroup() {
    return bufferAsString(sourcePartitionGroupProp.getValue());
  }

  @JsonIgnore
  public DirectBuffer getSourcePartitionGroupBuffer() {
    return sourcePartitionGroupProp.getValue();
  }

  public PartitionSourceRecord setSourcePartitionGroup(final String targetPartitionGroup) {
    if (targetPartitionGroup != null) {
      sourcePartitionGroupProp.setValue(wrapString(targetPartitionGroup));
    }
    return this;
  }

  public PartitionSourceRecord setSourcePartitionGroup(final DirectBuffer targetPartitionGroup) {
    sourcePartitionGroupProp.setValue(targetPartitionGroup);
    return this;
  }

  /** 原分区 ID */
  @Override
  public int getSourcePartitionId() {
    return sourcePartitionIdProp.getValue();
  }

  public PartitionSourceRecord setSourcePartitionId(final int targetPartitionId) {
    sourcePartitionIdProp.setValue(targetPartitionId);
    return this;
  }
}
