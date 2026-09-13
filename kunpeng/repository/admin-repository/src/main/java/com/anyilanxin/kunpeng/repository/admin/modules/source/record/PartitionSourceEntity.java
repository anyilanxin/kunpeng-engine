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
package com.anyilanxin.kunpeng.repository.admin.modules.source.record;

import static com.anyilanxin.kunpeng.structpack.util.BufferUtil.bufferAsString;
import static com.anyilanxin.kunpeng.structpack.util.BufferUtil.wrapString;

import com.anyilanxin.kunpeng.kvstore.types.ValueType;
import com.anyilanxin.kunpeng.protocol.admin.impl.record.command.source.PartitionSourceRecord;
import com.anyilanxin.kunpeng.structpack.AutoDeclareProperties;
import com.anyilanxin.kunpeng.structpack.UnpackedObject;
import com.anyilanxin.kunpeng.structpack.property.ArrayProperty;
import com.anyilanxin.kunpeng.structpack.property.IntegerProperty;
import com.anyilanxin.kunpeng.structpack.property.StringProperty;
import com.anyilanxin.kunpeng.structpack.value.IntegerValue;
import com.fasterxml.jackson.annotation.JsonIgnore;
import java.util.LinkedHashSet;
import java.util.Set;
import org.agrona.DirectBuffer;

/**
 * 分区来源实体，记录分区组、分区 ID、来源 ID 与代理来源 ID 集合。
 *
 * @author zxuanhong
 * @since
 */
@AutoDeclareProperties
public class PartitionSourceEntity extends UnpackedObject implements ValueType {
  // structpack-ids[PartitionSourceRecord]: 1,2,3,4
  private final StringProperty partitionGroupProp = new StringProperty(1, "PARTITION_GROUP", "");
  private final IntegerProperty partitionIdProp = new IntegerProperty(2, "PARTITION_ID", -1);
  private final IntegerProperty sourceIdProp = new IntegerProperty(3, "SOURCE_ID", -1);
  private final ArrayProperty<IntegerValue> agentSourceIdsProp =
      new ArrayProperty<>(4, "AGENT_SOURCE_IDS", IntegerValue::new);

  public PartitionSourceEntity() {
    super(4);
    // formatting:off
    declareProperty(partitionGroupProp)
      .declareProperty(partitionIdProp)
      .declareProperty(sourceIdProp)
      .declareProperty(agentSourceIdsProp);
    // formatting:on
  }

  public void wrap(final PartitionSourceRecord record) {
    reset();
    setPartitionGroup(record.getPartitionGroup())
        .setPartitionId(record.getPartitionId())
        .setSourceId(record.getSourceId());
    copyAgentSourceIds(record.agentSourceIds(), agentSourceIdsProp);
  }

  public PartitionSourceRecord unwrap(final PartitionSourceRecord record) {
    record.reset();
    record
        .setPartitionGroup(getPartitionGroup())
        .setPartitionId(getPartitionId())
        .setSourceId(getSourceId());
    copyAgentSourceIds(agentSourceIdsProp, record.agentSourceIds());
    return record;
  }

  private static void copyAgentSourceIds(
      final ArrayProperty<IntegerValue> source, final ArrayProperty<IntegerValue> target) {
    for (final IntegerValue agentSourceId : source) {
      target.add().setValue(agentSourceId.getValue());
    }
  }

  /** 所属分区组 */
  public String getPartitionGroup() {
    return bufferAsString(partitionGroupProp.getValue());
  }

  @JsonIgnore
  public DirectBuffer getPartitionGroupBuffer() {
    return partitionGroupProp.getValue();
  }

  public PartitionSourceEntity setPartitionGroup(final String partitionGroup) {
    if (partitionGroup != null) {
      partitionGroupProp.setValue(wrapString(partitionGroup));
    }
    return this;
  }

  public PartitionSourceEntity setPartitionGroup(final DirectBuffer partitionGroup) {
    partitionGroupProp.setValue(partitionGroup);
    return this;
  }

  /** 所属分区 ID */
  public int getPartitionId() {
    return partitionIdProp.getValue();
  }

  public PartitionSourceEntity setPartitionId(final int partitionId) {
    partitionIdProp.setValue(partitionId);
    return this;
  }

  /** 来源 ID */
  public int getSourceId() {
    return sourceIdProp.getValue();
  }

  public PartitionSourceEntity setSourceId(final int sourceId) {
    sourceIdProp.setValue(sourceId);
    return this;
  }

  /** 代理来源 ID 集合(原始数组访问) */
  public ArrayProperty<IntegerValue> agentSourceIds() {
    return agentSourceIdsProp;
  }

  public Set<Integer> getAgentSourceIds() {
    final Set<Integer> set = new LinkedHashSet<>(agentSourceIdsProp.size());
    for (final IntegerValue agentSourceId : agentSourceIdsProp) {
      set.add(agentSourceId.getValue());
    }
    return set;
  }
}
