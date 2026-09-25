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
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
package com.anyilanxin.kunpeng.engine.bpmn.command.behavior.impl;

import static com.anyilanxin.kunpeng.structpack.util.BufferUtil.bufferAsString;

import com.anyilanxin.kunpeng.engine.bpmn.JobDeliveryPort;
import com.anyilanxin.kunpeng.engine.bpmn.LogEventWriter;
import com.anyilanxin.kunpeng.engine.script.ScriptContext;
import com.anyilanxin.kunpeng.protocol.business.impl.record.command.job.JobBatchRecord;
import com.anyilanxin.kunpeng.protocol.business.impl.record.command.job.JobRecord;
import com.anyilanxin.kunpeng.protocol.business.impl.record.commandapi.job.activate.JobInfoRecord;
import com.anyilanxin.kunpeng.protocol.business.record.command.job.JobBatchLifeCycle;
import com.anyilanxin.kunpeng.protocol.business.record.command.job.JobKindType;
import com.anyilanxin.kunpeng.structpack.value.StringValue;
import com.anyilanxin.kunpeng.structpack.value.ValueArray;
import java.time.InstantSource;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.agrona.DirectBuffer;

/**
 * 让处理器激活任务的 behavior 类。任何需要任务被激活并交给 job worker 处理的地方都应使用它。
 *
 * <p>编排模式：有可用消费流则先写 ACTIVATED 批事件（deadline/worker 随事件落账，存储层切激活态并登记到期索引），
 * 再经副作用推送——事件先于推送，事务提交后推送才发出； 无流则广播"该类型可消费"，job 留 READY 由 gateway 长轮询取走（拉取走标准命令通道，由批量激活处理器写同样的
 * ACTIVATED 事件）。 推送失败由 broker 侧 WITHDRAW 回退、超时由到期扫描兜底。
 *
 * @author zxuanhong
 * @since 2026.9.0
 */
public class BpmnJobDeliveryBehavior {
  private static final long DEFAULT_DEADLINE_MILLIS = 60_000;

  private final LogEventWriter writer;
  private final InstantSource clock;
  private final VariableBehavior variableBehavior;
  private static final Map<String, Object> EMPTY_MAP = new HashMap<>();

  public BpmnJobDeliveryBehavior(
      final LogEventWriter writer, final VariableBehavior variableBehavior) {
    this.writer = writer;
    clock = writer.clock();
    this.variableBehavior = variableBehavior;
  }

  public void deliver(final long jobKey, final JobRecord jobRecord) {
    final JobDeliveryPort deliveryPort = writer.getJobDeliveryPort();
    if (deliveryPort == null) {
      // 派发面未装配（如独立引擎运行）：job 留 READY，由拉取链路兜底
      return;
    }
    final String jobType = jobRecord.getJobType();
    final Optional<JobDeliveryPort.DeliveryChannel> stream = deliveryPort.pickStream(jobType);
    if (stream.isEmpty()) {
      writer.addSideEffect(
          () -> {
            deliveryPort.announceAvailable(jobType);
            return true;
          });
      return;
    }
    final long deadline = deadlineOf(jobRecord);
    final JobBatchRecord jobBatchRecord = new JobBatchRecord();
    jobBatchRecord.setBatchJobId(writer.nextCurrentSourceKey(jobKey));
    jobBatchRecord.setJobType(jobType);
    jobBatchRecord.setMaxJobsActivate(1);
    // 消费归属：推送目标（聚合数组下标）选定的 worker 随事件落 lockOwner——记住谁消费的
    jobBatchRecord.setWorker(stream.get().worker());
    jobBatchRecord.setDeadline(deadline);
    jobBatchRecord.jobKeys().add().setValue(jobKey);
    writer.addEvent(
        jobBatchRecord.getBatchJobId(), JobBatchLifeCycle.ACTIVATED, -1, jobBatchRecord);

    final int partitionId = writer.getPartitionId();
    writer.addSideEffect(
        () -> {
          // 推送失败不回滚激活态：broker 侧写 WITHDRAW 回退，或由到期扫描回 READY（at-most-once 自愈）
          stream.get().push(jobKey, partitionId, deadline, jobRecord);
          return true;
        });
  }

  /** 到期时间：记录的锁定时长（秒）优先，缺省 60s */
  private long deadlineOf(final JobRecord record) {
    final long lockExpireMillis = record.getLockExpireTime() * 1000L;
    final var now = clock.millis();
    return lockExpireMillis > 0 ? now + lockExpireMillis : now + DEFAULT_DEADLINE_MILLIS;
  }

  public void collectVariable(final ValueArray<StringValue> variables, final JobInfoRecord record) {
    final Set<String> variableKeys = new HashSet<>();
    for (final StringValue stringValue : variables) {
      variableKeys.add(bufferAsString(stringValue.getValue()));
    }
    collectVariable(variableKeys, record);
  }

  public void collectVariable(
      final Collection<DirectBuffer> variables, final JobInfoRecord record) {
    final Set<String> variableKeys = new HashSet<>(variables.size());
    for (final DirectBuffer directBuffer : variables) {
      variableKeys.add(bufferAsString(directBuffer));
    }
    collectVariable(variableKeys, record);
  }

  private void collectVariable(final Set<String> variableKeys, final JobInfoRecord record) {
    if (variableKeys.isEmpty()) {
      record.setVariables(EMPTY_MAP);
      return;
    }
    final JobKindType jobKind = record.getJobKind();
    final Map<String, Object> jobAllVariable = new HashMap<>();
    final Map<String, Object> variable = new HashMap<>(variableKeys.size());
    if (jobKind == JobKindType.PROCESS_LISTENER) {
      final ScriptContext scriptContext =
          variableBehavior.scriptContext(
              record.getProcessInstanceId(), record.getProcessInstanceId());
      jobAllVariable.putAll(scriptContext.getVariable());
    } else if (jobKind != JobKindType.UNKNOWN_ENUM_VALUE) {
      final ScriptContext scriptContext =
          variableBehavior.scriptContext(
              record.getProcessInstanceId(), record.getActivityInstanceId());
      jobAllVariable.putAll(scriptContext.getVariable());
    }
    for (final String key : variableKeys) {
      variable.put(key, jobAllVariable.get(key));
    }
    record.setVariables(variable);
  }
}
