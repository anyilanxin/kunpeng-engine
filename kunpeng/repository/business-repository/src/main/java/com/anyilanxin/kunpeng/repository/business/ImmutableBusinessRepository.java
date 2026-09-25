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

import com.anyilanxin.kunpeng.repository.business.modules.activityinstance.ImmutableActivityInstanceRepository;
import com.anyilanxin.kunpeng.repository.business.modules.async.ImmutableAsyncRepository;
import com.anyilanxin.kunpeng.repository.business.modules.batch.ImmutableBatchRepository;
import com.anyilanxin.kunpeng.repository.business.modules.delay.ImmutableDelayRepository;
import com.anyilanxin.kunpeng.repository.business.modules.deployment.bpmnresource.ImmutableBpmnResourceRepository;
import com.anyilanxin.kunpeng.repository.business.modules.deployment.bytearray.ImmutableByteArrayResourceRepository;
import com.anyilanxin.kunpeng.repository.business.modules.deployment.deployment.ImmutableDeploymentRepository;
import com.anyilanxin.kunpeng.repository.business.modules.deployment.dmnresource.ImmutableDmnResourceRepository;
import com.anyilanxin.kunpeng.repository.business.modules.distribute.parallel.ImmutableDistributeParallelRepository;
import com.anyilanxin.kunpeng.repository.business.modules.distribute.serial.ImmutableDistributeSerialRepository;
import com.anyilanxin.kunpeng.repository.business.modules.historycleanup.ImmutableHistoryCleanupRepository;
import com.anyilanxin.kunpeng.repository.business.modules.incident.ImmutableIncidentRepository;
import com.anyilanxin.kunpeng.repository.business.modules.job.ImmutableJobRepository;
import com.anyilanxin.kunpeng.repository.business.modules.key.ImmutableKeyGeneratorRepository;
import com.anyilanxin.kunpeng.repository.business.modules.message.ImmutableMessageEventRepository;
import com.anyilanxin.kunpeng.repository.business.modules.position.ImmutableProcessedPositionRepository;
import com.anyilanxin.kunpeng.repository.business.modules.processinstance.ImmutableProcessInstanceRepository;
import com.anyilanxin.kunpeng.repository.business.modules.signal.ImmutableSignalEventRepository;
import com.anyilanxin.kunpeng.repository.business.modules.sink.ImmutableSinkRepository;
import com.anyilanxin.kunpeng.repository.business.modules.timer.ImmutableTimerEventRepository;
import com.anyilanxin.kunpeng.repository.business.modules.usertask.ImmutableUserTaskRepository;
import com.anyilanxin.kunpeng.repository.business.modules.variable.ImmutableVariableRepository;

/**
 * 业务面只读仓储接口：各业务域查询能力聚合。
 *
 * @author zxuanhong
 * @since 2026.9.0
 */
public interface ImmutableBusinessRepository {

  ImmutableActivityInstanceRepository instanceRepository();

  ImmutableAsyncRepository asyncRepository();

  ImmutableBatchRepository batchRepository();

  ImmutableDelayRepository delayRepository();

  ImmutableBpmnResourceRepository bpmnResourceRepository();

  ImmutableByteArrayResourceRepository byteArrayResourceRepository();

  ImmutableDeploymentRepository deploymentRepository();

  ImmutableDmnResourceRepository dmnResourceRepository();

  ImmutableDistributeParallelRepository distributeParallelRepository();

  ImmutableDistributeSerialRepository distributeSerialRepository();

  ImmutableSinkRepository sinkRepository();

  ImmutableHistoryCleanupRepository historyCleanupRepository();

  ImmutableIncidentRepository incidentRepository();

  ImmutableJobRepository jobRepository();

  ImmutableKeyGeneratorRepository keyGeneratorRepository();

  ImmutableMessageEventRepository messageEventRepository();

  ImmutableProcessedPositionRepository processedPositionRepository();

  ImmutableProcessInstanceRepository processInstanceRepository();

  ImmutableSignalEventRepository signalEventRepository();

  ImmutableTimerEventRepository timerEventRepository();

  ImmutableUserTaskRepository userTaskRepository();

  ImmutableVariableRepository variableRepository();
}
