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
package com.anyilanxin.kunpeng.cluster.dispatch.command.business.dispatch.processor;

import static com.anyilanxin.kunpeng.protocol.admin.impl.record.command.common.ExecutionRecordSerialize.decode;
import static com.anyilanxin.kunpeng.protocol.admin.impl.record.command.common.ExecutionRecordSerialize.encode;
import static com.anyilanxin.kunpeng.structpack.util.BufferUtil.copyInto;

import com.anyilanxin.kunpeng.cluster.cluster.MemberId;
import com.anyilanxin.kunpeng.cluster.cluster.PartitionId;
import com.anyilanxin.kunpeng.cluster.config.topology.cluster.ClusterTopologyService;
import com.anyilanxin.kunpeng.cluster.dispatch.ClusterDispatchLoggers;
import com.anyilanxin.kunpeng.cluster.dispatch.LogEventWriter;
import com.anyilanxin.kunpeng.cluster.dispatch.command.business.dispatch.AbstractBusinessDispatchProcessor;
import com.anyilanxin.kunpeng.cluster.dispatch.eventlog.LogRecord;
import com.anyilanxin.kunpeng.protocol.admin.impl.record.command.business.BusinessDispatchPlanExecutionRecord;
import com.anyilanxin.kunpeng.protocol.admin.impl.record.command.business.BusinessDispatchPlanRecord;
import com.anyilanxin.kunpeng.protocol.admin.impl.record.command.common.*;
import com.anyilanxin.kunpeng.protocol.admin.impl.record.command.source.PartitionSourceMetaRecord;
import com.anyilanxin.kunpeng.protocol.admin.impl.record.command.source.PartitionSourceRecord;
import com.anyilanxin.kunpeng.protocol.admin.record.command.PartitionExecutionRecordValue;
import com.anyilanxin.kunpeng.protocol.admin.record.command.PartitionExecutionType;
import com.anyilanxin.kunpeng.protocol.admin.record.command.PartitionType;
import com.anyilanxin.kunpeng.protocol.admin.record.command.business.BusinessDispatchPlanExecutionLifeCycle;
import com.anyilanxin.kunpeng.protocol.admin.record.command.business.BusinessDispatchPlanLifeCycle;
import com.anyilanxin.kunpeng.protocol.admin.record.command.source.PartitionSourceLifeCycle;
import com.anyilanxin.kunpeng.protocol.admin.record.command.source.PartitionSourceMetaLifeCycle;
import com.anyilanxin.kunpeng.protocol.common.UnifiedRecordValue;
import com.anyilanxin.kunpeng.repository.admin.AdminImmutableRepository;
import com.anyilanxin.kunpeng.repository.admin.modules.business.ImmutableRepositoryBusiness;
import com.anyilanxin.kunpeng.repository.admin.modules.key.ImmutableRepositoryKey;
import com.anyilanxin.kunpeng.repository.admin.modules.source.ImmutableRepositorySource;
import com.anyilanxin.kunpeng.structpack.value.IntegerValue;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import org.slf4j.Logger;

/**
 * @author zxuanhong
 * @since
 */
public class BusinessDispatchClusterExecutingProcessor extends AbstractBusinessDispatchProcessor {
  protected final LogEventWriter writer;
  private final ImmutableRepositoryBusiness repositoryBusiness;
  private final ImmutableRepositorySource repositorySource;
  private final ImmutableRepositoryKey repositoryKey;
  private final ClusterTopologyService clusterTopologyService;
  public static final Logger LOGGER = ClusterDispatchLoggers.CLUSTER_DISPATCH;

  public BusinessDispatchClusterExecutingProcessor(final LogEventWriter writer) {
    super(writer);
    this.writer = writer;
    final AdminImmutableRepository repository = writer.getRepository();
    repositoryBusiness = repository.repositoryBusiness();
    repositorySource = repository.repositorySource();
    repositoryKey = repository.repositoryKey();
    clusterTopologyService = writer.getClusterTopologyService();
  }

