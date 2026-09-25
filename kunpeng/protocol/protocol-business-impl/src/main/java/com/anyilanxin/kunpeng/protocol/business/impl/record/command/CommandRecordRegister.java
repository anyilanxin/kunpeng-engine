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
package com.anyilanxin.kunpeng.protocol.business.impl.record.command;

import com.anyilanxin.kunpeng.protocol.business.impl.record.command.activityinstance.ActivityInstanceRecord;
import com.anyilanxin.kunpeng.protocol.business.impl.record.command.async.AsyncRequestRecord;
import com.anyilanxin.kunpeng.protocol.business.impl.record.command.delay.DelayEventCommandRecord;
import com.anyilanxin.kunpeng.protocol.business.impl.record.command.deployment.*;
import com.anyilanxin.kunpeng.protocol.business.impl.record.command.distribute.parallel.DistributeParallelRecord;
import com.anyilanxin.kunpeng.protocol.business.impl.record.command.distribute.serial.DistributeSerialRecord;
import com.anyilanxin.kunpeng.protocol.business.impl.record.command.historycleanup.HistoryCleanupRecord;
import com.anyilanxin.kunpeng.protocol.business.impl.record.command.incodent.IncidentRecord;
import com.anyilanxin.kunpeng.protocol.business.impl.record.command.job.JobBatchRecord;
import com.anyilanxin.kunpeng.protocol.business.impl.record.command.job.JobRecord;
import com.anyilanxin.kunpeng.protocol.business.impl.record.command.message.MessageDistributeCorrelateRecord;
import com.anyilanxin.kunpeng.protocol.business.impl.record.command.message.MessageSubscriptionRecord;
import com.anyilanxin.kunpeng.protocol.business.impl.record.command.processinstance.ProcessInstanceBatchRecord;
import com.anyilanxin.kunpeng.protocol.business.impl.record.command.processinstance.ProcessInstanceRecord;
import com.anyilanxin.kunpeng.protocol.business.impl.record.command.signal.SignalDistributeCorrelateRecord;
import com.anyilanxin.kunpeng.protocol.business.impl.record.command.signal.SignalSubscriptionRecord;
import com.anyilanxin.kunpeng.protocol.business.impl.record.command.timer.TimerEventRecord;
import com.anyilanxin.kunpeng.protocol.business.impl.record.command.usertask.UserTaskRecord;
import com.anyilanxin.kunpeng.protocol.business.impl.record.command.variable.VariableRecord;
import com.anyilanxin.kunpeng.protocol.business.record.RecordValueMapperRegister;
import com.anyilanxin.kunpeng.protocol.business.record.command.activityinstance.ActivityInstanceLifeCycle;
import com.anyilanxin.kunpeng.protocol.business.record.command.async.AsyncRequestLifeCycle;
import com.anyilanxin.kunpeng.protocol.business.record.command.delay.DelayLifeCycle;
import com.anyilanxin.kunpeng.protocol.business.record.command.deployment.*;
import com.anyilanxin.kunpeng.protocol.business.record.command.distribute.parallel.DistributeParallelLifeCycle;
import com.anyilanxin.kunpeng.protocol.business.record.command.distribute.serial.DistributeSerialLifeCycle;
import com.anyilanxin.kunpeng.protocol.business.record.command.historycleanup.HistoryCleanupLifeCycle;
import com.anyilanxin.kunpeng.protocol.business.record.command.incident.IncidentLifeCycle;
import com.anyilanxin.kunpeng.protocol.business.record.command.job.JobBatchLifeCycle;
import com.anyilanxin.kunpeng.protocol.business.record.command.job.JobLifeCycle;
import com.anyilanxin.kunpeng.protocol.business.record.command.message.MessageDistributeCorrelateLifeCycle;
import com.anyilanxin.kunpeng.protocol.business.record.command.message.MessageSubscriptionLifeCycle;
import com.anyilanxin.kunpeng.protocol.business.record.command.processinstance.ProcessInstanceBatchState;
import com.anyilanxin.kunpeng.protocol.business.record.command.processinstance.ProcessInstanceLifeCycle;
import com.anyilanxin.kunpeng.protocol.business.record.command.signal.SignalDistributeCorrelateLifeCycle;
import com.anyilanxin.kunpeng.protocol.business.record.command.signal.SignalSubscriptionLifeCycle;
import com.anyilanxin.kunpeng.protocol.business.record.command.timer.TimerLifeCycle;
import com.anyilanxin.kunpeng.protocol.business.record.command.usertask.UserTaskLifeCycle;
import com.anyilanxin.kunpeng.protocol.business.record.command.variable.VariableLifeCycle;

/**
 * 业务协议命令 Record 注册器。
 *
 * @author zxuanhong
 * @since 2026.9.0
 */
public class CommandRecordRegister {
  private CommandRecordRegister() {}

  @SuppressWarnings({"rawtypes", "unchecked"})
  public static void register(final RecordValueMapperRegister valueMapper) {
    valueMapper
        .register(DelayLifeCycle.NULL_VAL, DelayEventCommandRecord::new)
        .register(DeploymentLifeCycle.NULL_VAL, DeploymentRecord::new)
        .register(ProcessDefinitionLifeCycle.NULL_VAL, ProcessDefinitionRecord::new)
        .register(DecisionDefinitionLifeCycle.NULL_VAL, DecisionDefinitionRecord::new)
        .register(
            DecisionRequirementDefinitionLifeCycle.NULL_VAL,
            DecisionRequirementDefinitionRecord::new)
        .register(ResourceDefinitionLifeCycle.NULL_VAL, ResourceDefinitionRecord::new)
        .register(DistributeParallelLifeCycle.NULL_VAL, DistributeParallelRecord::new)
        .register(DistributeSerialLifeCycle.NULL_VAL, DistributeSerialRecord::new)
        .register(HistoryCleanupLifeCycle.NULL_VAL, HistoryCleanupRecord::new)
        .register(JobBatchLifeCycle.NULL_VAL, JobBatchRecord::new)
        .register(JobLifeCycle.NULL_VAL, JobRecord::new)
        .register(MessageSubscriptionLifeCycle.NULL_VAL, MessageSubscriptionRecord::new)
        .register(
            MessageDistributeCorrelateLifeCycle.NULL_VAL, MessageDistributeCorrelateRecord::new)
        .register(ActivityInstanceLifeCycle.NULL_VAL, ActivityInstanceRecord::new)
        .register(IncidentLifeCycle.NULL_VAL, IncidentRecord::new)
        .register(ProcessInstanceBatchState.NULL_VAL, ProcessInstanceBatchRecord::new)
        .register(ProcessInstanceLifeCycle.NULL_VAL, ProcessInstanceRecord::new)
        .register(UserTaskLifeCycle.NULL_VAL, UserTaskRecord::new)
        .register(VariableLifeCycle.NULL_VAL, VariableRecord::new)
        .register(SignalSubscriptionLifeCycle.NULL_VAL, SignalSubscriptionRecord::new)
        .register(SignalDistributeCorrelateLifeCycle.NULL_VAL, SignalDistributeCorrelateRecord::new)
        .register(TimerLifeCycle.NULL_VAL, TimerEventRecord::new)
        .register(AsyncRequestLifeCycle.NULL_VAL, AsyncRequestRecord::new);
  }
}
