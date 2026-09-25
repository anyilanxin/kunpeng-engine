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
package com.anyilanxin.kunpeng.repository.business.modules.deployment.deployment.record;

import static com.anyilanxin.kunpeng.protocol.business.impl.BusinessRecordConstant.DEPLOYMENT_ID;
import static com.anyilanxin.kunpeng.protocol.business.impl.BusinessRecordConstant.TENANT_ID;
import static com.anyilanxin.kunpeng.structpack.util.BufferUtil.bufferAsString;
import static com.anyilanxin.kunpeng.structpack.util.BufferUtil.wrapString;

import com.anyilanxin.kunpeng.kvstore.types.StoreValue;
import com.anyilanxin.kunpeng.protocol.business.impl.record.command.deployment.DeploymentRecord;
import com.anyilanxin.kunpeng.protocol.common.TenantOwned;
import com.anyilanxin.kunpeng.structpack.AutoDeclareProperties;
import com.anyilanxin.kunpeng.structpack.UnpackedObject;
import com.anyilanxin.kunpeng.structpack.property.LongProperty;
import com.anyilanxin.kunpeng.structpack.property.StringProperty;
import org.agrona.DirectBuffer;

/**
 * 部署 Entity：部署 Record 的落库映射。
 *
 * @author zxuanhong
 * @since 2026.9.0
 */
@AutoDeclareProperties
public class DeploymentEntity extends UnpackedObject implements StoreValue {
  // structpack-ids[DeploymentEntity]: 1,2,3,4,5
  private final LongProperty deploymentIdProp = new LongProperty(1, DEPLOYMENT_ID, -1);
  private final StringProperty deploymentNameProp = new StringProperty(2, "DEPLOYMENT_NAME", "");
  private final LongProperty deploymentTimeProp = new LongProperty(3, "DEPLOYMENT_TIME", -1);
  private final LongProperty activateProcessDefinitionsOnProp =
      new LongProperty(4, "ACTIVATE_PROCESS_DEFINITIONS_ON", -1);
  private final StringProperty tenantIdProp =
      new StringProperty(5, TENANT_ID, TenantOwned.DEFAULT_TENANT_IDENTIFIER);

  public DeploymentEntity() {
    super(5);
    declareProperty(deploymentIdProp)
        .declareProperty(deploymentNameProp)
        .declareProperty(deploymentTimeProp)
        .declareProperty(activateProcessDefinitionsOnProp)
        .declareProperty(tenantIdProp);
  }

  public void wrap(final DeploymentRecord record) {
    setDeploymentId(record.getDeploymentId())
        .setDeploymentName(record.getDeploymentNameBuffer())
        .setDeploymentTime(record.getDeploymentTime())
        .setActivateProcessDefinitionsOn(record.getActivateProcessDefinitionsOn())
        .setTenantId(record.getTenantIdBuffer());
  }

  public DeploymentRecord unwrap(final DeploymentRecord record) {
    record.reset();
    return record
        .setDeploymentId(getDeploymentId())
        .setDeploymentName(getDeploymentNameBuffer())
        .setDeploymentTime(getDeploymentTime())
        .setActivateProcessDefinitionsOn(getActivateProcessDefinitionsOn())
        .setTenantId(getTenantIdBuffer());
  }

  public long getDeploymentId() {
    return deploymentIdProp.getValue();
  }

  public DeploymentEntity setDeploymentId(final long deploymentId) {
    deploymentIdProp.setValue(deploymentId);
    return this;
  }

  public String getDeploymentName() {
    return bufferAsString(deploymentNameProp.getValue());
  }

  public DirectBuffer getDeploymentNameBuffer() {
    return deploymentNameProp.getValue();
  }

  public DeploymentEntity setDeploymentName(final String deploymentName) {
    if (deploymentName != null) {
      deploymentNameProp.setValue(wrapString(deploymentName));
    }
    return this;
  }

  public DeploymentEntity setDeploymentName(final DirectBuffer deploymentName) {
    deploymentNameProp.setValue(deploymentName);
    return this;
  }

  public long getDeploymentTime() {
    return deploymentTimeProp.getValue();
  }

  public DeploymentEntity setDeploymentTime(final long deploymentTime) {
    deploymentTimeProp.setValue(deploymentTime);
    return this;
  }

  public long getActivateProcessDefinitionsOn() {
    return activateProcessDefinitionsOnProp.getValue();
  }

  public DeploymentEntity setActivateProcessDefinitionsOn(final long activateProcessDefinitionsOn) {
    activateProcessDefinitionsOnProp.setValue(activateProcessDefinitionsOn);
    return this;
  }

  public String getTenantId() {
    return bufferAsString(tenantIdProp.getValue());
  }

  public DirectBuffer getTenantIdBuffer() {
    return tenantIdProp.getValue();
  }

  public DeploymentEntity setTenantId(final String tenantId) {
    if (tenantId != null) {
      tenantIdProp.setValue(wrapString(tenantId));
    }
    return this;
  }

  public DeploymentEntity setTenantId(final DirectBuffer tenantId) {
    tenantIdProp.setValue(tenantId);
    return this;
  }
}