  @Override
  public void processRecord(final LogRecord<BusinessDispatchPlanRecord> record) {
    final BusinessDispatchPlanRecord planRecord = record.getValue();
    final BusinessDispatchPlanRecord dispatchPlan =
        repositoryBusiness.getDispatchPlan(planRecord.getDispatchPlanId());
    if (dispatchPlan == null) {
      throw new RuntimeException("计划不存在");
    }
    if (dispatchPlan.executionPlan().isEmpty()) {
      LOGGER.info("\n\n------->调度计划执行内容为空，直接完成<-------\n");
      writer.addEvent(
          dispatchPlan.getDispatchPlanId(),
          BusinessDispatchPlanLifeCycle.COMPLETED,
          record.getRequestId(),
          dispatchPlan);
    } else {
      handleBootstrapExecutionSource(dispatchPlan, record.getRequestId());
      LOGGER.info("\n\n------->调度计划生成成功<-------\n");
      final StringBuffer stringBuffer = new StringBuffer();
      for (final BusinessDispatchPlanExecutionRecord executionRecord :
          dispatchPlan.executionPlan()) {
        stringBuffer
            .append("\n")
            .append("执行 id:")
            .append(executionRecord.getDispatchPlanExecutionId())
            .append("\n")
            .append("调度 order:")
            .append(executionRecord.getExecutionOrder())
            .append("\n")
            .append("调度 type:")
            .append(executionRecord.getExecutionType())
            .append("\n");
      }
      stringBuffer.append("\n");
      LOGGER.info("\n------->完整执行信息<-------\n{}", stringBuffer);
      writer.addEvent(
          dispatchPlan.getDispatchPlanId(),
          BusinessDispatchPlanLifeCycle.EXECUTED,
          record.getRequestId(),
          dispatchPlan);
      final BusinessDispatchPlanExecutionRecord planExecutionRecord =
          dispatchPlan.executionPlan().get(0);
      writer.addCommand(
          planExecutionRecord.getDispatchPlanExecutionId(),
          BusinessDispatchPlanExecutionLifeCycle.EXECUTING,
          record.getRequestId(),
          planExecutionRecord);
    }
  }

  /**
   * 引导分区来源处理：遍历原始执行计划重建新数组，遇到引导明细即查询可迁移的代理资源（事件同步应用， 上一条引导发出的转移移除已生效，本次查询不会再包含已迁移资源），迁移资源升格为分区自身
   * sourceId， 来源指向捐赠分区，无资源可迁移时自动申请新 source； 接受迁移的引导明细后追加一条 BOOTSTRAP_SOURCE_DATA_TRANSFER （在引导分区执行、
   * 从捐赠分区拉取迁移资源数据） 与一条 BOOTSTRAP_SOURCE_TRANSFER（捐赠分区 leader，标记资源已迁出； busimeta 经 raft 自动同步其余副本）。
   * 若一条转移明细都未插入，原明细上的变更 （sourceId 回填、planData 重编码）已直接生效，无需替换数组。
   */
  private void handleBootstrapExecutionSource(
      final BusinessDispatchPlanRecord dispatchPlan, final long requestId) {
    final List<BusinessDispatchPlanExecutionRecord> rebuilt = new ArrayList<>();
    for (final BusinessDispatchPlanExecutionRecord executionRecord : dispatchPlan.executionPlan()) {
      if (executionRecord.getExecutionType() != PartitionExecutionType.BOOTSTRAP) {
        final BusinessDispatchPlanExecutionRecord planExecutionRecord =
            new BusinessDispatchPlanExecutionRecord();
        copyInto(executionRecord, planExecutionRecord);
        rebuilt.add(planExecutionRecord);
        if (executionRecord.getExecutionType() == PartitionExecutionType.LEAVE_SOURCE_TRANSFER) {
          final byte[] planData = executionRecord.getPlanData();
          final PartitionLeaveSourceTransferRecord leaveSourceTransferRecord =
              decode(planData, PartitionExecutionType.LEAVE_SOURCE_TRANSFER);
          final PartitionSourceRecord value = new PartitionSourceRecord();
          value.setSourcePartitionId(leaveSourceTransferRecord.getSourcePartitionId());
          value.setSourcePartitionGroup(leaveSourceTransferRecord.getSourcePartitionGroup());
          value.setPartitionGroup(leaveSourceTransferRecord.getPartitionGroup());
          value.setPartitionId(leaveSourceTransferRecord.getPartitionId());
          for (final IntegerValue agentSourceId : leaveSourceTransferRecord.agentSourceIds()) {
            value.agentSourceIds().add().setValue(agentSourceId.getValue());
          }
          writer.addEvent(PartitionSourceLifeCycle.TRANSFERRED_ADD, requestId, value);
        }
      } else {
        final PartitionBootstrapRecord bootstrapRecord =
            decode(executionRecord.getPlanData(), PartitionExecutionType.BOOTSTRAP);
        final PartitionInfoMetaRecord targetMeta = bootstrapRecord.getTargetMeta();
        if (!bootstrapRecord.isBootstrapSnapshot()) {
          final PartitionSourceRecord partitionSource = applyNewSource(targetMeta, requestId);
          final BusinessDispatchPlanExecutionRecord planExecutionRecord =
              fillBootstrapSource(executionRecord, bootstrapRecord, partitionSource);
          rebuilt.add(planExecutionRecord);
        } else {
          final PartitionSourceRecord transferPartitionSource = nextTransfer(targetMeta, requestId);
          if (transferPartitionSource != null) {
            final BusinessDispatchPlanExecutionRecord planExecutionRecord =
                fillBootstrapSource(executionRecord, bootstrapRecord, transferPartitionSource);
            rebuilt.add(planExecutionRecord);
            appendTransferExecutions(
                executionRecord.getDispatchPlanId(),
                bootstrapRecord,
                transferPartitionSource,
                rebuilt);
          } else {
            final PartitionSourceRecord partitionSource = applyNewSource(targetMeta, requestId);
            final BusinessDispatchPlanExecutionRecord planExecutionRecord =
                fillBootstrapSource(executionRecord, bootstrapRecord, partitionSource);
            rebuilt.add(planExecutionRecord);
          }
        }
      }
    }
    if (rebuilt.size() == dispatchPlan.executionPlan().size()) {
      return;
    }
    dispatchPlan.executionPlan().reset();
    dispatchPlan.setExecutionPlan(rebuilt);
    renumberExecutionOrder(dispatchPlan);
  }

