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
package com.anyilanxin.kunpeng.engine.bpmn.command.deployment.processor;

import com.anyilanxin.kunpeng.bpm.model.bpmn.Bpmn;
import com.anyilanxin.kunpeng.bpm.model.bpmn.BpmnModelInstance;
import com.anyilanxin.kunpeng.bpm.model.dmn.Dmn;
import com.anyilanxin.kunpeng.bpm.model.dmn.DmnModelInstance;
import com.anyilanxin.kunpeng.bpm.parse.bpmn.BpmnValidator;
import com.anyilanxin.kunpeng.bpm.parse.bpmn.element.BpmnProcess;
import com.anyilanxin.kunpeng.bpm.parse.bpmn.transformation.BpmnTransformer;
import com.anyilanxin.kunpeng.bpm.parse.dmn.DmnValidator;
import com.anyilanxin.kunpeng.bpm.parse.dmn.element.DmnDecision;
import com.anyilanxin.kunpeng.bpm.parse.dmn.element.DmnDecisionRequirementsGraph;
import com.anyilanxin.kunpeng.bpm.parse.dmn.transformation.DmnTransformer;
import com.anyilanxin.kunpeng.engine.bpmn.LogEventWriter;
import com.anyilanxin.kunpeng.engine.bpmn.command.LogEventDistributeProcessor;
import com.anyilanxin.kunpeng.engine.bpmn.command.behavior.Behavior;
import com.anyilanxin.kunpeng.engine.bpmn.command.behavior.impl.CatchEventBehavior;
import com.anyilanxin.kunpeng.engine.bpmn.command.behavior.impl.DistributeParallelBehavior;
import com.anyilanxin.kunpeng.engine.bpmn.command.behavior.impl.ProcessDefinitionBehavior;
import com.anyilanxin.kunpeng.engine.bpmn.exception.EngineRollbackException;
import com.anyilanxin.kunpeng.protocol.business.ValueLifeCycle;
import com.anyilanxin.kunpeng.protocol.business.ValueType;
import com.anyilanxin.kunpeng.protocol.business.impl.eventlog.BusinessLogRecord;
import com.anyilanxin.kunpeng.protocol.business.impl.record.command.deployment.*;
import com.anyilanxin.kunpeng.protocol.business.impl.record.commandapi.deployment.create.DeploymentCreateResponseRecord;
import com.anyilanxin.kunpeng.protocol.business.record.command.deployment.*;
import com.anyilanxin.kunpeng.protocol.business.record.commandapi.record.deployment.CommandApiDeploymentValueLifeCycle;
import com.anyilanxin.kunpeng.repository.business.ImmutableBusinessRepository;
import com.anyilanxin.kunpeng.repository.business.modules.deployment.bpmnresource.ImmutableBpmnResourceRepository;
import com.anyilanxin.kunpeng.repository.business.modules.deployment.bpmnresource.record.ProcessDefinitionRuntime;
import com.anyilanxin.kunpeng.repository.business.modules.deployment.dmnresource.ImmutableDmnResourceRepository;
import com.anyilanxin.kunpeng.structpack.value.ValueArray;
import java.util.List;
import org.agrona.DirectBuffer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 部署创建命令处理器：解析流程定义并分发至各分区。
 *
 * @author zxuanhong
 * @since 2026.9.0
 */
public class DeploymentCreateProcessor extends LogEventDistributeProcessor<DeploymentRecord> {
  private static final Logger LOG = LoggerFactory.getLogger(DeploymentCreateProcessor.class);
  final LogEventWriter writer;
  private final BpmnTransformer bpmnTransformer;
  private final BpmnValidator bpmnValidator;
  private final ImmutableBpmnResourceRepository bpmnResource;
  private final ProcessDefinitionBehavior processDefinitionBehavior;
  private final CatchEventBehavior catchEventBehavior;
  private final DeploymentCreateResponseRecord response;
  private final DmnTransformer dmnTransformer;
  private final DmnValidator dmnValidator;
  private final ImmutableDmnResourceRepository dmnResource;
  private final DistributeParallelBehavior distributeParallelBehavior;

  public DeploymentCreateProcessor(final LogEventWriter writer) {
    super(writer);
    this.writer = writer;
    bpmnTransformer = writer.getBpmnTransformer();
    bpmnValidator = writer.getBpmnValidator();

    dmnTransformer = writer.getDmnTransformer();
    dmnValidator = writer.getDmnValidator();
    final ImmutableBusinessRepository repository = writer.getRepository();
    bpmnResource = repository.bpmnResourceRepository();
    dmnResource = repository.dmnResourceRepository();
    final Behavior behavior = writer.behavior();
    processDefinitionBehavior = behavior.processDefinitionBehavior();
    catchEventBehavior = behavior.catchEvent();
    distributeParallelBehavior = behavior.distributeParallelBehavior();
    response = new DeploymentCreateResponseRecord();
  }

