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
package com.anyilanxin.kunpeng.sink.rdbms.write;

import com.anyilanxin.kunpeng.sink.rdbms.mapper.SinkMappers;
import com.anyilanxin.kunpeng.sink.rdbms.mapper.TableSpec;
import com.anyilanxin.kunpeng.sink.rdbms.mapper.Tables;
import com.anyilanxin.kunpeng.sink.rdbms.model.ActivityInstanceDbModel;
import com.anyilanxin.kunpeng.sink.rdbms.model.IncidentDbModel;
import com.anyilanxin.kunpeng.sink.rdbms.model.JobDbModel;
import com.anyilanxin.kunpeng.sink.rdbms.model.MessageSubscriptionDbModel;
import com.anyilanxin.kunpeng.sink.rdbms.model.ProcessDefinitionDbModel;
import com.anyilanxin.kunpeng.sink.rdbms.model.ProcessInstanceDbModel;
import com.anyilanxin.kunpeng.sink.rdbms.model.SignalSubscriptionDbModel;
import com.anyilanxin.kunpeng.sink.rdbms.model.TimerDbModel;
import com.anyilanxin.kunpeng.sink.rdbms.model.UserTaskDbModel;
import com.anyilanxin.kunpeng.sink.rdbms.model.VariableDbModel;
import java.util.LinkedHashMap;
import java.util.Map;
import org.apache.ibatis.session.SqlSession;

/**
 * 把待写变更路由到对应实体的映射器：save 走 {@code insert}，update 走 {@code update}， 按列删除走 {@code cleanupHistory}。
 *
 * <p>会话使用批处理执行器，{@link #enqueue} 的每次调用只是入队；真正的一次性批量发送发生在 {@link SqlSession#commit()}。整个 flush
 * 在同一事务里提交，失败整体回滚——配合记录重投递与 {@link #enqueueSingle 恢复路径}的幂等收敛保证最终一致。
 *
 * @author zxuanhong
 * @since 2026.9.0
 */
public final class BatchFlusher {

  /** 单个行实体的目标映射器入口。 */
  @FunctionalInterface
  private interface ModelSink {

    void accept(SinkMappers mappers, Object model);
  }

  /** 按流程实例删除历史数据的映射器入口。 */
  @FunctionalInterface
  private interface HistorySink {

    void accept(SinkMappers mappers, long processInstanceId);
  }

  private final Map<TableSpec, ModelSink> inserts = new LinkedHashMap<>();
  private final Map<TableSpec, ModelSink> updates = new LinkedHashMap<>();
  private final Map<TableSpec, HistorySink> historyDeletes = new LinkedHashMap<>();

