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

import com.anyilanxin.kunpeng.protocol.admin.record.command.source.NodeSourceRecordValue;
import com.anyilanxin.kunpeng.protocol.common.UnifiedRecordValue;
import com.anyilanxin.kunpeng.structpack.AutoDeclareProperties;
import com.anyilanxin.kunpeng.structpack.property.IntegerProperty;
import com.anyilanxin.kunpeng.structpack.property.StringProperty;
import com.fasterxml.jackson.annotation.JsonIgnore;
import org.agrona.DirectBuffer;

/**
 * 节点来源记录，描述来源节点成员 ID 与来源 ID。
 *
 * @author zxuanhong
 * @since
 */
@AutoDeclareProperties
public class NodeSourceRecord extends UnifiedRecordValue<NodeSourceRecord>
    implements NodeSourceRecordValue {
  // structpack-ids[NodeSourceRecord]: 1,2
  private final StringProperty memberIdProp = new StringProperty(1, "MEMBER_ID");
  private final IntegerProperty sourceIdProp = new IntegerProperty(2, "SOURCE_ID", -1);

  public NodeSourceRecord() {
    super(2);
    // formatting:off
    declareProperty(memberIdProp)
      .declareProperty(sourceIdProp);
    // formatting:on
  }

  /** 来源节点成员 ID */
  @Override
  public String getMemberId() {
    return bufferAsString(memberIdProp.getValue());
  }

  @JsonIgnore
  public DirectBuffer getMemberIdBuffer() {
    return memberIdProp.getValue();
  }

  public NodeSourceRecord setMemberId(final String memberId) {
    if (memberId != null) {
      memberIdProp.setValue(wrapString(memberId));
    }
    return this;
  }

  public NodeSourceRecord setMemberId(final DirectBuffer memberId) {
    memberIdProp.setValue(memberId);
    return this;
  }

  /** 来源 ID */
  @Override
  public int getSourceId() {
    return sourceIdProp.getValue();
  }

  public NodeSourceRecord setSourceId(final int sourceId) {
    sourceIdProp.setValue(sourceId);
    return this;
  }
}
