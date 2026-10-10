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
package com.anyilanxin.kunpeng.sink.rdbms;

import com.anyilanxin.kunpeng.protocol.business.BusinessEventRecord;
import com.anyilanxin.kunpeng.protocol.business.ValueLifeCycle;
import com.anyilanxin.kunpeng.protocol.business.ValueType;
import com.anyilanxin.kunpeng.protocol.business.record.RecordType;
import com.anyilanxin.kunpeng.protocol.business.record.command.activityinstance.ActivityInstanceLifeCycle;
import com.anyilanxin.kunpeng.protocol.business.record.command.activityinstance.ActivityInstanceListenerType;
import com.anyilanxin.kunpeng.protocol.business.record.command.activityinstance.ActivityInstanceRecordValue;
import com.anyilanxin.kunpeng.protocol.business.record.command.activityinstance.ActivityInstanceState;
import com.anyilanxin.kunpeng.bpm.parse.bpmn.element.BpmnElementType;
import com.anyilanxin.kunpeng.protocol.business.record.command.deployment.ProcessDefinitionRecordValue;
import com.anyilanxin.kunpeng.protocol.business.record.command.deployment.StarterEventRecordValue;
import com.anyilanxin.kunpeng.protocol.business.record.command.historycleanup.HistoryCleanupRecordValue;
import com.anyilanxin.kunpeng.protocol.business.record.command.incident.IncidentRecordValue;
import com.anyilanxin.kunpeng.protocol.business.record.command.incident.IncidentType;
import com.anyilanxin.kunpeng.protocol.business.record.command.job.JobKindType;
import com.anyilanxin.kunpeng.protocol.business.record.command.job.JobLifeCycle;
import com.anyilanxin.kunpeng.protocol.business.record.command.job.JobRecordValue;
import com.anyilanxin.kunpeng.protocol.business.record.command.job.JobState;
import com.anyilanxin.kunpeng.protocol.business.record.command.message.MessageSubscriptionRecordValue;
import com.anyilanxin.kunpeng.protocol.business.record.command.message.MessageSubscriptionType;
import com.anyilanxin.kunpeng.protocol.business.record.command.processinstance.ProcessInstanceLifeCycle;
import com.anyilanxin.kunpeng.protocol.business.record.command.processinstance.ProcessInstanceListenerType;
import com.anyilanxin.kunpeng.protocol.business.record.command.processinstance.ProcessInstanceRecordValue;
import com.anyilanxin.kunpeng.protocol.business.record.command.processinstance.ProcessInstanceState;
import com.anyilanxin.kunpeng.protocol.business.record.command.signal.SignalSubscriptionRecordValue;
import com.anyilanxin.kunpeng.protocol.business.record.command.signal.SignalSubscriptionType;
import com.anyilanxin.kunpeng.protocol.business.record.command.timer.TimerElementType;
import com.anyilanxin.kunpeng.protocol.business.record.command.timer.TimerEventRecordValue;
import com.anyilanxin.kunpeng.protocol.business.record.command.timer.TimerState;
import com.anyilanxin.kunpeng.protocol.business.record.command.usertask.UserTaskLifeCycle;
import com.anyilanxin.kunpeng.protocol.business.record.command.usertask.UserTaskListenerType;
import com.anyilanxin.kunpeng.protocol.business.record.command.usertask.UserTaskRecordValue;
import com.anyilanxin.kunpeng.protocol.business.record.command.usertask.UserTaskState;
import com.anyilanxin.kunpeng.protocol.business.record.command.variable.VariableRecordValue;
import com.anyilanxin.kunpeng.protocol.common.RecordValue;
import com.anyilanxin.kunpeng.sink.api.context.CancellableTask;
import com.anyilanxin.kunpeng.sink.api.context.RecordMatcher;
import com.anyilanxin.kunpeng.sink.api.context.SinkConfiguration;
import com.anyilanxin.kunpeng.sink.api.context.SinkContext;
import com.anyilanxin.kunpeng.sink.api.context.SinkController;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.time.Duration;
import java.time.InstantSource;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** 测试用桩：sink 环境（上下文/控制器/配置）与记录值的最小实现。 */
@SuppressWarnings("unused")
final class Fakes {

  private Fakes() {}

