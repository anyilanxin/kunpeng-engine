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
package com.anyilanxin.kunpeng.repository.business;

import com.anyilanxin.kunpeng.kvstore.KvStore;
import com.anyilanxin.kunpeng.kvstore.TransactionContext;
import com.anyilanxin.kunpeng.repository.business.modules.activityinstance.ActivityInstanceRepository;
import com.anyilanxin.kunpeng.repository.business.modules.activityinstance.MutableActivityInstanceRepository;
import com.anyilanxin.kunpeng.repository.business.modules.async.AsyncRepository;
import com.anyilanxin.kunpeng.repository.business.modules.async.MutableAsyncRepository;
import com.anyilanxin.kunpeng.repository.business.modules.batch.BatchRepository;
import com.anyilanxin.kunpeng.repository.business.modules.batch.MutableBatchRepository;
import com.anyilanxin.kunpeng.repository.business.modules.delay.DelayRepository;
import com.anyilanxin.kunpeng.repository.business.modules.delay.MutableDelayRepository;
import com.anyilanxin.kunpeng.repository.business.modules.deployment.bpmnresource.BpmnResourceRepository;
import com.anyilanxin.kunpeng.repository.business.modules.deployment.bpmnresource.MutableBpmnResourceRepository;
import com.anyilanxin.kunpeng.repository.business.modules.deployment.bytearray.ByteArrayResourceRepository;
import com.anyilanxin.kunpeng.repository.business.modules.deployment.bytearray.MutableByteArrayResourceRepository;
import com.anyilanxin.kunpeng.repository.business.modules.deployment.deployment.MutableDeploymentRepository;
import com.anyilanxin.kunpeng.repository.business.modules.deployment.deployment.RepositoryDeploymentRepository;
import com.anyilanxin.kunpeng.repository.business.modules.deployment.dmnresource.DmnResourceRepository;
import com.anyilanxin.kunpeng.repository.business.modules.deployment.dmnresource.MutableDmnResourceRepository;
import com.anyilanxin.kunpeng.repository.business.modules.distribute.parallel.DistributeParallelRepository;
import com.anyilanxin.kunpeng.repository.business.modules.distribute.parallel.MutableDistributeParallelRepository;
import com.anyilanxin.kunpeng.repository.business.modules.distribute.serial.DistributeSerialRepository;
import com.anyilanxin.kunpeng.repository.business.modules.distribute.serial.MutableDistributeSerialRepository;
import com.anyilanxin.kunpeng.repository.business.modules.historycleanup.HistoryCleanupRepository;
import com.anyilanxin.kunpeng.repository.business.modules.historycleanup.MutableHistoryCleanupRepository;
import com.anyilanxin.kunpeng.repository.business.modules.incident.IncidentRepository;
import com.anyilanxin.kunpeng.repository.business.modules.incident.MutableIncidentRepository;
import com.anyilanxin.kunpeng.repository.business.modules.job.JobRepository;
import com.anyilanxin.kunpeng.repository.business.modules.job.MutableJobRepository;
import com.anyilanxin.kunpeng.repository.business.modules.key.KeyGeneratorRepository;
import com.anyilanxin.kunpeng.repository.business.modules.key.MutableKeyGeneratorRepository;
import com.anyilanxin.kunpeng.repository.business.modules.message.MessageEventRepository;
import com.anyilanxin.kunpeng.repository.business.modules.message.MutableMessageEventRepository;
import com.anyilanxin.kunpeng.repository.business.modules.position.MutableProcessedPositionRepository;
import com.anyilanxin.kunpeng.repository.business.modules.position.ProcessedPositionRepository;
import com.anyilanxin.kunpeng.repository.business.modules.processinstance.MutableProcessInstanceRepository;
import com.anyilanxin.kunpeng.repository.business.modules.processinstance.ProcessInstanceRepository;
import com.anyilanxin.kunpeng.repository.business.modules.signal.MutableSignalEventRepository;
import com.anyilanxin.kunpeng.repository.business.modules.signal.SignalEventRepository;
import com.anyilanxin.kunpeng.repository.business.modules.sink.MutableSinkRepository;
import com.anyilanxin.kunpeng.repository.business.modules.sink.SinkRepository;
import com.anyilanxin.kunpeng.repository.business.modules.timer.MutableTimerEventRepository;
import com.anyilanxin.kunpeng.repository.business.modules.timer.TimerEventRepository;
import com.anyilanxin.kunpeng.repository.business.modules.usertask.MutableUserTaskRepository;
import com.anyilanxin.kunpeng.repository.business.modules.usertask.UserTaskRepository;
import com.anyilanxin.kunpeng.repository.business.modules.variable.MutableVariableRepository;
import com.anyilanxin.kunpeng.repository.business.modules.variable.VariableRepository;
import io.micrometer.core.instrument.MeterRegistry;
import java.util.List;
import java.util.Set;
import org.springframework.beans.factory.BeanFactory;

