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
package com.anyilanxin.kunpeng.cluster.raft.logentry;

import com.anyilanxin.kunpeng.cluster.raft.logentry.util.TestAppender;
import com.anyilanxin.kunpeng.cluster.raft.logentry.util.ZeebeTestHelper;
import com.anyilanxin.kunpeng.cluster.raft.logentry.util.ZeebeTestNode;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.jupiter.api.AutoClose;
import org.junit.rules.TemporaryFolder;

import java.nio.ByteBuffer;
import java.util.Collections;

import static org.assertj.core.api.Assertions.assertThat;

/** 验证 {@link com.anyilanxin.kunpeng.cluster.raft.partition.RaftPartition} 的拍摄与关闭前快照。 */
public class RaftPartitionSnapshotIntegrationTest {

  @Rule public final TemporaryFolder temporaryFolder = new TemporaryFolder();

  @AutoClose MeterRegistry meterRegistry = new SimpleMeterRegistry();

  private ZeebeTestNode node;
  private ZeebeTestHelper helper;
  private final TestAppender appenderListener = new TestAppender();

  @Before
  public void setUp() throws Exception {
    node = new ZeebeTestNode(0, temporaryFolder.newFolder("0"), meterRegistry);
    helper = new ZeebeTestHelper(Collections.singleton(node));
    node.start(Collections.singleton(node)).join();
  }

  @After
  public void tearDown() {
    node.stop().join();
  }

  @Test
  public void takeSnapshotPersistsSnapshot() {
    // given：追加并提交一条日志，推进 commit index
    final var appender = helper.awaitLeaderAppender(1);
    appender.appendEntry(0, 0, ByteBuffer.allocate(Integer.BYTES).putInt(0, 1), appenderListener);
    final long commitIndex = appenderListener.pollCommitted();
    assertThat(commitIndex).isGreaterThan(0);

    // when
    node.getPartition(1).takeSnapshot().join();

    // then
    final var store = node.getPartitionServer(1).getPersistedSnapshotStore();
    assertThat(store.getLatestSnapshot()).isPresent();
    assertThat(store.getLatestSnapshot().get().getIndex()).isEqualTo(commitIndex);
  }

  @Test
  public void closeTakesFinalSnapshot() {
    // given：追加并提交一条日志
    final var appender = helper.awaitLeaderAppender(1);
    appender.appendEntry(0, 0, ByteBuffer.allocate(Integer.BYTES).putInt(0, 1), appenderListener);
    appenderListener.pollCommitted();

    // when：强制关闭（应在此之前拍一次快照）
    node.getPartition(1).close().join();

    // then：最新快照索引已推进到关闭前的 commit index
    final var store = node.getPartitionServer(1).getPersistedSnapshotStore();
    assertThat(store.getCurrentSnapshotIndex()).isGreaterThan(0);
  }
}