  static SinkContext context(final Map<String, Object> args) {
    return new SinkContext() {
      private final SimpleMeterRegistry meters = new SimpleMeterRegistry();
      private RecordMatcher matcher;

      @Override
      public MeterRegistry getMeterRegistry() {
        return meters;
      }

      @Override
      public Logger getLogger() {
        return LoggerFactory.getLogger("test.rdbms-sink");
      }

      @Override
      public InstantSource getClock() {
        return InstantSource.system();
      }

      @Override
      public SinkConfiguration getConfiguration() {
        return configuration(args);
      }

      @Override
      public int getPartitionId() {
        return 1;
      }

      @Override
      public void setRecordMatcher(final RecordMatcher matcher) {
        this.matcher = matcher;
      }

      RecordMatcher matcher() {
        return matcher;
      }
    };
  }

  static SinkConfiguration configuration(final Map<String, Object> args) {
    return new SinkConfiguration() {
      @Override
      public String getId() {
        return "rdbms-test";
      }

      @Override
      public Map<String, Object> getArguments() {
        return args;
      }

      @Override
      public <T> T createSettings(final Class<T> settingsClass) {
        try {
          final var settings = settingsClass.getDeclaredConstructor().newInstance();
          for (final var entry : args.entrySet()) {
            final var setter =
                "set" + Character.toUpperCase(entry.getKey().charAt(0)) + entry.getKey().substring(1);
            for (final var method : settingsClass.getMethods()) {
              if (!method.getName().equals(setter) || method.getParameterCount() != 1) {
                continue;
              }
              final var type = method.getParameterTypes()[0];
              final Object value =
                  type == Duration.class
                      ? Duration.ofMillis(((Number) entry.getValue()).longValue())
                      : entry.getValue();
              method.invoke(settings, value);
              break;
            }
          }
          return settingsClass.cast(settings);
        } catch (final Exception e) {
          throw new IllegalArgumentException("Failed to bind test settings", e);
        }
      }
    };
  }

  static final class RecordingController implements SinkController {

    private long position = -1L;
    private byte[] metadata;

    @Override
    public void updatePosition(final long position) {
      this.position = Math.max(this.position, position);
    }

    @Override
    public void updatePosition(final long position, final byte[] metadata) {
      updatePosition(position);
      this.metadata = metadata;
    }

    @Override
    public long getPosition() {
      return position;
    }

    @Override
    public CancellableTask scheduleTask(final Duration delay, final Runnable task) {
      return () -> {};
    }

    @Override
    public Optional<byte[]> readMetadata() {
      return Optional.ofNullable(metadata);
    }
  }

  record FakeRecord<T extends RecordValue>(
      long position,
      long key,
      long timestamp,
      int resourceId,
      ValueType valueType,
      ValueLifeCycle valueState,
      T value)
      implements BusinessEventRecord<T> {

    @Override
    public long getPosition() {
      return position;
    }

    @Override
    public long getKey() {
      return key;
    }

    @Override
    public long getTimestamp() {
      return timestamp;
    }

    @Override
    public int getResourceId() {
      return resourceId;
    }

    @Override
    public ValueType getValueType() {
      return valueType;
    }

    @Override
    public ValueLifeCycle getValueState() {
      return valueState;
    }

    @Override
    public T getValue() {
      return value;
    }

    @Override
    public long getSourceRecordPosition() {
      return -1L;
    }

    @Override
    public RecordType getRecordType() {
      return RecordType.EVENT;
    }

    @Override
    public String getRejectionType() {
      return null;
    }

    @Override
    public String getRejectionReason() {
      return null;
    }

    @Override
    public String getBrokerVersion() {
      return "test";
    }

    @Override
    public int getRecordVersion() {
      return 1;
    }
  }

  static final class ProcessDefinitionValue implements ProcessDefinitionRecordValue {
    long processDefinitionId = 1001;
    String processDefinitionKey = "order-process";
    String processDefinitionName = "Order Process";
    int processDefinitionVersion = 3;
    String versionTag = "v3";
    int historyTimeToLive = 30;
    long deploymentId = 77;
    long resourceDefinitionId = 88;
    String resourceDefinitionName = "order.bpmn";
    byte[] checksum = {1, 2, 3};
    byte[] resource = "<definitions/>".getBytes();

    @Override
    public long getProcessDefinitionId() {
      return processDefinitionId;
    }

