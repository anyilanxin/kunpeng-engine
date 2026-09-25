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

import static com.anyilanxin.kunpeng.protocol.business.impl.BusinessRecordConstant.*;
import static com.anyilanxin.kunpeng.structpack.util.BufferUtil.bufferAsString;
import static com.anyilanxin.kunpeng.structpack.util.BufferUtil.wrapString;

import com.anyilanxin.kunpeng.kvstore.types.StoreValue;
import com.anyilanxin.kunpeng.protocol.business.impl.record.command.deployment.ProcessDefinitionRecord;
import com.anyilanxin.kunpeng.protocol.business.impl.record.command.deployment.StarterEventRecord;
import com.anyilanxin.kunpeng.protocol.common.TenantOwned;
import com.anyilanxin.kunpeng.structpack.AutoDeclareProperties;
import com.anyilanxin.kunpeng.structpack.UnpackedObject;
import com.anyilanxin.kunpeng.structpack.property.*;
import com.anyilanxin.kunpeng.structpack.value.StringValue;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.agrona.DirectBuffer;

/**
 * 流程定义 Entity：流程定义 Record 的落库映射。
 *
 * @author zxuanhong
 * @since 2026.9.0
 */
@AutoDeclareProperties
public class ProcessDefinitionEntity extends UnpackedObject implements StoreValue {
  // structpack-ids[ProcessDefinitionEntity]: 1,2,3,4,5,6,7,8,9,10,11,12,13,14,15
  private final LongProperty processDefinitionIdProp =
      new LongProperty(1, PROCESS_DEFINITION_ID, -1);
  private final StringProperty processDefinitionNameProp =
      new StringProperty(2, PROCESS_DEFINITION_NAME, "");
  private final StringProperty processDefinitionKeyProp =
      new StringProperty(3, PROCESS_DEFINITION_KEY, "");
  private final IntegerProperty processDefinitionVersionProp =
      new IntegerProperty(4, PROCESS_DEFINITION_VERSION, -1);
  private final BooleanProperty suspensionProp = new BooleanProperty(5, "SUSPENSION", true);
  private final BooleanProperty startableProp = new BooleanProperty(6, "STARTABLE", true);
  private final IntegerProperty historyTimeToLiveProp =
      new IntegerProperty(7, "HISTORY_TIME_TO_LIVE", -1);
  private final ArrayProperty<StringValue> candidateGroupsProp =
      new ArrayProperty<>(8, "CANDIDATE_STARTER_GROUPS", StringValue::new);
  private final ArrayProperty<StringValue> candidateUsersProp =
      new ArrayProperty<>(9, "CANDIDATE_STARTER_USERS", StringValue::new);
  private final StringProperty versionTagProp = new StringProperty(10, VERSION_TAG, "");
  private final LongProperty deploymentIdProp = new LongProperty(11, DEPLOYMENT_ID, -1);
  private final LongProperty resourceDefinitionIdProp =
      new LongProperty(12, "RESOURCE_DEFINITION_ID", -1);
  private final StringProperty resourceDefinitionNameProp =
      new StringProperty(13, "RESOURCE_DEFINITION_NAME", "");
  private final StringProperty tenantIdProp =
      new StringProperty(14, TENANT_ID, TenantOwned.DEFAULT_TENANT_IDENTIFIER);
  private final ArrayProperty<StarterEventEntity> starterEventsProp =
      new ArrayProperty<>(15, "STARTER_EVENTS", StarterEventEntity::new);

  public ProcessDefinitionEntity() {
    super(15);
    declareProperty(processDefinitionIdProp)
        .declareProperty(processDefinitionNameProp)
        .declareProperty(processDefinitionKeyProp)
        .declareProperty(processDefinitionVersionProp)
        .declareProperty(suspensionProp)
        .declareProperty(startableProp)
        .declareProperty(historyTimeToLiveProp)
        .declareProperty(candidateGroupsProp)
        .declareProperty(candidateUsersProp)
        .declareProperty(versionTagProp)
        .declareProperty(deploymentIdProp)
        .declareProperty(resourceDefinitionIdProp)
        .declareProperty(resourceDefinitionNameProp)
        .declareProperty(tenantIdProp)
        .declareProperty(starterEventsProp);
  }

