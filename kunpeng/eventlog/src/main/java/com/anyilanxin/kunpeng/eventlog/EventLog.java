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
package com.anyilanxin.kunpeng.eventlog;

/**
 * 事件日志门面：一个分区一个实例，聚合追加（定序+流控+批帧）、拉取读、提交通知。
 *
 * <p>恢复语义：打开时通过存储 seekToEnd 找回 lastPosition，新写入从 lastPosition+1 续号。
 */
public interface EventLog extends AutoCloseable {

  static EventLogBuilder builder() {
    return new com.anyilanxin.kunpeng.eventlog.impl.EventLogBuilderImpl();
  }

  int getPartitionId();

  String getLogName();

  EventLogReader newReader();

  BatchEntryReader newBatchReader();

  EventLogWriter newWriter();

  LogFlowControl getFlowControl();

  /** 最后提交 position（未提交过返回 0） */
  long getLastCommittedPosition();

  void registerRecordAvailableListener(RecordAvailableListener listener);

  void removeRecordAvailableListener(RecordAvailableListener listener);

  @Override
  void close();
}