  private BusinessDispatchPlanExecutionRecord fillBootstrapSource(
      final BusinessDispatchPlanExecutionRecord executionRecord,
      final PartitionBootstrapRecord bootstrapRecord,
      final PartitionSourceRecord partitionSource) {
    bootstrapRecord.setSourceId(partitionSource.getSourceId());
    executionRecord.setPlanData(encode(bootstrapRecord));
    final BusinessDispatchPlanExecutionRecord planExecutionRecord =
        new BusinessDispatchPlanExecutionRecord();
    copyInto(executionRecord, planExecutionRecord);
    return planExecutionRecord;
  }

  /**
   * 选取可迁移代理资源数量最多的分区，取其首个代理资源升格为引导分区自身 sourceId：发 APPLIED 事件落库新分区来源， 发 TRANSFERRED_REMOVE
   * 事件从捐赠分区来源记录中移除该资源，返回指向捐赠分区的来源记录；无可用资源返回 null
   */
  private PartitionSourceRecord nextTransfer(
      final PartitionInfoMetaRecord targetMeta, final long requestId) {
    final PartitionSourceRecord partitionSourceRecord =
        repositorySource.getPartitionAgentSources().stream()
            .max(Comparator.comparingInt(source -> source.getAgentSourceIds().size()))
            .orElse(null);
    if (partitionSourceRecord == null) {
      return null;
    }
    final IntegerValue agentSourceIdValue = partitionSourceRecord.agentSourceIds().get(0);
    final int agentSourceId = agentSourceIdValue.getValue();
    final PartitionSourceRecord partitionSource =
        new PartitionSourceRecord()
            .setPartitionGroup(targetMeta.getPartitionGroup())
            .setPartitionId(targetMeta.getPartitionId())
            .setSourceId(agentSourceId)
            .setSourcePartitionGroup(partitionSourceRecord.getPartitionGroup())
            .setSourcePartitionId(partitionSourceRecord.getPartitionId());
    // 标记捐赠分区该代理资源已被迁出，应用侧从捐赠分区来源记录中移除
    final PartitionSourceRecord donatorRemove =
        new PartitionSourceRecord()
            .setPartitionGroup(partitionSourceRecord.getPartitionGroup())
            .setPartitionId(partitionSourceRecord.getPartitionId())
            .setSourceId(agentSourceId);
    writer.addEvent(PartitionSourceLifeCycle.APPLIED, requestId, partitionSource);
    writer.addEvent(PartitionSourceLifeCycle.TRANSFERRED_REMOVE, requestId, donatorRemove);
    return partitionSource;
  }

  /** 无迁移资源可复用时自动申请新来源：来源标识从全局元数据递增分配（与执行侧 getSource 同一逻辑） */
  private PartitionSourceRecord applyNewSource(
      final PartitionInfoMetaRecord partitionMeta, final long requestId) {
    final PartitionSourceMetaRecord sourceMeta = repositorySource.getPartitionSourceMeta();
    sourceMeta.setMaxPartitionSourceId(sourceMeta.getMaxPartitionSourceId() + 1);
    final PartitionSourceRecord partitionSource =
        new PartitionSourceRecord()
            .setPartitionGroup(partitionMeta.getPartitionGroup())
            .setPartitionId(partitionMeta.getPartitionId())
            .setSourceId(sourceMeta.getMaxPartitionSourceId());
    writer.addEvent(PartitionSourceMetaLifeCycle.UPDATED, requestId, sourceMeta);
    writer.addEvent(PartitionSourceLifeCycle.APPLIED, requestId, partitionSource);
    return partitionSource;
  }

