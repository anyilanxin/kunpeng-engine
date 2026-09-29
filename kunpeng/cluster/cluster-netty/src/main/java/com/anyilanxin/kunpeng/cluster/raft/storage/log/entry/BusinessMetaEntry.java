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
package com.anyilanxin.kunpeng.cluster.raft.storage.log.entry;

import com.anyilanxin.kunpeng.cluster.raft.storage.serializer.RaftEntrySerializer;
import com.anyilanxin.kunpeng.cluster.raft.storage.serializer.RaftEntrySerializer.SerializedBufferWriterAdapter;
import com.anyilanxin.kunpeng.structpack.buffer.BufferWriter;
import java.util.Map;
import java.util.Objects;

/**
 * 分区业务元数据条目：与 {@link ConfigurationEntry} 同类的内核专用条目类型， 记录分区业务元数据键值对（key/value，具体内容由调用方决定）的全量快照，经
 * raft 复制提交后整体生效。
 *
 * <p>Map 形态由类型保证单条目内 key 不重复；应用语义为整体覆盖（先清空再添加）， 未携带的 key 即视为删除。非
 * ApplicationEntry：业务日志消费端（EventStore/业务处理器）天然跳过； {@link RaftLogEntry#getLowestAsqn()} 对本类型返回
 * empty，不占用业务 asqn 序列。
 */
public record BusinessMetaEntry(Map<String, String> entries) implements RaftEntry {

  /** 单条目最大键值对数：SBE group numInGroup 为 uint8，上限 254（生成编码器 itemsCount 的硬约束）。 */
  public static final int MAX_ITEMS = 254;

  public BusinessMetaEntry {
    Objects.requireNonNull(entries, "entries cannot be null");
    if (entries.size() > MAX_ITEMS) {
      throw new IllegalArgumentException(
          "too many business meta entries: " + entries.size() + ", max " + MAX_ITEMS);
    }
    entries = Map.copyOf(entries);
  }

  @Override
  public BufferWriter toSerializable(final long term, final RaftEntrySerializer serializer) {
    return new SerializedBufferWriterAdapter(
        () -> serializer.getBusinessMetaEntrySerializedLength(this),
        (buffer, offset) -> serializer.writeBusinessMetaEntry(term, this, buffer, offset));
  }
}
