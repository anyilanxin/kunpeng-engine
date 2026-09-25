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

import com.anyilanxin.kunpeng.repository.business.modules.activityinstance.MutableActivityInstanceRepository;
import com.anyilanxin.kunpeng.repository.business.modules.async.MutableAsyncRepository;
import com.anyilanxin.kunpeng.repository.business.modules.batch.MutableBatchRepository;
import com.anyilanxin.kunpeng.repository.business.modules.delay.MutableDelayRepository;
import com.anyilanxin.kunpeng.repository.business.modules.deployment.bpmnresource.MutableBpmnResourceRepository;
import com.anyilanxin.kunpeng.repository.business.modules.deployment.bytearray.MutableByteArrayResourceRepository;
import com.anyilanxin.kunpeng.repository.business.modules.deployment.deployment.MutableDeploymentRepository;
import com.anyilanxin.kunpeng.repository.business.modules.deployment.dmnresource.MutableDmnResourceRepository;
import com.anyilanxin.kunpeng.repository.business.modules.distribute.parallel.MutableDistributeParallelRepository;
import com.anyilanxin.kunpeng.repository.business.modules.distribute.serial.MutableDistributeSerialRepository;
import com.anyilanxin.kunpeng.repository.business.modules.historycleanup.MutableHistoryCleanupRepository;
import com.anyilanxin.kunpeng.repository.business.modules.incident.MutableIncidentRepository;
import com.anyilanxin.kunpeng.repository.business.modules.job.MutableJobRepository;
import com.anyilanxin.kunpeng.repository.business.modules.key.MutableKeyGeneratorRepository;
import com.anyilanxin.kunpeng.repository.business.modules.message.MutableMessageEventRepository;
import com.anyilanxin.kunpeng.repository.business.modules.position.MutableProcessedPositionRepository;
import com.anyilanxin.kunpeng.repository.business.modules.processinstance.MutableProcessInstanceRepository;
import com.anyilanxin.kunpeng.repository.business.modules.signal.MutableSignalEventRepository;
import com.anyilanxin.kunpeng.repository.business.modules.sink.MutableSinkRepository;
import com.anyilanxin.kunpeng.repository.business.modules.timer.MutableTimerEventRepository;
import com.anyilanxin.kunpeng.repository.business.modules.usertask.MutableUserTaskRepository;
import com.anyilanxin.kunpeng.repository.business.modules.variable.MutableVariableRepository;

/**
 * 业务面可写仓储接口：各业务域写入能力聚合。
 *
 * @author zxuanhong
 * @since 2026.9.0
 */
public interface MutableBusinessRepository {

  MutableActivityInstanceRepository instanceRepository();

  MutableAsyncRepository asyncRepository();

  MutableBatchRepository batchRepository();

  MutableDelayRepository delayRepository();

  MutableBpmnResourceRepository bpmnResourceRepository();

  MutableByteArrayResourceRepository byteArrayResourceRepository();

  MutableDeploymentRepository deploymentRepository();

  MutableDmnResourceRepository dmnResourceRepository();

  MutableDistributeParallelRepository distributeParallelRepository();

  MutableDistributeSerialRepository distributeSerialRepository();

  MutableSinkRepository sinkRepository();

  MutableHistoryCleanupRepository historyCleanupRepository();

  MutableIncidentRepository incidentRepository();

  MutableJobRepository jobRepository();

  MutableKeyGeneratorRepository keyGeneratorRepository();

  MutableMessageEventRepository messageEventRepository();

  MutableProcessedPositionRepository processedPositionRepository();

  MutableProcessInstanceRepository processInstanceRepository();

  MutableSignalEventRepository signalEventRepository();

  MutableTimerEventRepository timerEventRepository();

  MutableUserTaskRepository userTaskRepository();

  MutableVariableRepository variableRepository();
}
