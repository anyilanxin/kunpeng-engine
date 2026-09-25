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
package com.anyilanxin.kunpeng.cluster.raft.storage.system;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/** {@link BusinessMetaStore} 文件读写测试。 */
class BusinessMetaStoreTest {

  @TempDir Path directory;

  private BusinessMetaStore newStore() throws IOException {
    // 直接以目录构造：store 只依赖目录与文件名前缀，RaftStorage 接线在后续任务
    return BusinessMetaStore.open(directory.toFile(), "raft-partition-test-1");
  }

  @Test
  void shouldCreateFileAndLoadEmpty() throws IOException {
    try (final BusinessMetaStore store = newStore()) {
      assertTrue(store.load().isEmpty());
      assertTrue(Files.exists(directory.resolve("raft-partition-test-1.state")));
    }
  }

  @Test
  void shouldStoreAndReloadState() throws IOException {
    try (final BusinessMetaStore store = newStore()) {
      store.store(33, 5, Map.of("sourceId", "9"));
    }
    try (final BusinessMetaStore reopened = newStore()) {
      final BusinessMetaStore.BusinessMetaState state = reopened.load().orElseThrow();
      assertEquals(33, state.index());
      assertEquals(5, state.term());
      assertEquals("9", state.entries().get("sourceId"));
    }
  }

  @Test
  void shouldTreatCorruptedFileAsEmpty() throws IOException {
    try (final BusinessMetaStore store = newStore()) {
      store.store(1, 1, Map.of("sourceId", "2"));
    }
    // 投影损坏按空处理：可由日志重放或快照恢复重建，不视为致命错误
    Files.write(directory.resolve("raft-partition-test-1.state"), new byte[] {1, 2, 3});
    try (final BusinessMetaStore reopened = newStore()) {
      assertTrue(reopened.load().isEmpty());
    }
  }

  @Test
  void shouldTruncateWhenStoringShorterContent() throws IOException {
    try (final BusinessMetaStore store = newStore()) {
      store.store(1, 1, Map.of("sourceId", "2", "anotherKey", "anotherLongValue"));
      store.store(2, 1, Map.of("k", "v"));
    }
    try (final BusinessMetaStore reopened = newStore()) {
      final BusinessMetaStore.BusinessMetaState state = reopened.load().orElseThrow();
      assertEquals(2, state.index());
      assertEquals(1, state.entries().size());
      assertEquals("v", state.entries().get("k"));
    }
  }

  @Test
  void shouldTreatTruncatedBodyAsEmpty() throws IOException {
    try (final BusinessMetaStore store = newStore()) {
      store.store(1, 1, Map.of("sourceId", "2"));
    }
    final Path file = directory.resolve("raft-partition-test-1.state");
    final byte[] full = Files.readAllBytes(file);
    Files.write(file, java.util.Arrays.copyOf(full, full.length - 3));
    try (final BusinessMetaStore reopened = newStore()) {
      assertTrue(reopened.load().isEmpty());
    }
  }
}