    @Override
    public String getProcessDefinitionName() {
      return processDefinitionName;
    }

    @Override
    public String getProcessDefinitionKey() {
      return processDefinitionKey;
    }

    @Override
    public int getProcessDefinitionVersion() {
      return processDefinitionVersion;
    }

    @Override
    public int getHistoryTimeToLive() {
      return historyTimeToLive;
    }

    @Override
    public Set<String> getCandidateStarterGroups() {
      return Set.of();
    }

    @Override
    public Set<String> getCandidateStarterUsers() {
      return Set.of();
    }

    @Override
    public String getVersionTag() {
      return versionTag;
    }

    @Override
    public long getDeploymentId() {
      return deploymentId;
    }

    @Override
    public long getResourceDefinitionId() {
      return resourceDefinitionId;
    }

    @Override
    public String getResourceDefinitionName() {
      return resourceDefinitionName;
    }

    @Override
    public byte[] getChecksum() {
      return checksum;
    }

    @Override
    public byte[] getResource() {
      return resource;
    }

    @Override
    public List<StarterEventRecordValue> getStarterEvents() {
      return List.of();
    }

    @Override
    public boolean isSuspension() {
      return false;
    }

    @Override
    public boolean isStartable() {
      return true;
    }

    @Override
    public String getTenantId() {
      return "";
    }
  }

  static final class ProcessInstanceValue implements ProcessInstanceRecordValue {
    long processInstanceId = 2001;
    long parentProcessInstanceId;
    long rootProcessInstanceId;
    int rev = 1;
    String businessKey = "bk-1";
    String processDefinitionKey = "order-process";
    String processDefinitionName = "Order Process";
    long processDefinitionId = 1001;
    String startUserId = "alice";
    ProcessInstanceState state = ProcessInstanceState.ACTIVATED;

    @Override
    public long getProcessInstanceId() {
      return processInstanceId;
    }

    @Override
    public long getParentProcessInstanceId() {
      return parentProcessInstanceId;
    }

    @Override
    public long getRootProcessInstanceId() {
      return rootProcessInstanceId;
    }

    @Override
    public int getRev() {
      return rev;
    }

    @Override
    public long getReferenceActivityInstanceId() {
      return 0;
    }

    @Override
    public String getBusinessKey() {
      return businessKey;
    }

    @Override
    public String getProcessDefinitionKey() {
      return processDefinitionKey;
    }

    @Override
    public String getProcessDefinitionName() {
      return processDefinitionName;
    }

    @Override
    public long getProcessDefinitionId() {
      return processDefinitionId;
    }

    @Override
    public String getStartUserId() {
      return startUserId;
    }

    @Override
    public Set<String> getStartActivityDefinitionKeys() {
      return Set.of();
    }

    @Override
    public Set<Long> getStartActivityInstanceIds() {
      return Set.of();
    }

    @Override
    public Set<String> getEndActivityDefinitionKeys() {
      return Set.of();
    }

    @Override
    public Set<Long> getEndActivityInstanceIds() {
      return Set.of();
    }

    @Override
    public ProcessInstanceState getState() {
      return state;
    }

    @Override
    public ProcessInstanceLifeCycle getLifeCycle() {
      return null;
    }

    @Override
    public ProcessInstanceListenerType getListenerType() {
      return null;
    }

    @Override
    public int getListenerIndex() {
      return 0;
    }

    @Override
    public String getTenantId() {
      return "";
    }

    @Override
    public Map<String, Object> getAdditions() {
      return Map.of();
    }

    @Override
    public Map<String, Object> getVariables() {
      return Map.of();
    }

    @Override
    public long getStartTime() {
      return 0;
    }

    @Override
    public long getEndTime() {
      return 0;
    }

    @Override
    public long getDuration() {
      return 0;
    }
  }

  static final class ActivityInstanceValue implements ActivityInstanceRecordValue {
    long activityInstanceId = 3001;
    long processInstanceId = 2001;
    long activityDefinitionKey;
    String activityName = "Pick Items";
    BpmnElementType activityElementType = BpmnElementType.SERVICE_TASK;
    String startActivityDefinitionKey;
    long startActivityInstanceId;
    long startTime = 1_000;
    long endTime;
    long duration;

    @Override
    public long getActivityInstanceId() {
      return activityInstanceId;
    }

