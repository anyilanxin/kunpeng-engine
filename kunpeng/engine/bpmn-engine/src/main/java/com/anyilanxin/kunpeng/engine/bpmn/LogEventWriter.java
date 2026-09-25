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
package com.anyilanxin.kunpeng.engine.bpmn;

import static com.anyilanxin.kunpeng.protocol.common.ClusterCommonConstant.BUSINESS_RAFT_ONE_SOURCE;
import static com.anyilanxin.kunpeng.protocol.common.Protocol.DEPLOYMENT_PARTITION;

import com.anyilanxin.kunpeng.bpm.parse.bpmn.BpmnFactory;
import com.anyilanxin.kunpeng.bpm.parse.bpmn.BpmnValidator;
import com.anyilanxin.kunpeng.bpm.parse.bpmn.transformation.BpmnTransformer;
import com.anyilanxin.kunpeng.bpm.parse.dmn.DmnFactory;
import com.anyilanxin.kunpeng.bpm.parse.dmn.DmnValidator;
import com.anyilanxin.kunpeng.bpm.parse.dmn.transformation.DmnTransformer;
import com.anyilanxin.kunpeng.engine.bpmn.command.behavior.Behavior;
import com.anyilanxin.kunpeng.engine.bpmn.command.behavior.impl.BehaviorImpl;
import com.anyilanxin.kunpeng.engine.bpmn.command.distribute.parallel.DistributeParallelChecker;
import com.anyilanxin.kunpeng.engine.bpmn.command.distribute.serial.DistributeSerialChecker;
import com.anyilanxin.kunpeng.engine.bpmn.command.historycleanup.HistoryCleanupDueDateChecker;
import com.anyilanxin.kunpeng.engine.bpmn.command.job.JobTimeoutChecker;
import com.anyilanxin.kunpeng.engine.bpmn.command.timer.TimerDueDateChecker;
import com.anyilanxin.kunpeng.engine.bpmn.scheduling.TimerClock;
import com.anyilanxin.kunpeng.engine.dmn.DmnEngine;
import com.anyilanxin.kunpeng.engine.dmn.DmnEngineFactory;
import com.anyilanxin.kunpeng.engine.script.ScriptEngine;
import com.anyilanxin.kunpeng.protocol.business.ValueLifeCycle;
import com.anyilanxin.kunpeng.protocol.business.impl.RecordMetadata;
import com.anyilanxin.kunpeng.protocol.business.impl.eventlog.BusinessLogRecord;
import com.anyilanxin.kunpeng.protocol.business.impl.record.DefaultRecordValueMapper;
import com.anyilanxin.kunpeng.protocol.business.impl.record.commandapi.empty.EmptyResponseRecord;
import com.anyilanxin.kunpeng.protocol.business.record.RecordType;
import com.anyilanxin.kunpeng.protocol.business.record.RecordValueMapper;
import com.anyilanxin.kunpeng.protocol.business.record.commandapi.CommandApiValueLifeCycle;
import com.anyilanxin.kunpeng.protocol.business.record.commandapi.record.empty.CommandApiEmptyValueLifeCycle;
import com.anyilanxin.kunpeng.protocol.common.PartitionSourceMetadata;
import com.anyilanxin.kunpeng.protocol.common.Protocol;
import com.anyilanxin.kunpeng.protocol.common.UnifiedRecordValue;
import com.anyilanxin.kunpeng.protocol.common.VersionInfo;
import com.anyilanxin.kunpeng.repository.business.BusinessRepository;
import com.anyilanxin.kunpeng.repository.business.BusinessRepositoryFactory;
import com.anyilanxin.kunpeng.repository.business.ImmutableBusinessRepository;
import com.anyilanxin.kunpeng.repository.business.modules.key.MutableKeyGeneratorRepository;
import com.anyilanxin.kunpeng.utils.VersionUtil;
import io.micrometer.core.instrument.MeterRegistry;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import org.springframework.beans.factory.BeanFactory;

/**
 * 日志事件写入器：将事件 Record 追加写入分区日志。
 *
 * @author zxuanhong
 * @since 2026.9.0
 */
public class LogEventWriter {
  // Broker 版本在启动时即固定；解析 semver 字符串开销不小（正则 + 3 次
  // Integer.parseInt + 分配），因此只解析缓存一次，避免每次写事件都重新解析。
  public static final VersionInfo BROKER_VERSION = VersionInfo.parse(VersionUtil.getVersion());

  private final BusinessRepository repository;
  private final ProcessingCollectSupplier collectSupplier;
  private final int sourceId;
  private final int partitionId;
  private final List<Integer> agentSourceIds;
  private final BeanFactory beanFactory;
  private final BpmnTransformer bpmnTransformer;
  private final BpmnValidator bpmnValidator;
  private final DmnTransformer dmnTransformer;
  private final DmnValidator dmnValidator;
  private final DmnEngine dmnEngine;
  private final InterPartitionCommandSender commandSender;
  private final MutableKeyGeneratorRepository keyGenerator;
  private final Behavior behavior;
  private final MeterRegistry meterRegistry;
  private final RecordValueMapper valueMapper = DefaultRecordValueMapper.getInstance();
  private final TimerClock clock;
  private final boolean isLeaderPartition;
  private final List<SchedulerCheckerAware> checkerAwareList = new ArrayList<>();
  private TimerDueDateChecker timerChecker;
  private final BusinessRepositoryFactory repositoryFactory;
  private final JobDeliveryPort deliveryPort;

