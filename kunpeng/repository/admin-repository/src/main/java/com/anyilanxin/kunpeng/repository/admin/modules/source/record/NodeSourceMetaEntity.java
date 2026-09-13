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

import com.anyilanxin.kunpeng.kvstore.types.ValueType;
import com.anyilanxin.kunpeng.protocol.admin.impl.record.command.source.NodeSourceMetaRecord;
import com.anyilanxin.kunpeng.structpack.AutoDeclareProperties;
import com.anyilanxin.kunpeng.structpack.UnpackedObject;
import com.anyilanxin.kunpeng.structpack.property.IntegerProperty;
import com.anyilanxin.kunpeng.structpack.property.LongProperty;

/**
 * 节点来源元数据实体，记录元数据版本号、创建与更新时间。
 *
 * @author zxuanhong
 * @since
 */
@AutoDeclareProperties
public class NodeSourceMetaEntity extends UnpackedObject implements ValueType {
  // structpack-ids[NodeSourceMetaRecord]: 1,2,3,5
  private final IntegerProperty versionProp = new IntegerProperty(1, "VERSION", 0);
  private final LongProperty createTimeProp = new LongProperty(2, "CREATE_TIME", -1);
  private final LongProperty updateTimeProp = new LongProperty(3, "UPDATE_TIME", -1);
  private final IntegerProperty maxNodeSourceIdProp =
      new IntegerProperty(5, "MAX_NODE_SOURCE_ID", 0);

  public NodeSourceMetaEntity() {
    super(4);
    // formatting:off
    declareProperty(versionProp)
      .declareProperty(createTimeProp)
      .declareProperty(updateTimeProp)
      .declareProperty(maxNodeSourceIdProp);
    // formatting:on
  }

  public void wrap(final NodeSourceMetaRecord record) {
    reset();
    setVersion(record.getVersion())
        .setCreateTime(record.getCreateTime())
        .setUpdateTime(record.getUpdateTime())
        .setMaxNodeSourceId(record.getMaxNodeSourceId());
  }

  public NodeSourceMetaRecord unwrap(final NodeSourceMetaRecord record) {
    record.reset();
    return record
        .setVersion(getVersion())
        .setCreateTime(getCreateTime())
        .setUpdateTime(getUpdateTime())
        .setMaxNodeSourceId(getMaxNodeSourceId());
  }

  public int getVersion() {
    return versionProp.getValue();
  }

  public NodeSourceMetaEntity setVersion(final int version) {
    versionProp.setValue(version);
    return this;
  }

  public long getCreateTime() {
    return createTimeProp.getValue();
  }

  public NodeSourceMetaEntity setCreateTime(final long createTime) {
    createTimeProp.setValue(createTime);
    return this;
  }

  public long getUpdateTime() {
    return updateTimeProp.getValue();
  }

  public NodeSourceMetaEntity setUpdateTime(final long updateTime) {
    updateTimeProp.setValue(updateTime);
    return this;
  }

  /** 已分配的最大节点来源 ID */
  public int getMaxNodeSourceId() {
    return maxNodeSourceIdProp.getValue();
  }

  public NodeSourceMetaEntity setMaxNodeSourceId(final int maxNodeSourceId) {
    maxNodeSourceIdProp.setValue(maxNodeSourceId);
    return this;
  }
}
