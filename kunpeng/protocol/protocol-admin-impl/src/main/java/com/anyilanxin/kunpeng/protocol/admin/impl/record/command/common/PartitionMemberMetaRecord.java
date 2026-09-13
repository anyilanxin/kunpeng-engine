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

import com.anyilanxin.kunpeng.protocol.admin.record.command.PartitionMemberMetaRecordValue;
import com.anyilanxin.kunpeng.protocol.common.UnifiedRecordValue;
import com.anyilanxin.kunpeng.structpack.AutoDeclareProperties;
import com.anyilanxin.kunpeng.structpack.property.IntegerProperty;
import com.anyilanxin.kunpeng.structpack.property.StringProperty;
import com.fasterxml.jackson.annotation.JsonIgnore;
import org.agrona.DirectBuffer;

/**
 * 分区组成员元数据记录，描述成员 ID 及其在分区组内的优先级。
 *
 * @author zxuanhong
 * @since
 */
@AutoDeclareProperties
public class PartitionMemberMetaRecord extends UnifiedRecordValue<PartitionMemberMetaRecord>
    implements PartitionMemberMetaRecordValue {
  // structpack-ids[PartitionMemberMetaRecord]: 1,3
  private final StringProperty memberIdProp = new StringProperty(1, "MEMBER_ID", "");
  private final IntegerProperty priorityProp = new IntegerProperty(3, "PRIORITY", -1);

  public PartitionMemberMetaRecord() {
    super(2);
    // formatting:off
    declareProperty(memberIdProp)
      .declareProperty(priorityProp);
    // formatting:on
  }

  @Override
  public String getMemberId() {
    return bufferAsString(memberIdProp.getValue());
  }

  @JsonIgnore
  public DirectBuffer getMemberIdBuffer() {
    return memberIdProp.getValue();
  }

  public PartitionMemberMetaRecord setMemberId(final String memberId) {
    if (memberId != null) {
      memberIdProp.setValue(wrapString(memberId));
    }
    return this;
  }

  public PartitionMemberMetaRecord setMemberId(final DirectBuffer memberId) {
    memberIdProp.setValue(memberId);
    return this;
  }

  @Override
  public int getPriority() {
    return priorityProp.getValue();
  }

  public PartitionMemberMetaRecord setPriority(final int priority) {
    priorityProp.setValue(priority);
    return this;
  }

  @Override
  protected PartitionMemberMetaRecord newRecord() {
    return new PartitionMemberMetaRecord();
  }
}