    @Override
    public long getParentActivityInstanceId() {
      return 0;
    }

    @Override
    public int getRev() {
      return 1;
    }

    @Override
    public long getProcessInstanceId() {
      return processInstanceId;
    }

    @Override
    public long getRootProcessInstanceId() {
      return 2001;
    }

    @Override
    public String getProcessDefinitionKey() {
      return "order-process";
    }

    @Override
    public long getProcessDefinitionId() {
      return 1001;
    }

    @Override
    public long getCallProcessInstanceId() {
      return 0;
    }

    @Override
    public String getActivityDefinitionKey() {
      return "pick-items";
    }

    @Override
    public String getActivityDefinitionName() {
      return activityName;
    }

    @Override
    public BpmnElementType getActivityDefinitionType() {
      return activityElementType;
    }

    @Override
    public long getTaskId() {
      return 0;
    }

    @Override
    public String getAssignee() {
      return null;
    }

    @Override
    public String getStartActivityDefinitionKey() {
      return startActivityDefinitionKey;
    }

    @Override
    public long getStartActivityInstanceId() {
      return startActivityInstanceId;
    }

    @Override
    public ActivityInstanceState getState() {
      return ActivityInstanceState.ACTIVE;
    }

    @Override
    public long getSequenceCounter() {
      return 1;
    }

    @Override
    public long getIncidentId() {
      return 0;
    }

    @Override
    public long getStartTime() {
      return startTime;
    }

    @Override
    public long getEndTime() {
      return endTime;
    }

    @Override
    public long getDuration() {
      return duration;
    }

    @Override
    public ActivityInstanceLifeCycle getLifeCycle() {
      return null;
    }

    @Override
    public ActivityInstanceListenerType getListenerType() {
      return null;
    }

    @Override
    public int getListenerIndex() {
      return 0;
    }

    @Override
    public String getTenantId() {
      return "";
    }

    @Override
    public Map<String, Object> getAdditions() {
      return Map.of();
    }

    @Override
    public Map<String, Object> getVariables() {
      return Map.of();
    }
  }

  static final class VariableValue implements VariableRecordValue {
    long scopId = 4001;
    long parentScopId = 2001;
    int rev = 1;
    long processDefinitionId = 1001;
    long processInstanceId = 2001;
    Map<String, Object> variables = new HashMap<>();

    @Override
    public long getScopId() {
      return scopId;
    }

    @Override
    public long getParentScopId() {
      return parentScopId;
    }

    @Override
    public int getRev() {
      return rev;
    }

    @Override
    public long getProcessDefinitionId() {
      return processDefinitionId;
    }

    @Override
    public long getProcessInstanceId() {
      return processInstanceId;
    }

    @Override
    public Map<String, Object> getVariables() {
      return variables;
    }

    @Override
    public String getTenantId() {
      return "";
    }
  }

  static final class UserTaskValue implements UserTaskRecordValue {
    long taskId = 5001;
    long activityInstanceId = 3001;
    long processInstanceId = 2001;
    long processDefinitionId = 1001;
    String assignee = "bob";
    UserTaskState state = UserTaskState.ACTIVE;
    long startTime = 1_000;

    @Override
    public long getTaskId() {
      return taskId;
    }

    @Override
    public long getParentTaskId() {
      return 0;
    }

    @Override
    public int getRev() {
      return 1;
    }

    @Override
    public long getActivityInstanceId() {
      return activityInstanceId;
    }

    @Override
    public UserTaskState getState() {
      return state;
    }

    @Override
    public long getProcessInstanceId() {
      return processInstanceId;
    }

    @Override
    public String getProcessDefinitionKey() {
      return "order-process";
    }

    @Override
    public long getProcessDefinitionId() {
      return processDefinitionId;
    }

    @Override
    public String getBusinessKey() {
      return "bk-1";
    }

    @Override
    public String getTaskDefinitionKey() {
      return "approve";
    }

    @Override
    public String getTaskDefinitionName() {
      return "Approve Order";
    }

    @Override
    public String getAssignee() {
      return assignee;
    }

    @Override
    public String getOwner() {
      return null;
    }

    @Override
    public int getPriority() {
      return 50;
    }

    @Override
    public long getDueDate() {
      return 0;
    }

    @Override
    public long getFollowUpDate() {
      return 0;
    }

