/*
 * Copyright © 2026 anyilanxin zxh (anyilanxin@aliyun.com)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * Software distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.anyilanxin.kunpeng.protocol.business;

import static com.anyilanxin.kunpeng.protocol.business.RecordProcessIndex.NOT_PROCESS_INDEX;

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
import com.anyilanxin.kunpeng.protocol.business.record.commandapi.record.deployment.CommandApiDeploymentValueLifeCycle;
import com.anyilanxin.kunpeng.protocol.business.record.commandapi.record.empty.CommandApiEmptyValueLifeCycle;
import com.anyilanxin.kunpeng.protocol.business.record.commandapi.record.incident.CommandApiIncidentValueLifeCycle;
import com.anyilanxin.kunpeng.protocol.business.record.commandapi.record.job.CommandApiJobBatchValueLifeCycle;
import com.anyilanxin.kunpeng.protocol.business.record.commandapi.record.job.CommandApiJobValueLifeCycle;
import com.anyilanxin.kunpeng.protocol.business.record.commandapi.record.message.CommandApiMessageValueLifeCycle;
import com.anyilanxin.kunpeng.protocol.business.record.commandapi.record.processdefinition.CommandApiProcessDefinitionValueLifeCycle;
import com.anyilanxin.kunpeng.protocol.business.record.commandapi.record.processinstance.CommandApiProcessInstanceValueLifeCycle;
import com.anyilanxin.kunpeng.protocol.business.record.commandapi.record.signal.CommandApiSignalValueLifeCycle;
import com.anyilanxin.kunpeng.protocol.business.record.commandapi.record.usertask.CommandApiUserTaskValueLifeCycle;
import com.anyilanxin.kunpeng.protocol.business.record.commandapi.record.variable.CommandApiVariableValueLifeCycle;
import com.anyilanxin.kunpeng.protocol.business.record.query.businessroute.BusinessRouteApiLifeCycle;
import java.util.Arrays;
import java.util.Collection;

/**
 * @author zxuanhong
 * @since 2026.9.0
 */
public interface ValueLifeCycle {
  Collection<Class<? extends ValueLifeCycle>> INTENT_CLASSES =
      Arrays.asList(
          AsyncRequestLifeCycle.class,
          ResourceDefinitionLifeCycle.class,
          DecisionDefinitionLifeCycle.class,
          DecisionRequirementDefinitionLifeCycle.class,
          CommandApiDeploymentValueLifeCycle.class,
          DeploymentLifeCycle.class,
          UnknownState.class,
          DistributeParallelLifeCycle.class,
          DistributeSerialLifeCycle.class,
          ActivityInstanceLifeCycle.class,
          FormDefinitionState.class,
          IncidentLifeCycle.class,
          CommandApiIncidentValueLifeCycle.class,
          CommandApiProcessDefinitionValueLifeCycle.class,
          ProcessDefinitionLifeCycle.class,
          ProcessInstanceBatchState.class,
          CommandApiProcessInstanceValueLifeCycle.class,
          ProcessInstanceLifeCycle.class,
          CommandApiVariableValueLifeCycle.class,
          VariableLifeCycle.class,
          CommandApiUserTaskValueLifeCycle.class,
          UserTaskLifeCycle.class,
          JobLifeCycle.class,
          CommandApiJobValueLifeCycle.class,
          CommandApiJobBatchValueLifeCycle.class,
          JobBatchLifeCycle.class,
          TimerLifeCycle.class,
          CommandApiMessageValueLifeCycle.class,
          MessageSubscriptionLifeCycle.class,
          MessageDistributeCorrelateLifeCycle.class,
          SignalSubscriptionLifeCycle.class,
          SignalDistributeCorrelateLifeCycle.class,
          DelayLifeCycle.class,
          HistoryCleanupLifeCycle.class,
          BusinessRouteApiLifeCycle.class,
          CommandApiEmptyValueLifeCycle.class);
  short NULL_VAL = 255;
  ValueLifeCycle UNKNOWN = UnknownState.UNKNOWN;

  short value();

  String name();

  boolean isEvent();

  short processIndex();

  ValueType getValueType();

  default boolean isProcess() {
    return processIndex() != NOT_PROCESS_INDEX;
  }

  short recordIndex();

