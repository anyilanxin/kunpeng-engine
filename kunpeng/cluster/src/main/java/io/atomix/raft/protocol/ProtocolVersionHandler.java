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
package io.atomix.raft.protocol;

import io.atomix.cluster.MemberId;
import java.util.List;

/** 把各受支持线上版本的追加请求统一转换为 {@link InternalAppendRequest}。 */
public final class ProtocolVersionHandler {

  private ProtocolVersionHandler() {
    // 工具类，禁止实例化
  }

  /** 转换旧版追加请求。 */
  public static InternalAppendRequest transform(final AppendRequest request) {
    return assemble(
        request.term(),
        request.leader(),
        request.prevLogIndex(),
        request.prevLogTerm(),
        request.commitIndex(),
        request.entries());
  }

  /** 转换带版本号的追加请求。 */
  public static InternalAppendRequest transform(final VersionedAppendRequest request) {
    return assemble(
        request.term(),
        request.leader(),
        request.prevLogIndex(),
        request.prevLogTerm(),
        request.commitIndex(),
        request.entries());
  }

  /** 将各版本共有的字段组装成内部统一形态。 */
  private static InternalAppendRequest assemble(
      final long term,
      final MemberId leader,
      final long prevLogIndex,
      final long prevLogTerm,
      final long commitIndex,
      final List<? extends ReplicatableRaftRecord> entries) {
    return new InternalAppendRequest(term, leader, prevLogIndex, prevLogTerm, commitIndex,
        entries);
  }
}