  public LogEventWriter(
      final BusinessRepositoryFactory repositoryFactory,
      final TimerClock clock,
      final BusinessRepository repository,
      final ProcessingCollectSupplier collectSupplier,
      final PartitionSourceMetadata partitionSourceMetadata,
      final BeanFactory beanFactory,
      final InterPartitionCommandSender commandSender,
      final JobDeliveryPort deliveryPort,
      final MeterRegistry meterRegistry) {
    this.repository = repository;
    this.deliveryPort = deliveryPort;
    this.meterRegistry = meterRegistry;
    this.repositoryFactory = repositoryFactory;
    this.clock = clock;
    this.collectSupplier = collectSupplier;
    sourceId = partitionSourceMetadata.sourceId();
    partitionId = partitionSourceMetadata.partitionId();
    agentSourceIds = new ArrayList<>(partitionSourceMetadata.agentSourceIds());
    this.beanFactory = beanFactory;
    final ScriptEngine expressionLanguage = BpmnFactory.createExpressionLanguage(beanFactory);
    bpmnTransformer = BpmnFactory.createTransformer(expressionLanguage);
    bpmnValidator = BpmnFactory.createValidator(beanFactory, 12 * 1024);

    dmnTransformer = DmnFactory.createTransformer(expressionLanguage);
    dmnValidator = new DmnValidator();
    dmnEngine =
        DmnEngineFactory.getInstance()
            .feelEngine(expressionLanguage)
            .meterRegistry(meterRegistry)
            .build();

    this.commandSender = commandSender;
    initCheckerAware(repositoryFactory);
    keyGenerator = repository.keyGeneratorRepository();

    behavior = new BehaviorImpl(this, timerChecker, commandSender);
    isLeaderPartition = partitionSourceMetadata.partitionId() == DEPLOYMENT_PARTITION;
  }

  private void initCheckerAware(final BusinessRepositoryFactory repositoryFactory) {

    final JobTimeoutChecker jobChecker =
        new JobTimeoutChecker(
            repositoryFactory.create().jobRepository(), Duration.ofSeconds(1), 10, clock);

    timerChecker =
        new TimerDueDateChecker(repositoryFactory.create().timerEventRepository(), clock);

    final HistoryCleanupDueDateChecker historyCleanupDueDateChecker =
        new HistoryCleanupDueDateChecker(
            repositoryFactory.create().historyCleanupRepository(), clock);

    final DistributeParallelChecker distributeParallelProcessor =
        new DistributeParallelChecker(
            repositoryFactory.create().distributeParallelRepository(), this);
    final DistributeSerialChecker distributeSerialProcessor =
        new DistributeSerialChecker(repositoryFactory.create().distributeSerialRepository(), this);

    checkerAwareList.add(jobChecker);
    checkerAwareList.add(timerChecker);
    checkerAwareList.add(distributeParallelProcessor);
    checkerAwareList.add(distributeSerialProcessor);
    checkerAwareList.add(historyCleanupDueDateChecker);
  }

  public void addCommand(
      final ValueLifeCycle lifeCycle, final long requestId, final UnifiedRecordValue recordValue) {
    addCommand(-1, lifeCycle, requestId, recordValue);
  }

  public void addCommand(
      final long key,
      final ValueLifeCycle lifeCycle,
      final long requestId,
      final UnifiedRecordValue recordValue) {
    final RecordMetadata metadata = new RecordMetadata();
    metadata
        .valueLifeCycle(lifeCycle)
        .recordType(RecordType.COMMAND)
        .valueType(lifeCycle.getValueType())
        .recordVersion(1)
        .brokerVersion(BROKER_VERSION)
        .batchOperationReference(-1)
        .operationReference(-1)
        .requestId(requestId);
    collectSupplier.get().addCommand(key, valueMapper.copyValue(lifeCycle, recordValue), metadata);
  }

  public void addCommandWrite(final UnifiedRecordValue recordValue, final RecordMetadata metadata) {
    collectSupplier.get().addCommandWrite(recordValue, metadata);
  }

  public void addCommand(
      final long key,
      final ValueLifeCycle lifeCycle,
      final long requestId,
      final long batchOperationReference,
      final UnifiedRecordValue recordValue) {
    final RecordMetadata metadata = new RecordMetadata();
    metadata
        .valueLifeCycle(lifeCycle)
        .recordType(RecordType.COMMAND)
        .valueType(lifeCycle.getValueType())
        .recordVersion(1)
        .brokerVersion(BROKER_VERSION)
        .batchOperationReference(batchOperationReference)
        .operationReference(-1)
        .requestId(requestId);
    collectSupplier.get().addCommand(key, valueMapper.copyValue(lifeCycle, recordValue), metadata);
  }