/**
 * RocksDB 业务面仓储实现。
 *
 * @author zxuanhong
 * @since 2026.9.0
 */
final class RocksdbBusinessRepository implements BusinessRepository {
  private final TransactionContext transaction;
  private final BusinessRepositoryAppliers appliers;
  private final DataSplitRegister splitRegister;
  private final ActivityInstanceRepository instanceRepository;
  private final AsyncRepository asyncRepository;
  private final BatchRepository batchRepository;
  private final DelayRepository delayRepository;
  private final BpmnResourceRepository bpmnResourceRepository;
  private final ByteArrayResourceRepository byteArrayResourceRepository;
  private final RepositoryDeploymentRepository deploymentRepository;
  private final DmnResourceRepository dmnResourceRepository;
  private final DistributeParallelRepository distributeParallelRepository;
  private final DistributeSerialRepository distributeSerialRepository;
  private final SinkRepository sinkRepository;
  private final HistoryCleanupRepository historyCleanupRepository;
  private final IncidentRepository incidentRepository;
  private final JobRepository jobRepository;
  private final KeyGeneratorRepository keyGeneratorRepository;
  private final MessageEventRepository messageEventRepository;
  private final ProcessedPositionRepository processedPositionRepository;
  private final ProcessInstanceRepository processInstanceRepository;
  private final SignalEventRepository signalEventRepository;
  private final TimerEventRepository timerEventRepository;
  private final UserTaskRepository userTaskRepository;
  private final VariableRepository variableRepository;

  public RocksdbBusinessRepository(
      final int positionId,
      final Set<Integer> resourceIds,
      final KvStore<BusinessRepositoryColumnFamilies> db,
      final BeanFactory beanFactory,
      final MeterRegistry meterRegistry) {
    this(positionId, resourceIds, db, db.createTransactionContext(), beanFactory, meterRegistry);
  }

