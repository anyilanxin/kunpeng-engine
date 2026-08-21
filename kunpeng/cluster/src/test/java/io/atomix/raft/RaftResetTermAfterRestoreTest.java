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
package io.atomix.raft;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.Rule;
import org.junit.Test;

/**
 * 覆盖恢复（restore）场景下的任期恢复：元数据文件丢失时，节点应从日志最后一条条目恢复任期，
 * 对应上游回归问题 camunda/issues/14509。
 */
public class RaftResetTermAfterRestoreTest {
  @Rule public RaftRule raftRule = RaftRule.withBootstrappedNodes(1);

  /** 删除分区目录中唯一的元数据文件，模拟恢复之后元数据存储为空的状态。 */
  private static void wipeMetaStore(final RaftServer server) throws IOException {
    final Path partitionDir = server.getContext().getStorage().directory().toPath();
    try (final var files = Files.list(partitionDir)) {
      final Path meta =
          files.filter(f -> f.getFileName().toString().endsWith("meta"))
              .findFirst()
              .orElseThrow(() -> new AssertionError("分区目录中未找到 meta 文件: " + partitionDir));
      Files.delete(meta);
    }
  }

  @Test
  public void termIsRebuiltFromLastLogEntryWhenMetaStoreIsGone() throws Exception {
    // 准备：写入一条日志并记录关闭前的任期（单节点场景任期应为 1）
    raftRule.appendEntries(1);
    final var node = raftRule.getServers().stream().findFirst().orElseThrow();
    final var nodeName = node.cluster().getLocalMember().memberId().id();
    final long termBeforeShutdown = node.getTerm();
    raftRule.shutdownServer(node);
    assertThat(termBeforeShutdown).as("关闭前任期应为 1").isEqualTo(1);

    // 执行：删除元数据文件后重新加入集群
    wipeMetaStore(node);
    raftRule.joinCluster(nodeName);

    // 验证：任期依据日志最后一条条目恢复，重新选举后递增为 2
    assertThat(raftRule.getLeader().orElseThrow().getTerm())
        .as("应从日志最后一条条目恢复任期")
        .isEqualTo(2L);
  }
}
