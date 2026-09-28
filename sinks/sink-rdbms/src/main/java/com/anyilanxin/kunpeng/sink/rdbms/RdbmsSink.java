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
package com.anyilanxin.kunpeng.sink.rdbms;

import com.anyilanxin.kunpeng.protocol.business.BusinessEventRecord;
import com.anyilanxin.kunpeng.protocol.business.ValueType;
import com.anyilanxin.kunpeng.sink.api.RecordSink;
import com.anyilanxin.kunpeng.sink.api.context.CancellableTask;
import com.anyilanxin.kunpeng.sink.api.context.SinkContext;
import com.anyilanxin.kunpeng.sink.api.context.SinkController;
import com.anyilanxin.kunpeng.sink.rdbms.handler.ActivityInstanceRecordHandler;
import com.anyilanxin.kunpeng.sink.rdbms.handler.HistoryCleanupRecordHandler;
import com.anyilanxin.kunpeng.sink.rdbms.handler.IncidentRecordHandler;
import com.anyilanxin.kunpeng.sink.rdbms.handler.JobRecordHandler;
import com.anyilanxin.kunpeng.sink.rdbms.handler.MessageSubscriptionRecordHandler;
import com.anyilanxin.kunpeng.sink.rdbms.handler.ProcessDefinitionRecordHandler;
import com.anyilanxin.kunpeng.sink.rdbms.handler.ProcessInstanceRecordHandler;
import com.anyilanxin.kunpeng.sink.rdbms.handler.RecordModelHandler;
import com.anyilanxin.kunpeng.sink.rdbms.handler.SignalSubscriptionRecordHandler;
import com.anyilanxin.kunpeng.sink.rdbms.handler.TimerRecordHandler;
import com.anyilanxin.kunpeng.sink.rdbms.handler.UserTaskRecordHandler;
import com.anyilanxin.kunpeng.sink.rdbms.handler.VariableRecordHandler;
import com.anyilanxin.kunpeng.sink.rdbms.mapper.Database;
import com.anyilanxin.kunpeng.sink.rdbms.write.BatchFlusher;
import com.anyilanxin.kunpeng.sink.rdbms.write.ChangeBuffer;
import com.anyilanxin.kunpeng.sink.rdbms.write.RowChange;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Timer;
import java.sql.SQLException;
import java.time.Duration;
import java.util.EnumMap;
import java.util.Map;
import org.slf4j.Logger;

/**
 * 把引擎事件流落到关系库的 {@link RecordSink} 实现。
 *
 * <p>写路径模型：记录按日志顺序到达本 sink 的单线程，转换层把每条记录翻译成表实体并以 save（insert）/ update 两种形态进入 {@link
 * ChangeBuffer}；缓冲按「形态 + 主键」收敛（同一窗口内同主键的连续 update 只剩最后一笔， insert 与 update
 * 保持先后、同事务内自然合并），刷盘时整窗进入同一个事务批量提交，成功后才向引擎确认位置。 任意一步失败则整体回滚，依赖引擎重投递收敛。
 *
 * <p>配置项见 {@link RdbmsSinkSettings}；建表默认自动迁移。
 *
 * @author zxuanhong
 * @since 2026.9.0
 */
public final class RdbmsSink implements RecordSink {

  private RdbmsSinkSettings settings;
  private Database database;
  private BatchFlusher flusher;
  private ChangeBuffer buffer;
  private Map<ValueType, RecordModelHandler> routes;
  private SinkController controller;
  private CancellableTask flushTask;
  private Logger log;
  private Timer flushTimer;
  private Counter flushFailures;
  private int partitionId = SinkContext.PARTITION_ID_UNSET;
  private long ackedPosition = -1L;

  @Override
  public void initialize(final SinkContext context) {
    settings = context.getConfiguration().createSettings(RdbmsSinkSettings.class);
    settings.validate();
    partitionId = context.getPartitionId();
    log = context.getLogger();
    database = Database.open(settings);
    if (settings.isAutoDdl()) {
      database.migrate(settings);
    }
    flusher = new BatchFlusher();
    buffer = new ChangeBuffer();
    routes = defaultRoutes();
    context.setRecordMatcher(new RdbmsRecordMatcher());

    final var meters = context.getMeterRegistry();
    flushTimer =
        Timer.builder("kunpeng.sink.rdbms.flush").description("flush duration").register(meters);
    flushFailures =
        Counter.builder("kunpeng.sink.rdbms.flush.failures")
            .description("failed flush attempts")
            .register(meters);
    log.info("Rdbms sink initialized against {} ({})", settings.getUrl(), database.dialect());
  }

  @Override
  public void start(final SinkController controller) {
    this.controller = controller;
    scheduleFlush();
  }

  @Override
  public void sink(final BusinessEventRecord<?> record) {
    buffer.advancePosition(record.getPosition());
    final var mapper = routes.get(record.getValueType());
    if (mapper != null && mapper.accepts(record.getValueState())) {
      mapper.transition(record, buffer);
    }
    if (buffer.size() >= settings.getFlushThreshold()) {
      // 背压：待写变更达到上限，在处理线程内同步刷盘；失败抛出，触发引擎对当前记录的重投递
      flush();
    }
  }

