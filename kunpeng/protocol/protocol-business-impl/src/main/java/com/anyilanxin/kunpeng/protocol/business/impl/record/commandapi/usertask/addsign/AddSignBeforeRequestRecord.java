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
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
package com.anyilanxin.kunpeng.protocol.business.impl.record.commandapi.usertask.addsign;

import static com.anyilanxin.kunpeng.structpack.util.BufferUtil.wrapString;

import com.anyilanxin.kunpeng.protocol.business.record.command.usertask.AddSignMode;
import com.anyilanxin.kunpeng.protocol.business.record.commandapi.record.usertask.addsign.AddSignBeforeRequestRecordValue;
import com.anyilanxin.kunpeng.protocol.common.UnifiedRecordValue;
import com.anyilanxin.kunpeng.structpack.AutoDeclareProperties;
import com.anyilanxin.kunpeng.structpack.property.ArrayProperty;
import com.anyilanxin.kunpeng.structpack.property.EnumProperty;
import com.anyilanxin.kunpeng.structpack.util.BufferUtil;
import com.anyilanxin.kunpeng.structpack.value.StringValue;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

/**
 * 前加签请求 Record。
 *
 * @author zxuanhong
 * @since 2026.9.0
 */
@AutoDeclareProperties
public class AddSignBeforeRequestRecord extends UnifiedRecordValue<AddSignBeforeRequestRecord>
    implements AddSignBeforeRequestRecordValue {
  private final ArrayProperty<StringValue> assigneesProp =
      new ArrayProperty<>(1, "ASSIGNEES", StringValue::new);
  private final EnumProperty<AddSignMode> addSignModeProp =
      new EnumProperty<>(2, "ADD_SIGN_MODE", AddSignMode.class, AddSignMode.NONE);

  public AddSignBeforeRequestRecord() {
    super(2);
    // formatting:off
    declareProperty(assigneesProp)
        .declareProperty(addSignModeProp);
    // formatting:on
  }

  @Override
  public List<String> getAssignees() {
    return StreamSupport.stream(assigneesProp.spliterator(), false)
        .map(StringValue::getValue)
        .map(BufferUtil::bufferAsString)
        .collect(Collectors.toList());
  }

  @Override
  public AddSignBeforeRequestRecordValue setAssignees(final List<String> assignees) {
    assigneesProp.reset();
    assignees.forEach(assignee -> assigneesProp.add().wrap(wrapString(assignee)));
    return this;
  }

  @Override
  public AddSignMode getAddSignMode() {
    return addSignModeProp.getValue();
  }

  @Override
  public AddSignBeforeRequestRecordValue setAddSignMode(final AddSignMode addSignMode) {
    addSignModeProp.setValue(addSignMode);
    return this;
  }
}