  /**
   * 引导明细后追加来源数据转移与来源转移执行明细： 数据转移在引导分区成员执行（从捐赠分区拉取迁移资源数据）； 来源转移仅发捐赠分区 leader 一条（资源标识已融合进 raft
   * 业务元数据，leader 变更经 raft 自动同步其余副本，无需逐成员标记）。
   */
  private void appendTransferExecutions(
      final long dispatchPlanId,
      final PartitionBootstrapRecord bootstrapRecord,
      final PartitionSourceRecord transferPartitionSource,
      final List<BusinessDispatchPlanExecutionRecord> rebuilt) {
    // 引导节点从捐赠分区，也就是 sourcePartition 拉取指定资源的数据，需要在新引导分区执行拉取资源
    final PartitionInfoMetaRecord partitionMeta = bootstrapRecord.getPartitionMeta();
    final PartitionBootstrapSourceDataTransferRecord dataTransfer =
        new PartitionBootstrapSourceDataTransferRecord()
            .setPartitionType(PartitionType.BUSINESS)
            .setExecutionType(PartitionExecutionType.BOOTSTRAP_SOURCE_DATA_TRANSFER)
            .setExecutionMemberId(bootstrapRecord.executionMemberId())
            .setPartitionGroup(partitionMeta.getPartitionGroup())
            .setPartitionId(partitionMeta.getPartitionId())
            .setSourcePartitionGroup(transferPartitionSource.getSourcePartitionGroup())
            .setSourcePartitionId(transferPartitionSource.getSourcePartitionId())
            .setDispatchPlanId(dispatchPlanId)
            .setDispatchPlanExecutionId(repositoryKey.nextKey())
            .setSourceId(transferPartitionSource.getSourceId());
    appendExecution(rebuilt, dataTransfer);
    // 标记捐赠分区该代理资源已被迁出：仅需捐赠分区 leader 执行，busimeta 变更经 raft 同步副本
    final PartitionId sourcePartitionId =
        PartitionId.from(
            transferPartitionSource.getSourcePartitionGroup(),
            transferPartitionSource.getSourcePartitionId());
    final MemberId sourceLeader = clusterTopologyService.getPartitionLeader(sourcePartitionId);
    if (sourceLeader == null) {
      throw new IllegalArgumentException(
          "捐赠分区 %s-%d 暂无 Leader，无法制定来源转移明细"
              .formatted(sourcePartitionId.group(), sourcePartitionId.id()));
    }
    final PartitionBootstrapSourceTransferRecord sourceTransfer =
        new PartitionBootstrapSourceTransferRecord()
            .setPartitionType(PartitionType.BUSINESS)
            .setExecutionType(PartitionExecutionType.BOOTSTRAP_SOURCE_TRANSFER)
            .setExecutionMemberId(sourceLeader.id())
            .setPartitionGroup(sourcePartitionId.group())
            .setPartitionId(sourcePartitionId.id())
            .setDispatchPlanId(dispatchPlanId)
            .setDispatchPlanExecutionId(repositoryKey.nextKey())
            .setSourceId(transferPartitionSource.getSourceId());
    appendExecution(rebuilt, sourceTransfer);
  }

  /** 向重建数组追加一条执行明细并填充公共字段（id、负载、目标成员），字段与计划制定侧 addExecution 保持一致 */
  private void appendExecution(
      final List<BusinessDispatchPlanExecutionRecord> rebuilt, final UnifiedRecordValue payload) {
    final PartitionExecutionRecordValue executionPayload = (PartitionExecutionRecordValue) payload;
    final BusinessDispatchPlanExecutionRecord executionRecord =
        new BusinessDispatchPlanExecutionRecord();
    rebuilt.add(executionRecord);
    executionRecord
        .setDispatchPlanId(executionPayload.getDispatchPlanId())
        .setDispatchPlanExecutionId(executionPayload.getDispatchPlanExecutionId())
        .setPlanData(encode(payload))
        .setDispatchMemberId(executionPayload.executionMemberId())
        .setPartitionType(PartitionType.BUSINESS)
        .setExecutionType(executionPayload.getExecutionType())
        .setExecutionMemberId(executionPayload.executionMemberId());
  }

  /** 插入转移明细后按列表顺序重编全部明细的执行顺序 */
  private void renumberExecutionOrder(final BusinessDispatchPlanRecord dispatchPlan) {
    int order = 0;
    for (final BusinessDispatchPlanExecutionRecord executionRecord : dispatchPlan.executionPlan()) {
      executionRecord.setExecutionOrder(order++);
    }
  }

  @Override
  public BusinessDispatchPlanLifeCycle valueLifeCycle() {
    return BusinessDispatchPlanLifeCycle.EXECUTING;
  }
}
