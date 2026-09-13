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

import com.anyilanxin.kunpeng.protocol.admin.record.command.source.NodeSourceMetaRecordValue;
import com.anyilanxin.kunpeng.protocol.admin.record.command.source.NodeSourceRecordValue;
import com.anyilanxin.kunpeng.protocol.common.UnifiedRecordValue;
import com.anyilanxin.kunpeng.structpack.AutoDeclareProperties;
import com.anyilanxin.kunpeng.structpack.property.ArrayProperty;
import com.anyilanxin.kunpeng.structpack.property.IntegerProperty;
import com.anyilanxin.kunpeng.structpack.property.LongProperty;
import java.util.ArrayList;
import java.util.List;

/**
 * 节点来源元数据实体，记录元数据版本号、创建与更新时间，以及节点来源列表。
 *
 * @author zxuanhong
 * @since
 */
@AutoDeclareProperties
public class NodeSourceMetaRecord extends UnifiedRecordValue<NodeSourceMetaRecord>
    implements NodeSourceMetaRecordValue {
  // structpack-ids[NodeSourceMetaRecord]: 1,2,3,4,5
  private final IntegerProperty versionProp = new IntegerProperty(1, "VERSION", 0);
  private final LongProperty createTimeProp = new LongProperty(2, "CREATE_TIME", -1);
  private final LongProperty updateTimeProp = new LongProperty(3, "UPDATE_TIME", -1);
  private final ArrayProperty<NodeSourceRecord> sourcesProp =
      new ArrayProperty<>(4, "SOURCES", NodeSourceRecord::new);
  private final IntegerProperty maxNodeSourceIdProp =
      new IntegerProperty(5, "MAX_NODE_SOURCE_ID", 0);

  public NodeSourceMetaRecord() {
    super(5);
    // formatting:off
    declareProperty(versionProp)
      .declareProperty(createTimeProp)
      .declareProperty(updateTimeProp)
      .declareProperty(sourcesProp)
      .declareProperty(maxNodeSourceIdProp);
    // formatting:on
  }

  @Override
  public int getVersion() {
    return versionProp.getValue();
  }

  public NodeSourceMetaRecord setVersion(final int version) {
    versionProp.setValue(version);
    return this;
  }

  @Override
  public long getCreateTime() {
    return createTimeProp.getValue();
  }

  public NodeSourceMetaRecord setCreateTime(final long createTime) {
    createTimeProp.setValue(createTime);
    return this;
  }

  @Override
  public long getUpdateTime() {
    return updateTimeProp.getValue();
  }

  public NodeSourceMetaRecord setUpdateTime(final long updateTime) {
    updateTimeProp.setValue(updateTime);
    return this;
  }

  /** 已分配的最大节点来源 ID */
  @Override
  public int getMaxNodeSourceId() {
    return maxNodeSourceIdProp.getValue();
  }

  public NodeSourceMetaRecord setMaxNodeSourceId(final int maxNodeSourceId) {
    maxNodeSourceIdProp.setValue(maxNodeSourceId);
    return this;
  }

  /** 节点来源列表(原始数组访问) */
  public ArrayProperty<NodeSourceRecord> sources() {
    return sourcesProp;
  }

  @Override
  public List<NodeSourceRecordValue> getSources() {
    final List<NodeSourceRecordValue> list = new ArrayList<>(sourcesProp.size());
    for (final NodeSourceRecord source : sourcesProp) {
      list.add(source);
    }
    return list;
  }

  public List<NodeSourceRecord> getSourcesRecord() {
    final List<NodeSourceRecord> list = new ArrayList<>(sourcesProp.size());
    for (final NodeSourceRecord source : sourcesProp) {
      list.add(source);
    }
    return list;
  }
}