  @Override
  public void pause() {
    flushQuietly("pause");
  }

  @Override
  public void resume() {
    scheduleFlush();
  }

  @Override
  public void close() {
    if (flushTask != null) {
      flushTask.cancel();
    }
    flushQuietly("close");
    database.close();
    log.info("Rdbms sink closed");
  }

  @Override
  public void purge() {
    database.purge();
  }

  @Override
  public void onPartitionRemoved() {
    // 本 sink 的行都带 resource_id（分区标识），分区移除时按列清理对应数据
    if (partitionId == SinkContext.PARTITION_ID_UNSET) {
      return;
    }
    database.removePartitionData(partitionId);
  }

  /**
   * 刷盘：整窗变更单事务提交（save 走 insert、update 走 update、清理走 delete），成功后确认位置。
   * 失败时抛出——调用方若在记录处理路径上，引擎将重投递当前记录；缓冲内容未动，重试基于同一份快照。
   */
  void flush() {
    final var position = buffer.lastPosition();
    if (buffer.isEmpty()) {
      acknowledge(position);
      return;
    }
    final var sample = Timer.start();
    try {
      try (final var session = database.openSession()) {
        flusher.enqueue(session, buffer.view());
        session.commit();
      } catch (final Exception e) {
        if (!isIntegrityConstraintViolation(e)) {
          throw e;
        }
        recoverWindow();
      }
    } catch (final Exception e) {
      flushFailures.increment();
      throw new IllegalStateException("Rdbms sink failed to flush at position " + position, e);
    } finally {
      sample.stop(flushTimer);
    }
    buffer.clear();
    acknowledge(position);
  }

  /**
   * 崩溃落在「已提交、未确认」之间时，重投递会把创建事件原样再送一遍，批量 insert 撞主键。 这里逐条开独立事务重放本窗口： 撞主键的 insert 降级为按可变列
   * update（行已在丢失确认的提交中落库，收敛到最新状态）， 其余变更照常执行——混合了重放行与新行的窗口也不会丢新行。
   */
  private void recoverWindow() {
    for (final var change : buffer.view()) {
      if (apply(change, false) || change.kind() != RowChange.Kind.INSERT) {
        continue;
      }
      if (!apply(change, true)) {
        throw new IllegalStateException(
            "Rdbms sink failed to recover insert for " + change.table().name());
      }
    }
  }

  private boolean apply(final RowChange change, final boolean insertAsUpdate) {
    try (final var session = database.openSession()) {
      flusher.enqueueSingle(session, change, insertAsUpdate);
      session.commit();
      return true;
    } catch (final Exception e) {
      if (isIntegrityConstraintViolation(e)) {
        return false;
      }
      throw e;
    }
  }

  private static boolean isIntegrityConstraintViolation(final Throwable error) {
    for (var cause = error; cause != null; cause = cause.getCause()) {
      if (cause instanceof final SQLException sql
          && sql.getSQLState() != null
          && sql.getSQLState().startsWith("23")) {
        return true;
      }
    }
    return false;
  }

  private void acknowledge(final long position) {
    if (position > ackedPosition) {
      ackedPosition = position;
      controller.updatePosition(position);
    }
  }

  private void scheduleFlush() {
    final Duration interval = settings.getFlushInterval();
    if (interval.isZero() || controller == null) {
      return;
    }
    flushTask = controller.scheduleTask(interval, this::onFlushTimer);
  }

  private void onFlushTimer() {
    try {
      flush();
    } catch (final Exception e) {
      // 定时刷盘失败不打断线程：位置未推进，后续记录达到阈值时会再次尝试
      flushFailures.increment();
      log.warn("Scheduled rdbms flush failed, will retry on next trigger", e);
    } finally {
      scheduleFlush();
    }
  }

  private void flushQuietly(final String phase) {
    try {
      flush();
    } catch (final Exception e) {
      log.warn("Failed to flush rdbms buffer during {}", phase, e);
    }
  }

  private static Map<ValueType, RecordModelHandler> defaultRoutes() {
    final Map<ValueType, RecordModelHandler> routes = new EnumMap<>(ValueType.class);
    routes.put(ValueType.PROCESS_DEFINITION, new ProcessDefinitionRecordHandler());
    routes.put(ValueType.PROCESS_INSTANCE, new ProcessInstanceRecordHandler());
    routes.put(ValueType.ACTIVITY, new ActivityInstanceRecordHandler());
    routes.put(ValueType.VARIABLE, new VariableRecordHandler());
    routes.put(ValueType.USER_TASK, new UserTaskRecordHandler());
    routes.put(ValueType.JOB, new JobRecordHandler());
    routes.put(ValueType.INCIDENT, new IncidentRecordHandler());
    routes.put(ValueType.TIMER, new TimerRecordHandler());
    routes.put(ValueType.MESSAGE_SUBSCRIPTION, new MessageSubscriptionRecordHandler());
    routes.put(ValueType.SIGNAL_SUBSCRIPTION, new SignalSubscriptionRecordHandler());
    routes.put(ValueType.HISTORY_CLEANUP, new HistoryCleanupRecordHandler());
    return routes;
  }
}