  public BatchFlusher() {
    inserts.put(
        Tables.PROCESS_DEFINITION,
        (m, v) -> m.processDefinition().insert((ProcessDefinitionDbModel) v));
    inserts.put(
        Tables.PROCESS_INSTANCE, (m, v) -> m.processInstance().insert((ProcessInstanceDbModel) v));
    inserts.put(
        Tables.ACTIVITY_INSTANCE,
        (m, v) -> m.activityInstance().insert((ActivityInstanceDbModel) v));
    inserts.put(Tables.VARIABLE, (m, v) -> m.variable().insert((VariableDbModel) v));
    inserts.put(Tables.USER_TASK, (m, v) -> m.userTask().insert((UserTaskDbModel) v));
    inserts.put(Tables.JOB, (m, v) -> m.job().insert((JobDbModel) v));
    inserts.put(Tables.INCIDENT, (m, v) -> m.incident().insert((IncidentDbModel) v));
    inserts.put(Tables.TIMER, (m, v) -> m.timer().insert((TimerDbModel) v));
    inserts.put(
        Tables.MESSAGE_SUBSCRIPTION,
        (m, v) -> m.messageSubscription().insert((MessageSubscriptionDbModel) v));
    inserts.put(
        Tables.SIGNAL_SUBSCRIPTION,
        (m, v) -> m.signalSubscription().insert((SignalSubscriptionDbModel) v));

    updates.put(
        Tables.PROCESS_INSTANCE, (m, v) -> m.processInstance().update((ProcessInstanceDbModel) v));
    updates.put(
        Tables.ACTIVITY_INSTANCE,
        (m, v) -> m.activityInstance().update((ActivityInstanceDbModel) v));
    updates.put(Tables.VARIABLE, (m, v) -> m.variable().update((VariableDbModel) v));
    updates.put(Tables.USER_TASK, (m, v) -> m.userTask().update((UserTaskDbModel) v));
    updates.put(Tables.JOB, (m, v) -> m.job().update((JobDbModel) v));
    updates.put(Tables.INCIDENT, (m, v) -> m.incident().update((IncidentDbModel) v));
    updates.put(Tables.TIMER, (m, v) -> m.timer().update((TimerDbModel) v));
    updates.put(
        Tables.MESSAGE_SUBSCRIPTION,
        (m, v) -> m.messageSubscription().update((MessageSubscriptionDbModel) v));
    updates.put(
        Tables.SIGNAL_SUBSCRIPTION,
        (m, v) -> m.signalSubscription().update((SignalSubscriptionDbModel) v));

    historyDeletes.put(Tables.PROCESS_INSTANCE, (m, id) -> m.processInstance().cleanupHistory(id));
    historyDeletes.put(
        Tables.ACTIVITY_INSTANCE, (m, id) -> m.activityInstance().cleanupHistory(id));
    historyDeletes.put(Tables.VARIABLE, (m, id) -> m.variable().cleanupHistory(id));
    historyDeletes.put(Tables.USER_TASK, (m, id) -> m.userTask().cleanupHistory(id));
    historyDeletes.put(Tables.INCIDENT, (m, id) -> m.incident().cleanupHistory(id));
    historyDeletes.put(Tables.TIMER, (m, id) -> m.timer().cleanupHistory(id));
    historyDeletes.put(
        Tables.MESSAGE_SUBSCRIPTION, (m, id) -> m.messageSubscription().cleanupHistory(id));
    historyDeletes.put(
        Tables.SIGNAL_SUBSCRIPTION, (m, id) -> m.signalSubscription().cleanupHistory(id));
  }

  /**
   * 在给定会话上入队全部变更（调用方负责一次提交或回滚）。
   *
   * @return 入队的语句数
   */
  public int enqueue(final SqlSession session, final Iterable<RowChange> changes) {
    final var mappers = SinkMappers.from(session);
    var count = 0;
    for (final var change : changes) {
      route(change, mappers, false);
      count++;
    }
    return count;
  }

  /**
   * 恢复路径：在给定会话上执行单条变更（调用方逐条开独立事务并提交）。
   *
   * @param insertAsUpdate {@code true} 时把 insert 降级为 update 派发——仅用于重投递导致主键冲突后的单条重试； 实体没有 update
   *     语句时（如不可变的流程定义）静默跳过，行已在丢失确认的提交中落库
   */
  public void enqueueSingle(
      final SqlSession session, final RowChange change, final boolean insertAsUpdate) {
    route(change, SinkMappers.from(session), insertAsUpdate);
  }

  private void route(
      final RowChange change, final SinkMappers mappers, final boolean insertAsUpdate) {
    switch (change.kind()) {
      case INSERT -> dispatch(insertAsUpdate ? updates : inserts, change, mappers, insertAsUpdate);
      case UPDATE -> dispatch(updates, change, mappers, false);
      case DELETE -> {
        final var delete = historyDeletes.get(change.table());
        if (delete == null) {
          throw new IllegalStateException("No delete route for " + change.table().name());
        }
        delete.accept(mappers, ((Number) change.keyValues().get(0)).longValue());
      }
    }
  }

  private static void dispatch(
      final Map<TableSpec, ModelSink> routes,
      final RowChange change,
      final SinkMappers mappers,
      final boolean allowMissing) {
    final var sink = routes.get(change.table());
    if (sink == null) {
      if (allowMissing) {
        return;
      }
      throw new IllegalStateException(
          "No route for " + change.kind() + " on " + change.table().name());
    }
    sink.accept(mappers, change.model());
  }
}
