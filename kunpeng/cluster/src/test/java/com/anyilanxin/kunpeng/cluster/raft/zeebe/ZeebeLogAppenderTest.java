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
package com.anyilanxin.kunpeng.cluster.raft.zeebe;

import static org.assertj.core.api.Assertions.assertThat;

import com.google.common.base.Stopwatch;
import com.anyilanxin.kunpeng.cluster.raft.storage.log.IndexedRaftLogEntry;
import com.anyilanxin.kunpeng.cluster.raft.zeebe.util.TestAppender;
import com.anyilanxin.kunpeng.cluster.raft.zeebe.util.ZeebeTestHelper;
import com.anyilanxin.kunpeng.cluster.raft.zeebe.util.ZeebeTestNode;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.nio.ByteBuffer;
import java.util.Collections;
import java.util.Set;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.jupiter.api.AutoClose;
import org.junit.rules.TemporaryFolder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Tests the {@link com.anyilanxin.kunpeng.cluster.raft.roles.LeaderRole} implementation of {@link ZeebeLogAppender} */
public class ZeebeLogAppenderTest {
  @Rule public final TemporaryFolder temporaryFolder = new TemporaryFolder();

  @AutoClose MeterRegistry meterRegistry = new SimpleMeterRegistry();
  private final Logger logger = LoggerFactory.getLogger(getClass());
  private final Stopwatch stopwatch = Stopwatch.createUnstarted();
  private final TestAppender appenderListener = new TestAppender();

  private ZeebeTestNode node;
  private ZeebeTestHelper helper;

  @Before
  public void setUp() throws Exception {
    node = new ZeebeTestNode(0, temporaryFolder.newFolder("0"), meterRegistry);

    final Set<ZeebeTestNode> nodes = Collections.singleton(node);
    helper = new ZeebeTestHelper(nodes);

    node.start(nodes).join();
    stopwatch.start();
  }

  @After
  public void tearDown() {
    if (stopwatch.isRunning()) {
      stopwatch.stop();
    }

    logger.info("Test run time: {}", stopwatch.toString());
    node.stop().join();
  }

  @Test
  public void shouldNotifyOnWrite() {
    // when
    append();

    // then
    final IndexedRaftLogEntry appended = appenderListener.pollWritten();
    assertThat(appended).isNotNull();
    assertThat(appenderListener.getErrors().size()).isEqualTo(0);
  }

  @Test
  public void shouldNotifyOnCommit() {
    // when
    append();

    // then
    final var committed = appenderListener.pollCommitted();
    assertThat(committed).isNotNull();
    assertThat(appenderListener.getErrors().size()).isEqualTo(0);
  }

  @Test
  public void shouldNotifyOnError() {
    // given - a message that cannot be appended because it's too large
    final ByteBuffer data = ByteBuffer.allocate(2048);

    // when
    append(data);

    // then
    final Throwable error = appenderListener.pollError();
    assertThat(error).isNotNull();
    assertThat(appenderListener.getWritten().size()).isEqualTo(0L);
    assertThat(appenderListener.getCommitted().size()).isEqualTo(0L);
  }

  private void append() {
    append(ByteBuffer.allocate(Integer.BYTES).putInt(0, 1));
  }

  private void append(final ByteBuffer data) {
    final ZeebeLogAppender appender = helper.awaitLeaderAppender(1);
    appender.appendEntry(0, 0, data, appenderListener);
  }
}
