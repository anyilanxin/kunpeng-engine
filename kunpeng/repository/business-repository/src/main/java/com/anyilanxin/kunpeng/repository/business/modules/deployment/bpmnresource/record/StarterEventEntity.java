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
package com.anyilanxin.kunpeng.repository.business.modules.deployment.bpmnresource.record;

import static com.anyilanxin.kunpeng.protocol.business.impl.BusinessRecordConstant.ACTIVITY_DEFINITION_KEY;
import static com.anyilanxin.kunpeng.structpack.util.BufferUtil.bufferAsString;
import static com.anyilanxin.kunpeng.structpack.util.BufferUtil.wrapString;

import com.anyilanxin.kunpeng.protocol.business.record.command.deployment.StartEventType;
import com.anyilanxin.kunpeng.structpack.AutoDeclareProperties;
import com.anyilanxin.kunpeng.structpack.UnpackedObject;
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
public class StarterEventEntity extends UnpackedObject {
  private final LongProperty startEventIdProp = new LongProperty(1, "START_EVENT_ID", -1);
  private final StringProperty startEventNameProp = new StringProperty(2, "START_EVENT_NAME", "");
  private final StringProperty activityDefinitionKeyProp =
      new StringProperty(3, ACTIVITY_DEFINITION_KEY, "");
  private final EnumProperty<StartEventType> typeProp =
      new EnumProperty<>(
          4, "START_EVENT_ID_MAPPING_TYPE", StartEventType.class, StartEventType.NULL_VAL);

  public StarterEventEntity() {
    super(4);
    declareProperty(startEventIdProp)
        .declareProperty(startEventNameProp)
        .declareProperty(activityDefinitionKeyProp)
        .declareProperty(typeProp);
  }

  public long getStartEventId() {
    return startEventIdProp.getValue();
  }

  public StarterEventEntity setStartEventId(final long startEventId) {
    startEventIdProp.setValue(startEventId);
    return this;
  }

  public String getStartEventName() {
    return bufferAsString(startEventNameProp.getValue());
  }

  @JsonIgnore
  public DirectBuffer getStartEventNameBuffer() {
    return startEventNameProp.getValue();
  }

  public StarterEventEntity setStartEventName(final String startEventName) {
    startEventNameProp.setValue(wrapString(startEventName));
    return this;
  }

  public StarterEventEntity setStartEventName(final DirectBuffer startEventName) {
    startEventNameProp.setValue(startEventName);
    return this;
  }

  public String getActivityDefinitionKey() {
    return bufferAsString(activityDefinitionKeyProp.getValue());
  }

  @JsonIgnore
  public DirectBuffer getActivityDefinitionKeyBuffer() {
    return activityDefinitionKeyProp.getValue();
  }

  public StarterEventEntity setActivityDefinitionKey(final String activityDefinitionKey) {
    activityDefinitionKeyProp.setValue(wrapString(activityDefinitionKey));
    return this;
  }

  public StarterEventEntity setActivityDefinitionKey(final DirectBuffer activityDefinitionKey) {
    activityDefinitionKeyProp.setValue(activityDefinitionKey);
    return this;
  }

  public StartEventType getType() {
    return typeProp.getValue();
  }

  public StarterEventEntity setType(final StartEventType type) {
    typeProp.setValue(type);
    return this;
  }

  protected StarterEventEntity newRecord() {
    return new StarterEventEntity();
  }
}
