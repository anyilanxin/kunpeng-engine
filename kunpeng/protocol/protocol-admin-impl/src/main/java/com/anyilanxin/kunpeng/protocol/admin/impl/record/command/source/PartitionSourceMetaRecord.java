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

import com.anyilanxin.kunpeng.protocol.admin.record.command.source.PartitionSourceMetaRecordValue;
import com.anyilanxin.kunpeng.protocol.admin.record.command.source.PartitionSourceRecordValue;
import com.anyilanxin.kunpeng.protocol.common.UnifiedRecordValue;
import com.anyilanxin.kunpeng.structpack.AutoDeclareProperties;
import com.anyilanxin.kunpeng.structpack.property.ArrayProperty;
import com.anyilanxin.kunpeng.structpack.property.IntegerProperty;
import com.anyilanxin.kunpeng.structpack.property.LongProperty;
import java.util.ArrayList;
import java.util.List;

/**
 * 分区来源元数据实体，记录元数据版本号、创建与更新时间，以及分区来源列表。
 *
 * @author zxuanhong
 * @since
 */
@AutoDeclareProperties
public class PartitionSourceMetaRecord extends UnifiedRecordValue<PartitionSourceMetaRecord>
    implements PartitionSourceMetaRecordValue {
  // structpack-ids[PartitionSourceMetaRecord]: 1,2,3,4,5
  private final IntegerProperty versionProp = new IntegerProperty(1, "VERSION", 0);
  private final LongProperty createTimeProp = new LongProperty(2, "CREATE_TIME", -1);
  private final LongProperty updateTimeProp = new LongProperty(3, "UPDATE_TIME", -1);
  private final ArrayProperty<PartitionSourceRecord> sourcesProp =
      new ArrayProperty<>(4, "SOURCES", PartitionSourceRecord::new);
  private final IntegerProperty maxPartitionSourceIdProp =
      new IntegerProperty(5, "MAX_PARTITION_SOURCE_ID", 0);

  public PartitionSourceMetaRecord() {
    super(5);
    // formatting:off
    declareProperty(versionProp)
      .declareProperty(createTimeProp)
      .declareProperty(updateTimeProp)
      .declareProperty(sourcesProp)
      .declareProperty(maxPartitionSourceIdProp);
    // formatting:on
  }

  @Override
  public int getVersion() {
    return versionProp.getValue();
  }

  public PartitionSourceMetaRecord setVersion(final int version) {
    versionProp.setValue(version);
    return this;
  }

  @Override
  public long getCreateTime() {
    return createTimeProp.getValue();
  }

  public PartitionSourceMetaRecord setCreateTime(final long createTime) {
    createTimeProp.setValue(createTime);
    return this;
  }

  @Override
  public long getUpdateTime() {
    return updateTimeProp.getValue();
  }

  public PartitionSourceMetaRecord setUpdateTime(final long updateTime) {
    updateTimeProp.setValue(updateTime);
    return this;
  }

  /** 已分配的最大分区来源 ID */
  @Override
  public int getMaxPartitionSourceId() {
    return maxPartitionSourceIdProp.getValue();
  }

  public PartitionSourceMetaRecord setMaxPartitionSourceId(final int maxPartitionSourceId) {
    maxPartitionSourceIdProp.setValue(maxPartitionSourceId);
    return this;
  }

  /** 分区来源列表(原始数组访问) */
  public ArrayProperty<PartitionSourceRecord> sources() {
    return sourcesProp;
  }

  @Override
  public List<PartitionSourceRecordValue> getSources() {
    final List<PartitionSourceRecordValue> list = new ArrayList<>(sourcesProp.size());
    for (final PartitionSourceRecord source : sourcesProp) {
      list.add(source);
    }
    return list;
  }

  public List<PartitionSourceRecord> getSourcesRecord() {
    final List<PartitionSourceRecord> list = new ArrayList<>(sourcesProp.size());
    for (final PartitionSourceRecord source : sourcesProp) {
      list.add(source);
    }
    return list;
  }
}
