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
package com.anyilanxin.kunpeng.cluster.raft.storage.log.entry;

import com.anyilanxin.kunpeng.cluster.cluster.PartitionId;
import com.anyilanxin.kunpeng.cluster.raft.storage.serializer.RaftEntrySerializer;
import com.anyilanxin.kunpeng.cluster.raft.storage.serializer.RaftEntrySerializer.SerializedBufferWriterAdapter;
import com.anyilanxin.kunpeng.structpack.buffer.BufferWriter;
import java.util.Objects;

/**
 * 合并记录条目：内核专用条目（与 {@link BusinessMetaEntry} 同类）， 记录"哪个分区的数据合并进了本分区"——目标分区 leader
 * 每完成一次跨分区合并（{@code RaftPartition#mergeReceivedSnapshot}）追加一条。
 *
 * <p>双重目的：
 *
 * <ul>
 *   <li>推进日志水位：合并状态经镜像通道落地（不走日志复制），若不追加条目， leader 的日志 index 停留在合并前水位，易主后可能选出日志
 *       "看似不落后"、实际未经历合并的 follower 当 leader；在线 follower 经正常复制跟上水位， 离线副本重新上线后发现日志偏离过远，走
 *       raft 标准的镜像安装追赶；
 *   <li>留痕：条目内容即合并审计记录（源分区身份），follower 侧无需反应。
 * </ul>
 *
 * <p>非 ApplicationEntry：业务日志消费端天然跳过，不路由到业务侧； {@link RaftLogEntry#getLowestAsqn()} 对本类型返回
 * empty，不占用业务 asqn 序列。
 */
public record MergeRecordEntry(PartitionId sourcePartition) implements RaftEntry {

  public MergeRecordEntry {
    Objects.requireNonNull(sourcePartition, "sourcePartition cannot be null");
  }

  @Override
  public BufferWriter toSerializable(final long term, final RaftEntrySerializer serializer) {
    return new SerializedBufferWriterAdapter(
        () -> serializer.getMergeRecordEntrySerializedLength(this),
        (buffer, offset) -> serializer.writeMergeRecordEntry(term, this, buffer, offset));
  }
}
