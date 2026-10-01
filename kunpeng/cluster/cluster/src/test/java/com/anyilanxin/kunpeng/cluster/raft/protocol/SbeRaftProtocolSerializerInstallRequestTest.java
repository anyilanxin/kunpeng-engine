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
package com.anyilanxin.kunpeng.cluster.raft.protocol;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;

import com.anyilanxin.kunpeng.cluster.cluster.MemberId;
import com.anyilanxin.kunpeng.cluster.raft.snapshot.SnapshotChunk;
import com.anyilanxin.kunpeng.cluster.raft.snapshot.SnapshotChunkBatch;
import com.anyilanxin.kunpeng.cluster.raft.snapshot.SnapshotTransferCodec;
import com.anyilanxin.kunpeng.cluster.raft.snapshot.TransferKind;
import com.anyilanxin.kunpeng.cluster.raft.snapshot.impl.SnapshotChunkImpl;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * {@link SbeRaftProtocolSerializer} 对 InstallRequest 变长字段链的回归测试。
 *
 * <p>覆盖事故场景：最后一批 install 请求的 {@code nextChunkId} 为 null 时，编码写入 4 字节零长度头；
 * 解码侧若不消费该长度头，后续 {@code data} 会错读到这 4 个零字节而丢失，follower 端表现为
 * "Failed to parse request data" 并无限重发快照。
 *
 * @author zxuanhong
 * @since 2026.9.0
 */
final class SbeRaftProtocolSerializerInstallRequestTest {

  private final SbeRaftProtocolSerializer serializer = new SbeRaftProtocolSerializer();

  @Test
  void shouldRoundTripLastBatchWithNullNextChunkId() {
    final byte[] content = new byte[1_326_109];
    new SecureRandom().nextBytes(content);
    final byte[] data =
        SnapshotTransferCodec.encodeChunkBatch(
            new SnapshotChunkBatch(
                TransferKind.FILE_CHUNKS, List.of(wholeFile("000013.sst@0", content)), false));

    final InstallRequest decoded =
        serializer.decode(encodeRequest(data, null, true, "000013.sst@0"));

    assertNull(decoded.nextChunkId(), "nextChunkId 应保持 null");
    assertEquals(data.length, decoded.data().remaining(), "data 长度应完整保留");
    final byte[] decodedData = new byte[data.length];
    decoded.data().get(decodedData);
    assertArrayEquals(data, decodedData);

    final SnapshotChunkBatch batch = SnapshotTransferCodec.decodeChunkBatch(decodedData);
    assertEquals(1, batch.chunks().size());
    assertEquals("000013.sst@0", batch.chunks().get(0).getChunkName());
    final byte[] decodedContent = new byte[content.length];
    batch.chunks().get(0).getContent().get(decodedContent);
    assertArrayEquals(content, decodedContent);
  }

  @Test
  void shouldRoundTripIntermediateBatchWithNextChunkId() {
    final byte[] data = new byte[1024];
    new SecureRandom().nextBytes(data);

    final InstallRequest decoded =
        serializer.decode(encodeRequest(data, "000013.sst@1048576", false, "000013.sst@0"));

    assertEquals(
        "000013.sst@1048576",
        StandardCharsets.UTF_8.decode(decoded.nextChunkId()).toString(),
        "nextChunkId 应完整保留");
    assertEquals(
        "000013.sst@0", StandardCharsets.UTF_8.decode(decoded.chunkId()).toString());
    final byte[] decodedData = new byte[data.length];
    decoded.data().get(decodedData);
    assertArrayEquals(data, decodedData);
    assertEquals(5995, decoded.index());
    assertFalse(decoded.complete());
  }

  private static SnapshotChunk wholeFile(final String name, final byte[] content) {
    return new SnapshotChunkImpl(name, 42L, content.length, 7L, ByteBuffer.wrap(content), 0L);
  }

  private byte[] encodeRequest(
      final byte[] data, final String nextChunkId, final boolean complete, final String chunkId) {
    return serializer.encode(
        InstallRequest.builder()
            .withCurrentTerm(1)
            .withLeader(MemberId.from("broker@node01"))
            .withIndex(5995)
            .withTerm(1)
            .withVersion(1)
            .withData(ByteBuffer.wrap(data))
            .withChunkId(ByteBuffer.wrap(chunkId.getBytes(StandardCharsets.UTF_8)))
            .withNextChunkId(
                nextChunkId == null
                    ? null
                    : ByteBuffer.wrap(nextChunkId.getBytes(StandardCharsets.UTF_8)))
            .withInitial(false)
            .withComplete(complete)
            .build());
  }
}
