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
package com.anyilanxin.kunpeng.engine.bpmn.commandapi.deployment.processor;

import com.anyilanxin.kunpeng.engine.bpmn.LogEventWriter;
import com.anyilanxin.kunpeng.engine.bpmn.commandapi.deployment.DeploymentApiAbstractProcessor;
import com.anyilanxin.kunpeng.protocol.business.impl.eventlog.BusinessLogRecord;
import com.anyilanxin.kunpeng.protocol.business.impl.record.command.deployment.DeploymentRecord;
import com.anyilanxin.kunpeng.protocol.business.impl.record.command.deployment.ResourceRecord;
import com.anyilanxin.kunpeng.protocol.business.impl.record.commandapi.deployment.create.DeploymentCreateRequestRecord;
import com.anyilanxin.kunpeng.protocol.business.record.command.deployment.DeploymentLifeCycle;
import com.anyilanxin.kunpeng.protocol.business.record.commandapi.record.deployment.CommandApiDeploymentValueLifeCycle;
import org.agrona.DirectBuffer;

/**
 * 部署创建 API 处理器。
 *
 * @author zxuanhong
 * @since 2026.9.0
 */
public class DeploymentCreateApiProcessor
    extends DeploymentApiAbstractProcessor<DeploymentCreateRequestRecord> {
  private final LogEventWriter writer;
  private final DeploymentRecord deploymentRecord;

  public DeploymentCreateApiProcessor(final LogEventWriter writer) {
    this.writer = writer;
    deploymentRecord = new DeploymentRecord();
  }

  @Override
  public void processRecord(final BusinessLogRecord<DeploymentCreateRequestRecord> record) {
    final DeploymentCreateRequestRecord value = record.getValue();
    deploymentRecord.reset();
    deploymentRecord.setDeploymentId(writer.nextGlobalKey());
    deploymentRecord.setActivateProcessDefinitionsOn(value.getProcessDefinitionsActivate());
    deploymentRecord.setDeploymentName(value.getDeploymentName());
    deploymentRecord.setTenantId(value.getTenantId());
    for (final ResourceRecord resource : value.resources()) {
      final DirectBuffer bytes = resource.getBytesBuffer();
      deploymentRecord
          .resourceDefinitions()
          .add()
          .setResource(bytes, 0, bytes.capacity())
          .setResourceDefinitionName(resource.getResourceNameBuffer());
    }
    writer.addCommand(
        deploymentRecord.getDeploymentId(),
        DeploymentLifeCycle.CREATE,
        record.getRequestId(),
        deploymentRecord);
  }

  @Override
  public CommandApiDeploymentValueLifeCycle valueLifeCycle() {
    return CommandApiDeploymentValueLifeCycle.CREATE_REQUEST;
  }
}