  public void addCommand(
      final long key,
      final ValueLifeCycle lifeCycle,
      final long requestId,
      final long operationReference,
      final long batchOperationReference,
      final UnifiedRecordValue recordValue) {
    final RecordMetadata metadata = new RecordMetadata();
    metadata
        .valueLifeCycle(lifeCycle)
        .recordType(RecordType.COMMAND)
        .valueType(lifeCycle.getValueType())
        .recordVersion(1)
        .brokerVersion(BROKER_VERSION)
        .operationReference(operationReference)
        .batchOperationReference(batchOperationReference)
        .requestId(requestId);
    collectSupplier.get().addCommand(key, valueMapper.copyValue(lifeCycle, recordValue), metadata);
  }

  public void addSideEffect(final SideEffectProducer producer) {
    collectSupplier.get().addSideEffect(producer);
  }

  public void addEvent(
      final long key,
      final ValueLifeCycle lifeCycle,
      final long requestId,
      final UnifiedRecordValue recordValue) {
    // addEvent（写日志）与 addState（应用仓储）都只读 metadata 字段，引擎 actor 单线程执行，
    // 共享同一实例是安全的。
    final RecordMetadata metadata = new RecordMetadata();

    metadata
        .valueLifeCycle(lifeCycle)
        .recordType(RecordType.EVENT)
        .valueType(lifeCycle.getValueType())
        .recordVersion(1)
        .brokerVersion(BROKER_VERSION)
        .batchOperationReference(-1)
        .operationReference(-1)
        .requestId(requestId);
    final UnifiedRecordValue value = valueMapper.copyValue(lifeCycle, recordValue);
    collectSupplier.get().addEvent(key, value, metadata);
    collectSupplier.get().addState(key, value, metadata);
  }

  public void adResponse(
      final CommandApiValueLifeCycle lifeCycle,
      final long requestId,
      final UnifiedRecordValue recordValue) {
    collectSupplier.get().adResponse(lifeCycle, requestId, recordValue);
  }

  public void adEmptyResponse(final long requestId, final long key) {
    final EmptyResponseRecord empty = new EmptyResponseRecord();
    empty.setKey(key);
    collectSupplier.get().adResponse(CommandApiEmptyValueLifeCycle.RESPONSE, requestId, empty);
  }

  public void adEmptyResponse(final BusinessLogRecord record) {
    final EmptyResponseRecord empty = new EmptyResponseRecord();
    empty.setKey(record.getKey());
    collectSupplier
        .get()
        .adResponse(CommandApiEmptyValueLifeCycle.RESPONSE, record.getRequestId(), empty);
  }

  public void adErrorResponse(final long requestId, final int code, final String message) {
    collectSupplier.get().adErrorResponse(requestId, code, message);
  }

  public ImmutableBusinessRepository getRepository() {
    return repository;
  }

  public MutableKeyGeneratorRepository getKeyRepository() {
    return repository.keyGeneratorRepository();
  }

  public InterPartitionCommandSender getCommandSender() {
    return commandSender;
  }

  public int getSourceId() {
    return sourceId;
  }

  /** 本分区编号（job 可消费广播等按分区寻址的出口使用） */
  public int getPartitionId() {
    return partitionId;
  }

  public List<Integer> getAgentSourceId() {
    return agentSourceIds;
  }

  public BeanFactory getBeanFactory() {
    return beanFactory;
  }

  public BpmnTransformer getBpmnTransformer() {
    return bpmnTransformer;
  }

  public BpmnValidator getBpmnValidator() {
    return bpmnValidator;
  }

  public DmnTransformer getDmnTransformer() {
    return dmnTransformer;
  }

  public DmnValidator getDmnValidator() {
    return dmnValidator;
  }

  public DmnEngine getDmnEngine() {
    return dmnEngine;
  }

  public Set<Integer> getActivitySourceIds() {
    return commandSender.getActivitySourceIds();
  }

  public long nextGlobalKey() {
    return keyGenerator.nextKey(BUSINESS_RAFT_ONE_SOURCE);
  }

  public long nextKey(final int resourceId) {
    return keyGenerator.nextKey(resourceId);
  }

  public long nextCurrentSourceKey() {
    return keyGenerator.nextKey(getSourceId());
  }

  public boolean isLeaderPartition() {
    return isLeaderPartition;
  }

  public long nextCurrentSourceKey(final long key) {
    final int resourceId = Protocol.decodeResourceId(key);
    if (resourceId == 1) {
      return keyGenerator.nextKey(getSourceId());
    } else {
      return keyGenerator.nextKey(resourceId);
    }
  }

  public Behavior behavior() {
    return behavior;
  }

  /** 引擎侧 job 流推送端口（broker 层装配注入；null = 派发面未装配） */
  public JobDeliveryPort getJobDeliveryPort() {
    return deliveryPort;
  }

  public TimerClock clock() {
    return clock;
  }

  public long millis() {
    return clock.millis();
  }

  List<SchedulerCheckerAware> schedulerCheckerAwares() {
    return checkerAwareList;
  }
}