  @Override
  public void processRecordNew(final BusinessLogRecord<DeploymentRecord> record) {
    response.reset();
    final long requestId = record.getRequestId();
    final DeploymentRecord deploymentRecord = record.getValue();
    writer.addEvent(
        deploymentRecord.getDeploymentId(),
        DeploymentLifeCycle.CREATED,
        record.getRequestId(),
        deploymentRecord);
    final ValueArray<ResourceDefinitionRecord> resourceDefinitions =
        deploymentRecord.resourceDefinitions();
    for (final ResourceDefinitionRecord resourceDefinition : resourceDefinitions) {
      resourceDefinition.setResourceDefinitionId(writer.nextGlobalKey());
      response.addResourceDefinition(resourceDefinition);
      switch (resourceDefinition.getResourceType()) {
        case DMN -> transformDmn(requestId, deploymentRecord, resourceDefinition);
        case BPMN -> transformBpmn(requestId, deploymentRecord, resourceDefinition);
        default -> {
          writer.adErrorResponse(requestId, -1, "不支持的文件类型");
          throw new EngineRollbackException("不支持的文件类型");
        }
      }
    }
    processDeployment(false, requestId, deploymentRecord);
    distributeParallelBehavior.addDistributeParallel(
        deploymentRecord.getDeploymentId(),
        processRecordDistributeLifeCycle(),
        processRecordDistributeAfterLifeCycle(),
        requestId,
        deploymentRecord);

    response
        .setDeploymentId(deploymentRecord.getDeploymentId())
        .setDeploymentName(deploymentRecord.getDeploymentNameBuffer())
        .setTenantId(deploymentRecord.getTenantIdBuffer());
    // 响应结果
    writer.adResponse(
        CommandApiDeploymentValueLifeCycle.CREATE_RESPONSE, record.getRequestId(), response);
  }

  @Override
  public void processRecordDistribute(final BusinessLogRecord<DeploymentRecord> record) {
    LOG.debug("processRecordDistribute source {}", writer.getSourceId());
    final DeploymentRecord deploymentRecord = record.getValue();
    writer.addEvent(
        deploymentRecord.getDeploymentId(),
        DeploymentLifeCycle.CREATED,
        record.getRequestId(),
        deploymentRecord);
    processDeployment(true, record.getRequestId(), deploymentRecord);
    distributeParallelBehavior.distributeParallelAck(record);
  }

  @Override
  public void processRecordDistributeAfter(final BusinessLogRecord<DeploymentRecord> record) {
    final DeploymentRecord deploymentRecord = record.getValue();
    // 处理定时任务激活
    for (final ProcessDefinitionRecord definitionRecord : deploymentRecord.processDefinitions()) {

      final ProcessDefinitionRuntime processDefinitionCache =
          bpmnResource.getRuntime(definitionRecord.getProcessDefinitionId());
      final BpmnProcess executableProcess = processDefinitionCache.executableProcess();
      processDefinitionBehavior.registerStartTimerEvent(
          definitionRecord, executableProcess.getStartEvents());
    }
    distributeParallelBehavior.distributeParallelAfterAck(record);
  }

  @Override
  public ValueType valueType() {
    return ValueType.DEPLOYMENT;
  }

  @Override
  public ValueLifeCycle processRecordNewLifeCycle() {
    return DeploymentLifeCycle.CREATE;
  }

  @Override
  public ValueLifeCycle processRecordDistributeLifeCycle() {
    return DeploymentLifeCycle.CREATE_DISTRIBUTE;
  }

  @Override
  public ValueLifeCycle processRecordDistributeAfterLifeCycle() {
    return DeploymentLifeCycle.CREATE_DISTRIBUTE_AFTER;
  }

  /**
   * 处理部署资源的存储
   *
   * @param requestId
   * @param deploymentRecord
   */
  private void processDeployment(
      final boolean distribute, final long requestId, final DeploymentRecord deploymentRecord) {
    processResource(requestId, deploymentRecord.resourceDefinitions());
    processBpmn(
        distribute,
        requestId,
        deploymentRecord.getActivateProcessDefinitionsOn(),
        deploymentRecord.processDefinitions());
    processDmnDecision(requestId, deploymentRecord.decisionDefinitions());
    processDmnDecisionRequirement(requestId, deploymentRecord.decisionRequirementDefinitions());
  }