  public void wrap(final ProcessDefinitionRecord record) {
    setProcessDefinitionId(record.getProcessDefinitionId())
        .setProcessDefinitionName(record.getProcessDefinitionNameBuffer())
        .setProcessDefinitionKey(record.getProcessDefinitionKeyBuffer())
        .setProcessDefinitionVersion(record.getProcessDefinitionVersion())
        .setSuspension(record.isSuspension())
        .setStartable(record.isStartable())
        .setHistoryTimeToLive(record.getHistoryTimeToLive())
        .setCandidateStarterGroups(record.getCandidateStarterGroups())
        .setCandidateStarterUsers(record.getCandidateStarterUsers())
        .setVersionTag(record.getVersionTagBuffer())
        .setDeploymentId(record.getDeploymentId())
        .setResourceDefinitionId(record.getResourceDefinitionId())
        .setResourceDefinitionName(record.getResourceDefinitionNameBuffer())
        .setTenantId(record.getTenantIdBuffer());
    starterEventsProp.reset();
    for (final StarterEventRecord event : record.starterEvents()) {
      starterEventsProp
          .add()
          .setStartEventId(event.getStartEventId())
          .setStartEventName(event.getStartEventNameBuffer())
          .setActivityDefinitionKey(event.getActivityDefinitionKeyBuffer())
          .setType(event.getType());
    }
  }

  public ProcessDefinitionRecord unwrap(final ProcessDefinitionRecord definitionRecord) {
    definitionRecord.reset();
    final ArrayProperty<StarterEventRecord> recordEvents = definitionRecord.starterEvents();
    recordEvents.reset();
    for (final StarterEventEntity event : starterEventsProp) {
      recordEvents
          .add()
          .setStartEventId(event.getStartEventId())
          .setStartEventName(event.getStartEventNameBuffer())
          .setActivityDefinitionKey(event.getActivityDefinitionKeyBuffer())
          .setType(event.getType());
    }
    return definitionRecord
        .setProcessDefinitionId(getProcessDefinitionId())
        .setProcessDefinitionName(getProcessDefinitionNameBuffer())
        .setProcessDefinitionKey(getProcessDefinitionKeyBuffer())
        .setProcessDefinitionVersion(getProcessDefinitionVersion())
        .setSuspension(isSuspension())
        .setStartable(isStartable())
        .setHistoryTimeToLive(getHistoryTimeToLive())
        .setCandidateStarterGroups(getCandidateStarterGroups())
        .setCandidateStarterUsers(getCandidateStarterUsers())
        .setVersionTag(getVersionTagBuffer())
        .setDeploymentId(getDeploymentId())
        .setResourceDefinitionId(getResourceDefinitionId())
        .setResourceDefinitionName(getResourceDefinitionNameBuffer())
        .setTenantId(getTenantIdBuffer());
  }

  public long getProcessDefinitionId() {
    return processDefinitionIdProp.getValue();
  }

  public ProcessDefinitionEntity setProcessDefinitionId(final long processDefinitionId) {
    processDefinitionIdProp.setValue(processDefinitionId);
    return this;
  }

  public String getProcessDefinitionName() {
    return bufferAsString(processDefinitionNameProp.getValue());
  }

  public DirectBuffer getProcessDefinitionNameBuffer() {
    return processDefinitionNameProp.getValue();
  }

  public ProcessDefinitionEntity setProcessDefinitionName(final String processDefinitionName) {
    processDefinitionNameProp.setValue(wrapString(processDefinitionName));
    return this;
  }

  public ProcessDefinitionEntity setProcessDefinitionName(
      final DirectBuffer processDefinitionName) {
    processDefinitionNameProp.setValue(processDefinitionName);
    return this;
  }

  public String getProcessDefinitionKey() {
    return bufferAsString(processDefinitionKeyProp.getValue());
  }

  public DirectBuffer getProcessDefinitionKeyBuffer() {
    return processDefinitionKeyProp.getValue();
  }

  public ProcessDefinitionEntity setProcessDefinitionKey(final String processDefinitionKey) {
    processDefinitionKeyProp.setValue(wrapString(processDefinitionKey));
    return this;
  }

  public ProcessDefinitionEntity setProcessDefinitionKey(final DirectBuffer processDefinitionKey) {
    processDefinitionKeyProp.setValue(processDefinitionKey);
    return this;
  }

  public int getProcessDefinitionVersion() {
    return processDefinitionVersionProp.getValue();
  }

  public ProcessDefinitionEntity setProcessDefinitionVersion(final int processDefinitionVersion) {
    processDefinitionVersionProp.setValue(processDefinitionVersion);
    return this;
  }

  public boolean isSuspension() {
    return suspensionProp.getValue();
  }

  public ProcessDefinitionEntity setSuspension(final boolean suspension) {
    suspensionProp.setValue(suspension);
    return this;
  }

  public boolean isStartable() {
    return startableProp.getValue();
  }

  public ProcessDefinitionEntity setStartable(final boolean startable) {
    startableProp.setValue(startable);
    return this;
  }

  public int getHistoryTimeToLive() {
    return historyTimeToLiveProp.getValue();
  }

  public ProcessDefinitionEntity setHistoryTimeToLive(final int historyTimeToLive) {
    historyTimeToLiveProp.setValue(historyTimeToLive);
    return this;
  }