  static ValueLifeCycle fromProtocolValue(final ValueType valueType, final short state) {
    return switch (valueType) {
      case DEPLOYMENT_API -> CommandApiDeploymentValueLifeCycle.from(state);
      case DEPLOYMENT -> DeploymentLifeCycle.from(state);
      case PROCESS_DEFINITION_API -> CommandApiProcessDefinitionValueLifeCycle.from(state);
      case PROCESS_DEFINITION -> ProcessDefinitionLifeCycle.from(state);
      case DECISION_DEFINITION -> DecisionDefinitionLifeCycle.from(state);
      case DECISION_REQUIREMENTS -> DecisionRequirementDefinitionLifeCycle.from(state);
      case FORM_DEFINITION -> FormDefinitionState.from(state);
      case RESOURCE_DEFINITION -> ResourceDefinitionLifeCycle.from(state);
      case PROCESS_INSTANCE_API -> CommandApiProcessInstanceValueLifeCycle.from(state);
      case PROCESS_INSTANCE -> ProcessInstanceLifeCycle.from(state);
      case ASYNC_REQUEST -> AsyncRequestLifeCycle.from(state);
      case PROCESS_BATCH_INSTANCE -> ProcessInstanceBatchState.from(state);
      case DISTRIBUTE_PARALLEL -> DistributeParallelLifeCycle.from(state);
      case DISTRIBUTE_SERIAL -> DistributeSerialLifeCycle.from(state);
      case ACTIVITY -> ActivityInstanceLifeCycle.from(state);
      case VARIABLE_API -> CommandApiVariableValueLifeCycle.from(state);
      case VARIABLE -> VariableLifeCycle.from(state);
      case USER_TASK_API -> CommandApiUserTaskValueLifeCycle.from(state);
      case USER_TASK -> UserTaskLifeCycle.from(state);
      case INCIDENT_API -> CommandApiIncidentValueLifeCycle.from(state);
      case INCIDENT -> IncidentLifeCycle.from(state);
      case JOB_API -> CommandApiJobValueLifeCycle.from(state);
      case JOB -> JobLifeCycle.from(state);
      case JOB_BATCH_API -> CommandApiJobBatchValueLifeCycle.from(state);
      case JOB_BATCH -> JobBatchLifeCycle.from(state);
      case TIMER_API -> throw new IllegalStateException("Unsupported valueType: " + valueType);
      case TIMER -> TimerLifeCycle.from(state);
      case MESSAGE_SUBSCRIPTION_API -> CommandApiMessageValueLifeCycle.from(state);
      case MESSAGE_SUBSCRIPTION -> MessageSubscriptionLifeCycle.from(state);
      case MESSAGE_DISTRIBUTE_CORRELATE -> MessageDistributeCorrelateLifeCycle.from(state);
      case SIGNAL_SUBSCRIPTION_API -> CommandApiSignalValueLifeCycle.from(state);
      case SIGNAL_SUBSCRIPTION -> SignalSubscriptionLifeCycle.from(state);
      case SIGNAL_DISTRIBUTE_CORRELATE -> SignalDistributeCorrelateLifeCycle.from(state);
      case DELAY_EVENT_COMMAND -> DelayLifeCycle.from(state);
      case HISTORY_CLEANUP -> HistoryCleanupLifeCycle.from(state);
      case QUERY_BUSINESS_ROUTE -> BusinessRouteApiLifeCycle.from(state);
      case RESPONSE_BUSINESS_ROUTE -> BusinessRouteApiLifeCycle.from(state);
      case EMPTY -> CommandApiEmptyValueLifeCycle.from(state);
      default -> throw new IllegalStateException("Illegal valueType: " + valueType);
    };
  }

  static int maxCardinality() {
    return INTENT_CLASSES.stream()
        .mapToInt(clazz -> clazz.getEnumConstants().length)
        .max()
        .orElse(1);
  }

  enum UnknownState implements ValueLifeCycle {
    UNKNOWN;

    @Override
    public short value() {
      return NULL_VAL;
    }

    @Override
    public boolean isEvent() {
      return false;
    }

    @Override
    public short processIndex() {
      return 0;
    }

    @Override
    public short recordIndex() {
      return 0;
    }

    @Override
    public ValueType getValueType() {
      return ValueType.UNKNOW;
    }
  }
}