  /**
   * 处理资源
   *
   * @param requestId
   * @param resourceDefinitionRecords
   */
  private void processResource(
      final long requestId, final ValueArray<ResourceDefinitionRecord> resourceDefinitionRecords) {
    for (final ResourceDefinitionRecord resourceDefinition : resourceDefinitionRecords) {
      writer.addEvent(
          resourceDefinition.getResourceDefinitionId(),
          ResourceDefinitionLifeCycle.CREATED,
          requestId,
          resourceDefinition);
    }
  }

  /**
   * 处理 bpmn
   *
   * @param requestId
   * @param activateProcessDefinitionsOn
   * @param processDefinitionRecords
   */
  private void processBpmn(
      final boolean distribute,
      final long requestId,
      final long activateProcessDefinitionsOn,
      final ValueArray<ProcessDefinitionRecord> processDefinitionRecords) {
    for (final ProcessDefinitionRecord processDefinition : processDefinitionRecords) {
      writer.addEvent(
          processDefinition.getProcessDefinitionId(),
          ProcessDefinitionLifeCycle.CREATED,
          requestId,
          processDefinition);
      processDefinitionBehavior.cancelRegisterStartEvent(distribute, processDefinition);
      final boolean activation =
          activateProcessDefinitionsOn == -1 || activateProcessDefinitionsOn < writer.millis();
      if (activation) {
        writer.addEvent(
            processDefinition.getProcessDefinitionId(),
            ProcessDefinitionLifeCycle.ACTIVATED,
            requestId,
            processDefinition);
        final ProcessDefinitionRuntime executableProcess =
            bpmnResource.getRuntime(processDefinition.getProcessDefinitionId());
        if (distribute) {
          processDefinitionBehavior.registerStartEvent(processDefinition);
        } else {
          processDefinitionBehavior.registerStartEvent(
              processDefinition, executableProcess.executableProcess().getStartEvents());
        }
      } else {
        // 注册流程定义激活服务(此时应该是需要特定时间激活流程定义)
      }
    }
  }

  /**
   * 处理决策定音
   *
   * @param requestId
   * @param processDefinitionRecords
   */
  private void processDmnDecision(
      final long requestId, final ValueArray<DecisionDefinitionRecord> processDefinitionRecords) {
    for (final DecisionDefinitionRecord decisionDefinition : processDefinitionRecords) {
      writer.addEvent(
          decisionDefinition.getDecisionDefinitionId(),
          DecisionDefinitionLifeCycle.CREATED,
          requestId,
          decisionDefinition);
    }
  }

  /**
   * 处理决定需求
   *
   * @param requestId
   * @param processDefinitionRecords
   */
  private void processDmnDecisionRequirement(
      final long requestId,
      final ValueArray<DecisionRequirementDefinitionRecord> processDefinitionRecords) {
    for (final DecisionRequirementDefinitionRecord decisionRequirementDefinition :
        processDefinitionRecords) {
      writer.addEvent(
          decisionRequirementDefinition.getDecisionRequirementDefinitionId(),
          DecisionRequirementDefinitionLifeCycle.CREATED,
          requestId,
          decisionRequirementDefinition);
    }
  }

  /**
   * 转换为 bpmn
   *
   * @param requestId
   * @param deploymentRecord
   * @param resourceDefinition
   */
  private void transformBpmn(
      final long requestId,
      final DeploymentRecord deploymentRecord,
      final ResourceDefinitionRecord resourceDefinition) {
    final BpmnModelInstance bpmnModelInstance =
        Bpmn.readModelFromBytes(resourceDefinition.getResource());
    final String validate = bpmnValidator.validate(bpmnModelInstance);
    if (validate != null) {
      LOG.error(
          "Bpmn validation failed for deployment {} resource {}: {}",
          deploymentRecord.getDeploymentId(),
          resourceDefinition.getResourceDefinitionName(),
          validate);
      writer.adErrorResponse(requestId, -1, "validate error:" + validate);
      throw new EngineRollbackException("validate error:" + validate);
    }
    final DirectBuffer resourceBuffer = resourceDefinition.getResourceBuffer();
    final DirectBuffer checksum = resourceDefinition.getChecksumBuffer();
    final List<BpmnProcess> executableProcesses =
        bpmnTransformer.transformDefinitions(bpmnModelInstance).stream()
            .filter(BpmnProcess::isExecutable)
            .toList();
    for (final BpmnProcess executableProcess : executableProcesses) {
      final ProcessDefinitionRecord processDefinitionRecord =
          deploymentRecord.processDefinitions().add();
      final Integer historyTimeToLive = executableProcess.getHistoryTimeToLive();
      processDefinitionRecord
          .setProcessDefinitionId(writer.nextGlobalKey())
          .setDeploymentId(deploymentRecord.getDeploymentId())
          .setProcessDefinitionName(executableProcess.getName())
          .setProcessDefinitionKey(executableProcess.getId())
          .setProcessDefinitionVersion(
              bpmnResource.getVersion(executableProcess.getId(), deploymentRecord.getTenantId()))
          .setResource(resourceBuffer, 0, resourceBuffer.capacity())
          .setChecksum(checksum, 0, checksum.capacity())
          .setSuspension(true)
          .setStartable(executableProcess.getNoneStartEvent() != null)
          .setHistoryTimeToLive(historyTimeToLive == null ? -1 : historyTimeToLive)
          .setVersionTag("")
          .setResourceDefinitionId(resourceDefinition.getResourceDefinitionId())
          .setResourceDefinitionName(resourceDefinition.getResourceDefinitionNameBuffer())
          .setTenantId(resourceDefinition.getTenantIdBuffer());
      response.addProcessDefinition(processDefinitionRecord);
    }
  }

