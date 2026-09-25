/*
 * Copyright © 2026 anyilanxin zxh (anyilanxin@aliyun.com)
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
package com.anyilanxin.kunpeng.broker.jobstream;

import com.anyilanxin.kunpeng.broker.client.jobstream.JobStreamMessages.StreamPush;
import com.anyilanxin.kunpeng.eventlog.AppendResult;
import com.anyilanxin.kunpeng.eventlog.EventLogWriter;
import com.anyilanxin.kunpeng.eventlog.WriteContext;
import com.anyilanxin.kunpeng.protocol.business.ValueType;
import com.anyilanxin.kunpeng.protocol.business.impl.RecordMetadata;
import com.anyilanxin.kunpeng.protocol.business.impl.eventlog.RecordAppendEntryFactory;
import com.anyilanxin.kunpeng.protocol.business.record.RecordType;
import com.anyilanxin.kunpeng.protocol.business.record.command.job.JobLifeCycle;
import com.anyilanxin.kunpeng.scheduler.Actor;
import java.util.HashMap;
import java.util.Map;
import java.util.function.BiConsumer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 推送失败回退：job 流推送未送达时向其所属分区追加 JOB/WITHDRAW 命令，让 job 立即回待激活（可被轮询或其他流抢占）， 不必等满一个 deadline 周期。
 *
 * <p>独立 actor 而非并入协调器，让错误处理与推送路径解耦。各分区写入器由 transition 装配在 leader 期注册； 推送失败发生在本节点失去分区 leader
 * 之后时无可用写入器，job 保持已激活直至到期扫描兜底。
 *
 * @author zxuanhong
 * @since 2026.9.0
 */
public final class PushFailureFallback extends Actor implements BiConsumer<StreamPush, Throwable> {
  private static final Logger LOG = LoggerFactory.getLogger(PushFailureFallback.class);

  /** actor 线程独占：分区 → 日志写入器（leader 期注册） */
  private final Map<Integer, EventLogWriter> partitionWriters = new HashMap<>();

  @Override
  public String getName() {
    return "job-stream-fallback";
  }

  /** leader 装配时注册分区写入器 */
  public void registerWriter(final int partitionId, final EventLogWriter writer) {
    actor.run(() -> partitionWriters.put(partitionId, writer));
  }

  /** 让出 leader/分区移除时摘除写入器 */
  public void removeWriter(final int partitionId) {
    actor.run(() -> partitionWriters.remove(partitionId));
  }

  @Override
  public void accept(final StreamPush push, final Throwable failure) {
    actor.run(
        () -> {
          final var writer = partitionWriters.get(push.partitionId());
          if (writer == null) {
            LOG.warn("分区 {} 无写入器, 无法处理 job {} 的推送失败(选举期间可能发生)", push.partitionId(), push.jobKey());
            return;
          }
          final var record = push.record();
          record.setDueDate(push.deadline());
          final var metadata = new RecordMetadata();
          metadata
              .recordType(RecordType.COMMAND)
              .valueType(ValueType.JOB)
              .valueLifeCycle(JobLifeCycle.WITHDRAW)
              .requestId(-1);
          final var result =
              writer.tryAppend(
                  WriteContext.INTER_PARTITION,
                  RecordAppendEntryFactory.of(push.jobKey(), metadata, record));
          if (!(result instanceof AppendResult.Appended)) {
            LOG.warn("job {} 的 WITHDRAW 回退写入被拒: {}", push.jobKey(), result);
          }
        });
  }
}