    @Override
    public UserTaskLifeCycle getLifeCycle() {
      return null;
    }

    @Override
    public List<String> getCandidateGroups() {
      return List.of();
    }

    @Override
    public List<String> getCandidateUsers() {
      return List.of();
    }

    @Override
    public UserTaskListenerType getListenerType() {
      return null;
    }

    @Override
    public int getListenerIndex() {
      return 0;
    }

    @Override
    public long getStartTime() {
      return startTime;
    }

    @Override
    public long getEndTime() {
      return 0;
    }

    @Override
    public long getDuration() {
      return 0;
    }

    @Override
    public String getTenantId() {
      return "";
    }

    @Override
    public Map<String, Object> getAdditions() {
      return Map.of();
    }

    @Override
    public Map<String, Object> getVariables() {
      return Map.of();
    }
  }

  static final class JobValue implements JobRecordValue {
    long jobId = 6001;
    String jobType = "pack-items";
    String state = "ACTIVE";

    @Override
    public long getJobId() {
      return jobId;
    }

    @Override
    public String getJobType() {
      return jobType;
    }

    @Override
    public JobKindType getJobKind() {
      return null;
    }

    @Override
    public int getRetries() {
      return 3;
    }

    @Override
    public int getRetryBackOff() {
      return 0;
    }

    @Override
    public int getPriority() {
      return 10;
    }

    @Override
    public int getRev() {
      return 1;
    }

    @Override
    public long getDueDate() {
      return 2_000;
    }

    @Override
    public int getLockExpireTime() {
      return 0;
    }

    @Override
    public String getLockOwner() {
      return null;
    }

    @Override
    public String getProcessDefinitionKey() {
      return "order-process";
    }

    @Override
    public long getProcessDefinitionId() {
      return 1001;
    }

    @Override
    public long getProcessInstanceId() {
      return 2001;
    }

    @Override
    public long getActivityInstanceId() {
      return 3001;
    }

    @Override
    public String getActivityDefinitionKey() {
      return "pick-items";
    }

    @Override
    public long getTaskId() {
      return 0;
    }

    @Override
    public JobLifeCycle getLifeCycle() {
      return null;
    }

    @Override
    public JobState getState() {
      return JobState.ACTIVE;
    }

    @Override
    public long getIncidentId() {
      return 0;
    }

    @Override
    public String getDeniedReason() {
      return null;
    }

    @Override
    public long getStartTime() {
      return 1_000;
    }

    @Override
    public long getEndTime() {
      return 0;
    }

    @Override
    public long getDuration() {
      return 0;
    }

    @Override
    public Map<String, Object> getVariables() {
      return Map.of();
    }

    @Override
    public Map<String, Object> getLocalVariables() {
      return Map.of();
    }

    @Override
    public boolean isDenied() {
      return false;
    }

    @Override
    public String getTenantId() {
      return "";
    }
  }

  static final class IncidentValue implements IncidentRecordValue {
    long incidentId = 7001;
    long processInstanceId = 2001;

    @Override
    public long getIncidentId() {
      return incidentId;
    }

    @Override
    public IncidentType getIncidentType() {
      return IncidentType.ACTIVITY;
    }

    @Override
    public String getIncidentMessage() {
      return "job failed";
    }

    @Override
    public String getProcessDefinitionKey() {
      return "order-process";
    }

    @Override
    public long getProcessDefinitionId() {
      return 1001;
    }

    @Override
    public long getProcessInstanceId() {
      return processInstanceId;
    }

    @Override
    public String getActivityDefinitionKey() {
      return "pick-items";
    }

    @Override
    public long getActivityInstanceId() {
      return 3001;
    }

    @Override
    public long getTaskId() {
      return 0;
    }

    @Override
    public long getJobId() {
      return 6001;
    }

    @Override
    public ValueType getIncidentRecordValueType() {
      return ValueType.JOB;
    }

    @Override
    public ValueLifeCycle getIncidentRecordLifeCycle() {
      return null;
    }

    @Override
    public long getStartTime() {
      return 0;
    }

    @Override
    public long getEndTime() {
      return 0;
    }

    @Override
    public long getDuration() {
      return 0;
    }

    @Override
    public String getTenantId() {
      return "";
    }
  }

  static final class TimerValue implements TimerEventRecordValue {
    long timerId = 8001;