  /**
   * 转换为 dmn
   *
   * @param requestId
   * @param deploymentRecord
   * @param resourceDefinition
   */
  private void transformDmn(
      final long requestId,
      final DeploymentRecord deploymentRecord,
      final ResourceDefinitionRecord resourceDefinition) {
    final DmnModelInstance dmnModelInstance =
        Dmn.readModelFromBytes(resourceDefinition.getResource());
    final String validate = dmnValidator.validate(dmnModelInstance);
    if (validate != null) {
      LOG.error(
          "Dmn validation failed for deployment {} resource {}: {}",
          deploymentRecord.getDeploymentId(),
          resourceDefinition.getResourceDefinitionName(),
          validate);
      writer.adErrorResponse(requestId, -1, "validate error:" + validate);
      throw new EngineRollbackException("validate error:" + validate);
    }
    final DirectBuffer resourceBuffer = resourceDefinition.getResourceBuffer();
    final DirectBuffer checksum = resourceDefinition.getChecksumBuffer();
    final DmnDecisionRequirementsGraph dmnDecisionRequirementsGraph =
        dmnTransformer.transformDefinitions(dmnModelInstance);
    final DecisionRequirementDefinitionRecord decisionRequirementDefinition =
        deploymentRecord.decisionRequirementDefinitions().add();
    decisionRequirementDefinition
        .setDecisionRequirementDefinitionId(writer.nextGlobalKey())
        .setDecisionRequirementDefinitionKey(dmnDecisionRequirementsGraph.getKey())
        .setDecisionRequirementDefinitionName(dmnDecisionRequirementsGraph.getName())
        .setDecisionRequirementsVersion(
            dmnResource.getDecisionRequirementVersion(
                dmnDecisionRequirementsGraph.getKey(), deploymentRecord.getTenantId()))
        .setTenantId(deploymentRecord.getTenantIdBuffer())
        .setDeploymentId(deploymentRecord.getDeploymentId())
        .setResource(resourceBuffer, 0, resourceBuffer.capacity())
        .setChecksum(checksum, 0, checksum.capacity())
        .setResourceDefinitionName(resourceDefinition.getResourceDefinitionNameBuffer())
        .setResourceDefinitionId(resourceDefinition.getResourceDefinitionId());

    response.addDecisionRequirementDefinition(decisionRequirementDefinition);
    for (final DmnDecision dmnDecision : dmnDecisionRequirementsGraph.getDecisions()) {
      final DecisionDefinitionRecord decisionDefinition =
          deploymentRecord.decisionDefinitions().add();
      decisionDefinition
          .setDecisionDefinitionId(writer.nextGlobalKey())
          .setDecisionDefinitionKey(dmnDecision.getKey())
          .setDecisionDefinitionName(dmnDecision.getName())
          .setDecisionDefinitionVersion(
              dmnResource.getDecisionVersion(dmnDecision.getKey(), deploymentRecord.getTenantId()))
          .setDecisionRequirementDefinitionId(
              decisionRequirementDefinition.getDecisionRequirementDefinitionId())
          .setDecisionRequirementDefinitionKey(
              decisionRequirementDefinition.getDecisionRequirementDefinitionKeyBuffer())
          .setVersionTag("")
          .setHistoryTimeToLive(-1)
          .setResource(resourceBuffer, 0, resourceBuffer.capacity())
          .setChecksum(checksum, 0, checksum.capacity())
          .setDeploymentId(deploymentRecord.getDeploymentId())
          .setResourceDefinitionId(resourceDefinition.getResourceDefinitionId())
          .setResourceDefinitionName(resourceDefinition.getResourceDefinitionNameBuffer())
          .setTenantId(deploymentRecord.getTenantIdBuffer());
      response.addDecisionDefinition(decisionDefinition);
    }
  }
}
