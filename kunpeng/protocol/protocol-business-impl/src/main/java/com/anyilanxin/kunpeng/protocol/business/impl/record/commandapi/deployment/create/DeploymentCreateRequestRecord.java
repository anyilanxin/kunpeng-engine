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
package com.anyilanxin.kunpeng.protocol.business.impl.record.commandapi.deployment.create;

import static com.anyilanxin.kunpeng.protocol.business.impl.BusinessRecordConstant.TENANT_ID;
import static com.anyilanxin.kunpeng.structpack.util.BufferUtil.wrapString;

import com.anyilanxin.kunpeng.protocol.business.impl.record.command.deployment.ResourceRecord;
import com.anyilanxin.kunpeng.protocol.business.record.commandapi.record.deployment.create.DeploymentCreateRequestRecordValue;
import com.anyilanxin.kunpeng.protocol.common.TenantOwned;
import com.anyilanxin.kunpeng.protocol.common.UnifiedRecordValue;
import com.anyilanxin.kunpeng.structpack.AutoDeclareProperties;
import com.anyilanxin.kunpeng.structpack.property.ArrayProperty;
import com.anyilanxin.kunpeng.structpack.property.LongProperty;
import com.anyilanxin.kunpeng.structpack.property.StringProperty;
import org.agrona.DirectBuffer;

/**
 * 部署创建请求 Record。
 *
 * @author zxuanhong
 * @since 2026.9.0
 */
@AutoDeclareProperties
public class DeploymentCreateRequestRecord extends UnifiedRecordValue<DeploymentCreateRequestRecord>
    implements DeploymentCreateRequestRecordValue {
  // structpack-ids[DeploymentCreateRequestRecord]: 1,2,3,4
  private final StringProperty deploymentNameProp = new StringProperty(1, "DEPLOYMENT_NAME", "");
  private final LongProperty processDefinitionsActivateProp =
      new LongProperty(2, "PROCESS_DEFINITIONS_ACTIVATE", -1);
  private final StringProperty tenantIdProp =
      new StringProperty(4, TENANT_ID, TenantOwned.DEFAULT_TENANT_IDENTIFIER);
  private final ArrayProperty<ResourceRecord> resourcesProp =
      new ArrayProperty<>(3, "RESOURCES", ResourceRecord::new);

  public DeploymentCreateRequestRecord() {
    super(4);
    declareProperty(deploymentNameProp)
        .declareProperty(processDefinitionsActivateProp)
        .declareProperty(tenantIdProp)
        .declareProperty(resourcesProp);
  }

  @Override
  public DeploymentCreateRequestRecordValue setDeploymentName(final String deploymentName) {
    deploymentNameProp.setValue(wrapString(deploymentName));
    return this;
  }

  public DirectBuffer getDeploymentName() {
    return deploymentNameProp.getValue();
  }

  @Override
  public DeploymentCreateRequestRecordValue setProcessDefinitionsActivate(
      final long processDefinitionsActivate) {
    processDefinitionsActivateProp.setValue(processDefinitionsActivate);
    return this;
  }

  public long getProcessDefinitionsActivate() {
    return processDefinitionsActivateProp.getValue();
  }

  @Override
  public DeploymentCreateRequestRecordValue setTenantId(final String tenantId) {
    tenantIdProp.setValue(wrapString(tenantId));
    return this;
  }

  public DirectBuffer getTenantId() {
    return tenantIdProp.getValue();
  }

  @Override
  public DeploymentCreateRequestRecordValue addResources(
      final String resourceName, final byte[] bytes) {
    resourcesProp.add().setResourceName(resourceName).setBytes(bytes);
    return this;
  }

  public ArrayProperty<ResourceRecord> resources() {
    return resourcesProp;
  }
}
