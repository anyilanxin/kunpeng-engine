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

import static com.anyilanxin.kunpeng.protocol.business.impl.BusinessRecordConstant.*;
import static com.anyilanxin.kunpeng.structpack.util.BufferUtil.*;

import com.anyilanxin.kunpeng.protocol.business.record.command.deployment.ProcessDefinitionRecordValue;
import com.anyilanxin.kunpeng.protocol.business.record.command.deployment.StarterEventRecordValue;
import com.anyilanxin.kunpeng.protocol.common.TenantOwned;
import com.anyilanxin.kunpeng.protocol.common.UnifiedRecordValue;
import com.anyilanxin.kunpeng.structpack.AutoDeclareProperties;
import com.anyilanxin.kunpeng.structpack.property.*;
import com.anyilanxin.kunpeng.structpack.util.BufferUtil;
import com.anyilanxin.kunpeng.structpack.value.StringValue;
import com.fasterxml.jackson.annotation.JsonIgnore;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.agrona.DirectBuffer;
import org.agrona.concurrent.UnsafeBuffer;

/**
 * 流程定义记录
 *
 * @author zxuanhong
 * @since 2026.9.0
 */
@AutoDeclareProperties
public class ProcessDefinitionRecord extends UnifiedRecordValue<ProcessDefinitionRecord>
    implements ProcessDefinitionRecordValue {
  private final LongProperty processDefinitionIdProp =
      new LongProperty(11, PROCESS_DEFINITION_ID, -1);
  private final StringProperty processDefinitionNameProp =
      new StringProperty(12, PROCESS_DEFINITION_NAME, "");
  private final StringProperty processDefinitionKeyProp =
      new StringProperty(13, PROCESS_DEFINITION_KEY, "");
  private final IntegerProperty processDefinitionVersionProp =
      new IntegerProperty(14, PROCESS_DEFINITION_VERSION, -1);
  private final BooleanProperty suspensionProp = new BooleanProperty(1, "SUSPENSION", true);
  private final BooleanProperty startableProp = new BooleanProperty(2, "STARTABLE", true);
  private final IntegerProperty historyTimeToLiveProp =
      new IntegerProperty(3, "HISTORY_TIME_TO_LIVE", -1);
  private final ArrayProperty<StringValue> candidateGroupsProp =
      new ArrayProperty<>(4, "CANDIDATE_STARTER_GROUPS", StringValue::new);
  private final ArrayProperty<StringValue> candidateUsersProp =
      new ArrayProperty<>(5, "CANDIDATE_STARTER_USERS", StringValue::new);
  private final StringProperty versionTagProp = new StringProperty(15, VERSION_TAG, "");
  private final LongProperty deploymentIdProp = new LongProperty(16, DEPLOYMENT_ID, -1);
  private final LongProperty resourceDefinitionIdProp =
      new LongProperty(6, "RESOURCE_DEFINITION_ID", -1);
  private final StringProperty resourceDefinitionNameProp =
      new StringProperty(7, "RESOURCE_DEFINITION_NAME", "");
  private final BinaryProperty checksumProp = new BinaryProperty(8, "CHECKSUM", new UnsafeBuffer());
  private final BinaryProperty resourceProp = new BinaryProperty(9, "RESOURCE", new UnsafeBuffer());
  private final StringProperty tenantIdProp =
      new StringProperty(17, TENANT_ID, TenantOwned.DEFAULT_TENANT_IDENTIFIER);
  private final ArrayProperty<StarterEventRecord> starterEventsProp =
      new ArrayProperty<>(10, "STARTER_EVENTS", StarterEventRecord::new);

  public ProcessDefinitionRecord() {
    super(17);
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
        .declareProperty(checksumProp)
        .declareProperty(resourceProp)
        .declareProperty(tenantIdProp)
        .declareProperty(starterEventsProp);
  }

  @Override
  public long getProcessDefinitionId() {
    return processDefinitionIdProp.getValue();
  }

  public ProcessDefinitionRecord setProcessDefinitionId(final long processDefinitionId) {
    processDefinitionIdProp.setValue(processDefinitionId);
    return this;
  }

  @Override
  public String getProcessDefinitionName() {
    return bufferAsString(processDefinitionNameProp.getValue());
  }

  @JsonIgnore
  public DirectBuffer getProcessDefinitionNameBuffer() {
    return processDefinitionNameProp.getValue();
  }

  public ProcessDefinitionRecord setProcessDefinitionName(final String processDefinitionName) {
    if (processDefinitionName != null) {
      processDefinitionNameProp.setValue(wrapString(processDefinitionName));
    }
    return this;
  }

  public ProcessDefinitionRecord setProcessDefinitionName(
      final DirectBuffer processDefinitionName) {
    processDefinitionNameProp.setValue(processDefinitionName);
    return this;
  }

  @Override
  public String getProcessDefinitionKey() {
    return bufferAsString(processDefinitionKeyProp.getValue());
  }

  @JsonIgnore
  public DirectBuffer getProcessDefinitionKeyBuffer() {
    return processDefinitionKeyProp.getValue();
  }

  public ProcessDefinitionRecord setProcessDefinitionKey(final String processDefinitionKey) {
    if (processDefinitionKey != null) {
      processDefinitionKeyProp.setValue(wrapString(processDefinitionKey));
    }
    return this;
  }

  public ProcessDefinitionRecord setProcessDefinitionKey(final DirectBuffer processDefinitionKey) {
    processDefinitionKeyProp.setValue(processDefinitionKey);
    return this;
  }

  @Override
  public int getProcessDefinitionVersion() {
    return processDefinitionVersionProp.getValue();
  }

  public ProcessDefinitionRecord setProcessDefinitionVersion(final int processDefinitionVersion) {
    processDefinitionVersionProp.setValue(processDefinitionVersion);
    return this;
  }

  @Override
  public boolean isSuspension() {
    return suspensionProp.getValue();
  }

  public ProcessDefinitionRecord setSuspension(final boolean suspension) {
    suspensionProp.setValue(suspension);
    return this;
  }

  @Override
  public boolean isStartable() {
    return startableProp.getValue();
  }

  public ProcessDefinitionRecord setStartable(final boolean startable) {
    startableProp.setValue(startable);
    return this;
  }

  @Override
  public int getHistoryTimeToLive() {
    return historyTimeToLiveProp.getValue();
  }

  public ProcessDefinitionRecord setHistoryTimeToLive(final int historyTimeToLive) {
    historyTimeToLiveProp.setValue(historyTimeToLive);
    return this;
  }

  @Override
  public Set<String> getCandidateStarterGroups() {
    final Set<String> set = new HashSet<>();
    if (candidateGroupsProp.hasValue()) {
      for (final StringValue candidateGroupBuffer : candidateGroupsProp) {
        set.add(bufferAsString(candidateGroupBuffer.getValue()));
      }
    }
    return set;
  }

  public ProcessDefinitionRecord setCandidateStarterGroups(
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

  @Override
  public Set<String> getCandidateStarterUsers() {
    final Set<String> set = new HashSet<>();
    if (candidateUsersProp.hasValue()) {
      for (final StringValue candidateUserBuffer : candidateUsersProp) {
        set.add(bufferAsString(candidateUserBuffer.getValue()));
      }
    }
    return set;
  }

  public ProcessDefinitionRecord setCandidateStarterUsers(final Set<String> candidateStarterUsers) {
    candidateUsersProp.reset();
    if (candidateStarterUsers == null) {
      return this;
    }
    for (final String candidateStarterUser : candidateStarterUsers) {
      candidateUsersProp.add().wrap(wrapString(candidateStarterUser));
    }
    return this;
  }

  @Override
  public String getVersionTag() {
    return bufferAsString(versionTagProp.getValue());
  }

  @JsonIgnore
  public DirectBuffer getVersionTagBuffer() {
    return versionTagProp.getValue();
  }

  public ProcessDefinitionRecord setVersionTag(final String versionTag) {
    if (versionTag != null) {
      versionTagProp.setValue(wrapString(versionTag));
    }
    return this;
  }

  public ProcessDefinitionRecord setVersionTag(final DirectBuffer versionTag) {
    versionTagProp.setValue(versionTag);
    return this;
  }

  @Override
  public long getDeploymentId() {
    return deploymentIdProp.getValue();
  }

  public ProcessDefinitionRecord setDeploymentId(final long deploymentId) {
    deploymentIdProp.setValue(deploymentId);
    return this;
  }

  @Override
  public long getResourceDefinitionId() {
    return resourceDefinitionIdProp.getValue();
  }

  public ProcessDefinitionRecord setResourceDefinitionId(final long resourceId) {
    resourceDefinitionIdProp.setValue(resourceId);
    return this;
  }

  @Override
  public String getResourceDefinitionName() {
    return bufferAsString(resourceDefinitionNameProp.getValue());
  }

  @JsonIgnore
  public DirectBuffer getResourceDefinitionNameBuffer() {
    return resourceDefinitionNameProp.getValue();
  }

  public ProcessDefinitionRecord setResourceDefinitionName(final String resourceName) {
    if (resourceName != null) {
      resourceDefinitionNameProp.setValue(wrapString(resourceName));
    }
    return this;
  }

  public ProcessDefinitionRecord setResourceDefinitionName(final DirectBuffer resourceName) {
    resourceDefinitionNameProp.setValue(resourceName);
    return this;
  }

  @Override
  public byte[] getChecksum() {
    return bufferAsArray(checksumProp.getValue());
  }

  @JsonIgnore
  public DirectBuffer getChecksumBuffer() {
    return checksumProp.getValue();
  }

  public ProcessDefinitionRecord setChecksum(
      final DirectBuffer checksum, final int offset, final int length) {
    checksumProp.setValue(checksum, offset, length);
    return this;
  }

  public ProcessDefinitionRecord setChecksum(final byte[] checksum) {
    checksumProp.setValue(BufferUtil.wrapArray(checksum));
    return this;
  }

  @Override
  public byte[] getResource() {
    return bufferAsArray(resourceProp.getValue());
  }

  @JsonIgnore
  public DirectBuffer getResourceBuffer() {
    return resourceProp.getValue();
  }

  public ProcessDefinitionRecord setResource(
      final DirectBuffer resource, final int offset, final int length) {
    resourceProp.setValue(resource, offset, length);
    return this;
  }

  public ProcessDefinitionRecord setResource(final byte[] resource) {
    resourceProp.setValue(BufferUtil.wrapArray(resource));
    return this;
  }

  @Override
  public String getTenantId() {
    return bufferAsString(tenantIdProp.getValue());
  }

  @JsonIgnore
  public DirectBuffer getTenantIdBuffer() {
    return tenantIdProp.getValue();
  }

  public ProcessDefinitionRecord setTenantId(final String tenantId) {
    if (tenantId != null) {
      tenantIdProp.setValue(wrapString(tenantId));
    }
    return this;
  }

  public ProcessDefinitionRecord setTenantId(final DirectBuffer tenantId) {
    tenantIdProp.setValue(tenantId);
    return this;
  }

  /** 启动事件列表（原始数组访问，引擎处理器可直接 add() 链式追加） */
  public ArrayProperty<StarterEventRecord> starterEvents() {
    return starterEventsProp;
  }

  @Override
  public List<StarterEventRecordValue> getStarterEvents() {
    final List<StarterEventRecordValue> list = new ArrayList<>(starterEventsProp.size());
    for (final StarterEventRecord record : starterEventsProp) {
      list.add(record);
    }
    return list;
  }

  @Override
  protected ProcessDefinitionRecord newRecord() {
    return new ProcessDefinitionRecord();
  }
}
