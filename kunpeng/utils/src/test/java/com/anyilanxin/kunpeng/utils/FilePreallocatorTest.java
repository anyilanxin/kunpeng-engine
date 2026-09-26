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
package com.anyilanxin.kunpeng.utils;

import static java.nio.charset.StandardCharsets.UTF_8;
import static java.nio.file.StandardOpenOption.READ;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/** {@link FilePreallocator} 行为测试：平台最优路径与零填充兜底的对外表现必须一致。 */
class FilePreallocatorTest {

  @TempDir Path tempDir;

  @Test
  void shouldExtendFreshFileToTargetSizeReadingZeros() throws IOException {
    final Path file = tempDir.resolve("segment.log");
    Files.createFile(file);
    final long target = 64 * 1024 + 3; // 非块对齐大小
    FilePreallocator.preallocate(file, target);
    assertThat(Files.size(file)).isEqualTo(target);
    assertRangeAllZero(file, 0, target);
  }

  @Test
  void shouldPreserveExistingContentWhileExtending() throws IOException {
    final Path file = tempDir.resolve("existing.log");
    Files.write(file, "hello".getBytes(UTF_8));
    FilePreallocator.preallocate(file, 8 * 1024);
    assertThat(Files.size(file)).isEqualTo(8 * 1024);
    assertThat(readRange(file, 0, 5)).isEqualTo("hello".getBytes(UTF_8));
    assertRangeAllZero(file, 5, 8 * 1024);
  }

  @Test
  void shouldBeNoOpWhenFileAlreadyReachesTarget() throws IOException {
    final Path file = tempDir.resolve("grown.log");
    final byte[] pattern = "0123456789".getBytes(UTF_8);
    Files.write(file, pattern);
    FilePreallocator.preallocate(file, 4);
    assertThat(Files.size(file)).isEqualTo(pattern.length);
    assertThat(readRange(file, 0, pattern.length)).isEqualTo(pattern);
  }

  @Test
  void shouldBeIdempotentWhenCalledTwice() throws IOException {
    final Path file = tempDir.resolve("twice.log");
    Files.createFile(file);
    FilePreallocator.preallocate(file, 16 * 1024);
    FilePreallocator.preallocate(file, 16 * 1024);
    assertThat(Files.size(file)).isEqualTo(16 * 1024);
    assertRangeAllZero(file, 0, 16 * 1024);
  }

  @Test
  void shouldRejectNonPositiveSize() throws IOException {
    final Path file = tempDir.resolve("tiny.log");
    Files.createFile(file);
    assertThatThrownBy(() -> FilePreallocator.preallocate(file, 0))
        .isInstanceOf(IllegalArgumentException.class);
    assertThatThrownBy(() -> FilePreallocator.preallocate(file, -1))
        .isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  void shouldFailOnMissingFile() {
    assertThatThrownBy(() -> FilePreallocator.preallocate(tempDir.resolve("absent.log"), 1024))
        .isInstanceOf(IOException.class);
  }

  private static byte[] readRange(final Path file, final long from, final long to)
      throws IOException {
    final var buffer = ByteBuffer.allocate((int) (to - from));
    try (FileChannel channel = FileChannel.open(file, READ)) {
      channel.read(buffer, from);
    }
    return buffer.array();
  }

  private static void assertRangeAllZero(final Path file, final long from, final long to)
      throws IOException {
    final long length = to - from;
    long position = from;
    final var buffer = ByteBuffer.allocate(8 * 1024);
    try (FileChannel channel = FileChannel.open(file, READ)) {
      while (position < to) {
        buffer.clear();
        channel.read(buffer, position);
        buffer.flip();
        for (int i = 0; i < buffer.remaining(); i++) {
          assertThat(buffer.get(i))
              .as("byte at %d should be zero", position + i)
              .isZero();
        }
        position += buffer.remaining();
      }
    }
    assertThat(position - from).isEqualTo(length);
  }
}