  public Set<String> getCandidateStarterGroups() {
    final Set<String> set = new HashSet<>();
    if (candidateGroupsProp.hasValue()) {
      for (final StringValue candidateGroupBuffer : candidateGroupsProp) {
        set.add(bufferAsString(candidateGroupBuffer.getValue()));
      }
    }
    return set;
  }

  public ProcessDefinitionEntity setCandidateStarterGroups(
      final Set<String> candidateStarterGroups) {
    candidateGroupsProp.reset();
    if (candidateStarterGroups == null) {
      return this;
    }
    for (final String candidateStarterGroup : candidateStarterGroups) {
      candidateGroupsProp.add().wrap(wrapString(candidateStarterGroup));
    }
    return this;
  }

  public Set<String> getCandidateStarterUsers() {
    final Set<String> set = new HashSet<>();
    if (candidateUsersProp.hasValue()) {
      for (final StringValue candidateUserBuffer : candidateUsersProp) {
        set.add(bufferAsString(candidateUserBuffer.getValue()));
      }
    }
    return set;
  }

  public ProcessDefinitionEntity setCandidateStarterUsers(final Set<String> candidateStarterUsers) {
    candidateUsersProp.reset();
    if (candidateStarterUsers == null) {
      return this;
    }
    for (final String candidateStarterUser : candidateStarterUsers) {
      candidateUsersProp.add().wrap(wrapString(candidateStarterUser));
    }
    return this;
  }

  public String getVersionTag() {
    return bufferAsString(versionTagProp.getValue());
  }

  public DirectBuffer getVersionTagBuffer() {
    return versionTagProp.getValue();
  }

  public ProcessDefinitionEntity setVersionTag(final String versionTag) {
    versionTagProp.setValue(wrapString(versionTag));
    return this;
  }

  public ProcessDefinitionEntity setVersionTag(final DirectBuffer versionTag) {
    versionTagProp.setValue(versionTag);
    return this;
  }

  public long getDeploymentId() {
    return deploymentIdProp.getValue();
  }

  public ProcessDefinitionEntity setDeploymentId(final long deploymentId) {
    deploymentIdProp.setValue(deploymentId);
    return this;
  }

  public long getResourceDefinitionId() {
    return resourceDefinitionIdProp.getValue();
  }

  public ProcessDefinitionEntity setResourceDefinitionId(final long resourceDefinitionId) {
    resourceDefinitionIdProp.setValue(resourceDefinitionId);
    return this;
  }

  public String getResourceDefinitionName() {
    return bufferAsString(resourceDefinitionNameProp.getValue());
  }

  public DirectBuffer getResourceDefinitionNameBuffer() {
    return resourceDefinitionNameProp.getValue();
  }

  public ProcessDefinitionEntity setResourceDefinitionName(final String resourceDefinitionName) {
    if (resourceDefinitionName != null) {
      resourceDefinitionNameProp.setValue(wrapString(resourceDefinitionName));
    }
    return this;
  }

  public ProcessDefinitionEntity setResourceDefinitionName(
      final DirectBuffer resourceDefinitionName) {
    resourceDefinitionNameProp.setValue(resourceDefinitionName);
    return this;
  }

  public String getTenantId() {
    return bufferAsString(tenantIdProp.getValue());
  }

  public DirectBuffer getTenantIdBuffer() {
    return tenantIdProp.getValue();
  }

  public ProcessDefinitionEntity setTenantId(final String tenantId) {
    if (tenantId != null) {
      tenantIdProp.setValue(wrapString(tenantId));
    }
    return this;
  }

  public ProcessDefinitionEntity setTenantId(final DirectBuffer tenantId) {
    tenantIdProp.setValue(tenantId);
    return this;
  }

  /** 启动事件列表（原始数组访问） */
  public ArrayProperty<StarterEventEntity> starterEvents() {
    return starterEventsProp;
  }

  public List<StarterEventEntity> getStarterEvents() {
    final List<StarterEventEntity> list = new ArrayList<>(starterEventsProp.size());
    for (final StarterEventEntity event : starterEventsProp) {
      list.add(event);
    }
    return list;
  }

  public ProcessDefinitionEntity setStarterEvents(final List<StarterEventEntity> starterEvents) {
    starterEventsProp.reset();
    if (starterEvents == null) {
      return this;
    }
    for (final StarterEventEntity event : starterEvents) {
      starterEventsProp
          .add()
          .setStartEventId(event.getStartEventId())
          .setStartEventName(event.getStartEventNameBuffer())
          .setActivityDefinitionKey(event.getActivityDefinitionKeyBuffer())
          .setType(event.getType());
    }
    return this;
  }
}
