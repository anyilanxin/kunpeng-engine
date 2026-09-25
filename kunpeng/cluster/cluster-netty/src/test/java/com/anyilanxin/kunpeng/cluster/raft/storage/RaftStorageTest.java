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
package com.anyilanxin.kunpeng.cluster.raft.storage;

import static org.assertj.core.api.Assertions.assertThat;

import com.anyilanxin.kunpeng.cluster.raft.storage.log.RaftLogFlusher;
import com.anyilanxin.kunpeng.utils.FileUtil;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import org.junit.After;
import org.junit.Test;
import org.junit.jupiter.api.AutoClose;

/** Raft storage test. */
public class RaftStorageTest {

  private static final Path PATH = Paths.get("target/test-logs/");
  @AutoClose private final MeterRegistry meterRegistry = new SimpleMeterRegistry();

  @Test
  public void testDefaultConfiguration() {
    final RaftStorage storage = RaftStorage.builder(meterRegistry).build();
    assertThat(storage.prefix()).isEqualTo("atomix");
    assertThat(storage.directory()).isEqualTo(new File(System.getProperty("user.dir")));
  }

  @Test
  public void testCustomConfiguration() {
    final RaftStorage storage =
        RaftStorage.builder(meterRegistry)
            .withPrefix("foo")
            .withDirectory(new File(PATH.toFile(), "foo"))
            .withMaxSegmentSize(1024 * 1024)
            .withFreeDiskSpace(100)
            .withFlusherFactory(RaftLogFlusher.Factory::noop)
            .build();
    assertThat(storage.prefix()).isEqualTo("foo");
    assertThat(storage.directory()).isEqualTo(new File(PATH.toFile(), "foo"));
  }

  @Test
  public void canAcquireLockOnEmptyDirectory() {
    // given empty directory in PATH

    // when
    final RaftStorage storage1 =
        RaftStorage.builder(meterRegistry).withDirectory(PATH.toFile()).withPrefix("test").build();

    // then
    assertThat(storage1.lock("a")).isTrue();
  }

  @Test
  public void cannotLockAlreadyLockedDirectory() {
    // given
    final RaftStorage storage1 =
        RaftStorage.builder(meterRegistry).withDirectory(PATH.toFile()).withPrefix("test").build();
    storage1.lock("a");

    // when
    final RaftStorage storage2 =
        RaftStorage.builder(meterRegistry).withDirectory(PATH.toFile()).withPrefix("test").build();

    // then
    assertThat(storage2.lock("b")).isFalse();
  }

  @Test
  public void canAcquireLockOnDirectoryLockedBySameNode() {
    // given
    final RaftStorage storage1 =
        RaftStorage.builder(meterRegistry).withDirectory(PATH.toFile()).withPrefix("test").build();
    storage1.lock("a");

    // when
    final RaftStorage storage3 =
        RaftStorage.builder(meterRegistry).withDirectory(PATH.toFile()).withPrefix("test").build();

    // then
    assertThat(storage3.lock("a")).isTrue();
  }

  @After
  public void cleanupStorage() throws IOException {
    FileUtil.deleteTreeIfExists(PATH);
  }
}