    @Override
    public long getTimerId() {
      return timerId;
    }

    @Override
    public long getDueDate() {
      return 5_000;
    }

    @Override
    public int getRepetitions() {
      return 1;
    }

    @Override
    public long getProcessDefinitionId() {
      return 1001;
    }

    @Override
    public String getProcessDefinitionKey() {
      return "order-process";
    }

    @Override
    public long getProcessInstanceId() {
      return 2001;
    }

    @Override
    public long getActivityInstanceId() {
      return 3001;
    }

    @Override
    public String getActivityDefinitionKey() {
      return "wait";
    }

    @Override
    public TimerElementType getTimerElementType() {
      return null;
    }

    @Override
    public String getTimerType() {
      return "duration";
    }

    @Override
    public String getTimerContent() {
      return "PT1H";
    }

    @Override
    public TimerState getState() {
      return TimerState.CREATED;
    }

    @Override
    public boolean isInterrupting() {
      return false;
    }

    @Override
    public long getStartTime() {
      return 0;
    }

    @Override
    public long getEndTime() {
      return 0;
    }

    @Override
    public long getDuration() {
      return 0;
    }

    @Override
    public String getTenantId() {
      return "";
    }
  }

  static final class MessageSubscriptionValue implements MessageSubscriptionRecordValue {
    long messageSubscriptionId = 9001;

    @Override
    public long getMessageSubscriptionId() {
      return messageSubscriptionId;
    }

    @Override
    public String getMessageName() {
      return "order-received";
    }

    @Override
    public MessageSubscriptionType getMessageType() {
      return null;
    }

    @Override
    public String getCorrelationKey() {
      return "order-42";
    }

    @Override
    public long getProcessDefinitionId() {
      return 1001;
    }

    @Override
    public String getProcessDefinitionKey() {
      return "order-process";
    }

    @Override
    public long getProcessInstanceId() {
      return 2001;
    }

    @Override
    public String getActivityDefinitionKey() {
      return "catch-order";
    }

    @Override
    public long getActivityInstanceId() {
      return 3001;
    }

    @Override
    public long getCorrelationActivityInstanceId() {
      return 0;
    }

    @Override
    public long getCorrelationProcessInstanceId() {
      return 0;
    }

    @Override
    public boolean isInterrupting() {
      return false;
    }

    @Override
    public Map<String, Object> getVariables() {
      return Map.of();
    }

    @Override
    public String getTenantId() {
      return "";
    }
  }

  static final class SignalSubscriptionValue implements SignalSubscriptionRecordValue {
    long signalSubscriptionId = 9101;

    @Override
    public long getSignalSubscriptionId() {
      return signalSubscriptionId;
    }

    @Override
    public String getSignalName() {
      return "shutdown";
    }

    @Override
    public SignalSubscriptionType getSignalType() {
      return null;
    }

    @Override
    public long getProcessDefinitionId() {
      return 1001;
    }

    @Override
    public String getProcessDefinitionKey() {
      return "order-process";
    }

    @Override
    public long getProcessInstanceId() {
      return 2001;
    }

    @Override
    public String getActivityDefinitionKey() {
      return "catch-signal";
    }

    @Override
    public long getActivityInstanceId() {
      return 3001;
    }

    @Override
    public boolean isInterrupting() {
      return false;
    }

    @Override
    public Map<String, Object> getVariables() {
      return Map.of();
    }

    @Override
    public String getTenantId() {
      return "";
    }
  }

  static final class HistoryCleanupValue implements HistoryCleanupRecordValue {
    long processInstanceId = 2001;

    @Override
    public long getHistoryCleanupId() {
      return 1;
    }

    @Override
    public long getProcessInstanceId() {
      return processInstanceId;
    }

    @Override
    public String getProcessDefinitionName() {
      return "Order Process";
    }

    @Override
    public long getProcessDefinitionId() {
      return 1001;
    }

    @Override
    public String getProcessDefinitionKey() {
      return "order-process";
    }

    @Override
    public long getDueDate() {
      return 0;
    }

    @Override
    public long getStartTime() {
      return 0;
    }

    @Override
    public long getEndTime() {
      return 0;
    }

    @Override
    public long getDuration() {
      return 0;
    }

    @Override
    public String getTenantId() {
      return "";
    }
  }
}