  public RocksdbBusinessRepository(
      final int positionId,
      final Set<Integer> resourceIds,
      final KvStore<BusinessRepositoryColumnFamilies> db,
      final TransactionContext transaction,
      final BeanFactory beanFactory,
      final MeterRegistry meterRegistry) {
    this.transaction = transaction;
    splitRegister = new DataSplitRegister();
    appliers = new RocksdbBusinessRepositoryApplier(this);
    instanceRepository = new ActivityInstanceRepository(db, transaction, splitRegister);
    asyncRepository = new AsyncRepository(db, transaction, splitRegister);
    batchRepository = new BatchRepository(db, transaction, splitRegister);
    delayRepository = new DelayRepository(db, transaction, splitRegister);
    bpmnResourceRepository =
        new BpmnResourceRepository(db, transaction, splitRegister, beanFactory, meterRegistry);
    byteArrayResourceRepository = new ByteArrayResourceRepository(db, transaction, splitRegister);
    deploymentRepository =
        new RepositoryDeploymentRepository(
            db, transaction, splitRegister, beanFactory, meterRegistry);
    dmnResourceRepository = new DmnResourceRepository(db, transaction, splitRegister);
    distributeParallelRepository = new DistributeParallelRepository(db, transaction, splitRegister);
    distributeSerialRepository = new DistributeSerialRepository(db, transaction, splitRegister);
    sinkRepository = new SinkRepository(db, transaction, splitRegister);
    historyCleanupRepository = new HistoryCleanupRepository(db, transaction, splitRegister);
    incidentRepository = new IncidentRepository(db, transaction, splitRegister);
    jobRepository = new JobRepository(db, transaction, splitRegister);
    keyGeneratorRepository = new KeyGeneratorRepository(db, transaction, splitRegister);
    messageEventRepository = new MessageEventRepository(db, transaction, splitRegister);
    processedPositionRepository = new ProcessedPositionRepository(db, transaction, splitRegister);
    processInstanceRepository = new ProcessInstanceRepository(db, transaction, splitRegister);
    signalEventRepository = new SignalEventRepository(db, transaction, splitRegister);
    timerEventRepository = new TimerEventRepository(db, transaction, splitRegister);
    userTaskRepository = new UserTaskRepository(db, transaction, splitRegister);
    variableRepository = new VariableRepository(db, transaction, splitRegister);
  }

  @Override
  public TransactionContext getContext() {
    return transaction;
  }

  @Override
  public BusinessRepositoryAppliers getAppliers() {
    return appliers;
  }

  @Override
  public List<ResourceDataSplit> getResourceDataSplits() {
    return splitRegister.getDataSplits();
  }

  @Override
  public MutableActivityInstanceRepository instanceRepository() {
    return instanceRepository;
  }

  @Override
  public MutableAsyncRepository asyncRepository() {
    return asyncRepository;
  }

  @Override
  public MutableBatchRepository batchRepository() {
    return batchRepository;
  }

  @Override
  public MutableDelayRepository delayRepository() {
    return delayRepository;
  }

  @Override
  public MutableBpmnResourceRepository bpmnResourceRepository() {
    return bpmnResourceRepository;
  }

  @Override
  public MutableByteArrayResourceRepository byteArrayResourceRepository() {
    return byteArrayResourceRepository;
  }

  @Override
  public MutableDeploymentRepository deploymentRepository() {
    return deploymentRepository;
  }

  @Override
  public MutableDmnResourceRepository dmnResourceRepository() {
    return dmnResourceRepository;
  }

  @Override
  public MutableDistributeParallelRepository distributeParallelRepository() {
    return distributeParallelRepository;
  }

  @Override
  public MutableDistributeSerialRepository distributeSerialRepository() {
    return distributeSerialRepository;
  }

  @Override
  public MutableSinkRepository sinkRepository() {
    return sinkRepository;
  }

  @Override
  public MutableHistoryCleanupRepository historyCleanupRepository() {
    return historyCleanupRepository;
  }

  @Override
  public MutableIncidentRepository incidentRepository() {
    return incidentRepository;
  }

  @Override
  public MutableJobRepository jobRepository() {
    return jobRepository;
  }

  @Override
  public MutableKeyGeneratorRepository keyGeneratorRepository() {
    return keyGeneratorRepository;
  }

  @Override
  public MutableMessageEventRepository messageEventRepository() {
    return messageEventRepository;
  }

  @Override
  public MutableProcessedPositionRepository processedPositionRepository() {
    return processedPositionRepository;
  }

  @Override
  public MutableProcessInstanceRepository processInstanceRepository() {
    return processInstanceRepository;
  }

  @Override
  public MutableSignalEventRepository signalEventRepository() {
    return signalEventRepository;
  }

  @Override
  public MutableTimerEventRepository timerEventRepository() {
    return timerEventRepository;
  }

  @Override
  public MutableUserTaskRepository userTaskRepository() {
    return userTaskRepository;
  }

  @Override
  public MutableVariableRepository variableRepository() {
    return variableRepository;
  }
}
