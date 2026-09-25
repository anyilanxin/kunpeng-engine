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
package com.anyilanxin.kunpeng.protocol.business.impl.record.command.deployment;

import static com.anyilanxin.kunpeng.protocol.business.impl.BusinessRecordConstant.ACTIVITY_DEFINITION_KEY;
import static com.anyilanxin.kunpeng.structpack.util.BufferUtil.bufferAsString;
import static com.anyilanxin.kunpeng.structpack.util.BufferUtil.wrapString;

import com.anyilanxin.kunpeng.protocol.business.record.command.deployment.StartEventType;
import com.anyilanxin.kunpeng.protocol.business.record.command.deployment.StarterEventRecordValue;
import com.anyilanxin.kunpeng.protocol.common.UnifiedRecordValue;
import com.anyilanxin.kunpeng.structpack.AutoDeclareProperties;
import com.anyilanxin.kunpeng.structpack.property.EnumProperty;
import com.anyilanxin.kunpeng.structpack.property.LongProperty;
import com.anyilanxin.kunpeng.structpack.property.StringProperty;
import com.fasterxml.jackson.annotation.JsonIgnore;
import org.agrona.DirectBuffer;

/**
 * 流程定义记录
 *
 * @author zxuanhong
 * @since 2026.9.0
 */
@AutoDeclareProperties
public class StarterEventRecord extends UnifiedRecordValue<StarterEventRecord>
    implements StarterEventRecordValue {
  // structpack-ids[StarterEventRecord]: 1,2,3,4
  private final LongProperty startEventIdProp = new LongProperty(1, "START_EVENT_ID", -1);
  private final StringProperty startEventNameProp = new StringProperty(2, "START_EVENT_NAME", "");
  private final StringProperty activityDefinitionKeyProp =
      new StringProperty(4, ACTIVITY_DEFINITION_KEY, "");
  private final EnumProperty<StartEventType> typeProp =
      new EnumProperty<>(
          3, "START_EVENT_ID_MAPPING_TYPE", StartEventType.class, StartEventType.NULL_VAL);

  public StarterEventRecord() {
    super(4);
    declareProperty(startEventIdProp)
        .declareProperty(startEventNameProp)
        .declareProperty(activityDefinitionKeyProp)
        .declareProperty(typeProp);
  }

  @Override
  public long getStartEventId() {
    return startEventIdProp.getValue();
  }

  public StarterEventRecord setStartEventId(final long startEventId) {
    startEventIdProp.setValue(startEventId);
    return this;
  }

  @Override
  public String getStartEventName() {
    return bufferAsString(startEventNameProp.getValue());
  }

  @JsonIgnore
  public DirectBuffer getStartEventNameBuffer() {
    return startEventNameProp.getValue();
  }

  public StarterEventRecord setStartEventName(final String startEventName) {
    startEventNameProp.setValue(wrapString(startEventName));
    return this;
  }

  public StarterEventRecord setStartEventName(final DirectBuffer startEventName) {
    startEventNameProp.setValue(startEventName);
    return this;
  }

  @Override
  public String getActivityDefinitionKey() {
    return bufferAsString(activityDefinitionKeyProp.getValue());
  }

  @JsonIgnore
  public DirectBuffer getActivityDefinitionKeyBuffer() {
    return activityDefinitionKeyProp.getValue();
  }

  public StarterEventRecord setActivityDefinitionKey(final String activityDefinitionKey) {
    activityDefinitionKeyProp.setValue(wrapString(activityDefinitionKey));
    return this;
  }

  public StarterEventRecord setActivityDefinitionKey(final DirectBuffer activityDefinitionKey) {
    activityDefinitionKeyProp.setValue(activityDefinitionKey);
    return this;
  }

  @Override
  public StartEventType getType() {
    return typeProp.getValue();
  }

  public StarterEventRecord setType(final StartEventType type) {
    typeProp.setValue(type);
    return this;
  }

  @Override
  protected StarterEventRecord newRecord() {
    return new StarterEventRecord();
  }
}
