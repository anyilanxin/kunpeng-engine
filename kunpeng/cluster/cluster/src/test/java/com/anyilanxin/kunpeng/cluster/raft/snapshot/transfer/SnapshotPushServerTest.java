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
package com.anyilanxin.kunpeng.cluster.raft.snapshot.transfer;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

/** F-04：info 批与"空文件内容批"形状相同（单 chunk、双 0 长度），只能靠 chunkName 区分。 */
final class SnapshotPushServerTest {

  @Test
  void shouldDetectInfoBatchOnlyForValidSnapshotIdChunkName() {
    // 内容分片名固定带 '@' 偏移（见 FileSnapshotChunkReader），不会误判为 info
    assertThat(SnapshotPushServer.isInfoChunkName("6c6561646572-100-5")).isTrue();
    assertThat(SnapshotPushServer.isInfoChunkName("data.bin@0")).isFalse();
    // 即使 '@' 前的部分恰好是合法镜像 id，含 '@' 即为内容分片名，不能当 info 批
    assertThat(SnapshotPushServer.isInfoChunkName("6c6561646572-100-5@0")).isFalse();
    assertThat(SnapshotPushServer.isInfoChunkName("not-a-valid-id")).isFalse();
    assertThat(SnapshotPushServer.isInfoChunkName(null)).isFalse();
  }
}
