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

import java.io.IOException;
import java.lang.foreign.Arena;
import java.lang.foreign.FunctionDescriptor;
import java.lang.foreign.Linker;
import java.lang.foreign.MemorySegment;
import java.lang.foreign.SymbolLookup;
import java.lang.foreign.ValueLayout;
import java.lang.invoke.MethodHandle;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 文件空间预分配工具：把文件扩展到目标大小并预留磁盘空间，已有内容不被改写、未写区间保证读取为零。
 *
 * <p>按平台选择最优策略：POSIX 系统经 FFM 直连 {@code posix_fallocate}，以 unwritten extents 完成物理预留 ——只有元数据
 * I/O、无数据写入，且磁盘满会在预分配阶段前置暴露；平台或文件系统不支持时回退到纯 Java 零填充（逐块写入并刷盘）作为兜底默认方案。native 路径全部经 {@code
 * openat}/{@code posix_fallocate}/{@code close} 完成，不依赖任何 JDK 内部 API 与反射；建议 JVM 带 {@code
 * --enable-native-access=ALL-UNNAMED}， 缺失时自动退化为零填充。
 *
 * @author zxuanhong
 * @since 2026.9.0
 */
public final class FilePreallocator {

  private static final Logger LOGGER = LoggerFactory.getLogger(FilePreallocator.class);

  /** 零填充兜底路径每次写入的块大小。 */
  private static final int FILL_CHUNK_SIZE = 4 * 1024 * 1024;

  /** “平台/文件系统不支持 fallocate”类 errno，出现时静默回退零填充；其余 errno 视为真实故障上抛。 */
  private static final Set<Integer> UNSUPPORTED_ERRNOS = unsupportedErrnos();

  /** 实际生效路径只在首次各打一条 INFO（进程内一次性，避免频繁建段刷屏），后续降为 DEBUG。 */
  private static final AtomicBoolean NATIVE_LOGGED = new AtomicBoolean();

  private static final AtomicBoolean FALLBACK_LOGGED = new AtomicBoolean();

  private FilePreallocator() {}

  /**
   * 把 {@code file} 扩展到 {@code size} 字节并预留磁盘空间。
   *
   * @param file 目标文件（须已存在）
   * @param size 目标大小，必须为正
   * @throws IOException 预分配失败时抛出
   */
  public static void preallocate(final Path file, final long size) throws IOException {
    if (size <= 0) {
      throw new IllegalArgumentException("size must be positive: " + size);
    }
    if (Native.AVAILABLE) {
      final int errno = Native.posixFallocate(file, size);
      if (errno == 0) {
        if (NATIVE_LOGGED.compareAndSet(false, true)) {
          LOGGER.info("文件预分配生效路径：posix_fallocate（FFM 本地调用，物理预留）");
        } else {
          LOGGER.info("posix_fallocate 预分配完成：{}（{} 字节）", file, size);
        }
        return;
      }
      if (!UNSUPPORTED_ERRNOS.contains(errno)) {
        throw new IOException(
            "posix_fallocate failed with errno %d on file %s".formatted(errno, file));
      }
      if (FALLBACK_LOGGED.compareAndSet(false, true)) {
        LOGGER.info("文件预分配生效路径：零填充兜底（posix_fallocate 不可用，errno={}，首个文件 {}）", errno, file);
      } else {
        LOGGER.info("posix_fallocate 不受支持（errno={}），回退零填充预分配：{}", errno, file);
      }
    }
    fillZeros(file, size);
  }

  private static Set<Integer> unsupportedErrnos() {
    final String os = System.getProperty("os.name", "").toLowerCase();
    if (os.contains("linux")) {
      // ENOSYS=38（内核无 fallocate）、EOPNOTSUPP=ENOTSUP=95（文件系统不支持）
      return Set.of(38, 95);
    }
    if (os.contains("mac") || os.contains("darwin")) {
      // ENOTSUP=45、ENOSYS=78、EOPNOTSUPP=102（APFS/HFS+ 不实现 posix_fallocate）
      return Set.of(45, 78, 102);
    }
    return Set.of();
  }

  /** 兜底默认方案：以零块把文件从当前大小填充到目标大小并刷盘。 */
  private static void fillZeros(final Path file, final long size) throws IOException {
    try (final FileChannel channel =
        FileChannel.open(file, StandardOpenOption.READ, StandardOpenOption.WRITE)) {
      long position = channel.size();
      final ByteBuffer chunk = ByteBuffer.allocate((int) Math.min(FILL_CHUNK_SIZE, size));
      while (position < size) {
        final int writeLength = (int) Math.min(chunk.capacity(), size - position);
        chunk.clear().limit(writeLength);
        final int written = channel.write(chunk, position);
        if (written <= 0) {
          throw new IOException(
              "Failed to pre-allocate file %s: no progress at position %d of %d"
                  .formatted(file, position, size));
        }
        position += written;
      }
      channel.force(true);
    }
  }

  /** posix_fallocate 的 FFM 绑定：openat 取 fd → fallocate → close，全程不经过 JDK 内部 API。 */
  private static final class Native {
    private static final int AT_FDCWD = atFdcwd();
    private static final int O_RDWR = 2;
    private static final int FALLBACK = -1;

    private static int atFdcwd() {
      final String os = System.getProperty("os.name", "").toLowerCase();
      // Linux 与 macOS 的 AT_FDCWD 值不同（-100/-2）；未知平台沿用 Linux 值，打开失败自然回退
      return os.contains("mac") || os.contains("darwin") ? -2 : -100;
    }

    private static final boolean AVAILABLE;
    private static final MethodHandle OPENAT;
    private static final MethodHandle FALLOCATE;
    private static final MethodHandle CLOSE;

    static {
      final Linker linker = Linker.nativeLinker();
      final SymbolLookup lookup = linker.defaultLookup();
      MethodHandle openat = null;
      MethodHandle fallocate = null;
      MethodHandle close = null;
      try {
        openat =
            linker.downcallHandle(
                lookup.find("openat").orElseThrow(),
                FunctionDescriptor.of(
                    ValueLayout.JAVA_INT,
                    ValueLayout.JAVA_INT,
                    ValueLayout.ADDRESS,
                    ValueLayout.JAVA_INT,
                    ValueLayout.JAVA_INT));
        fallocate =
            linker.downcallHandle(
                lookup.find("posix_fallocate").orElseThrow(),
                FunctionDescriptor.of(
                    ValueLayout.JAVA_INT,
                    ValueLayout.JAVA_INT,
                    ValueLayout.JAVA_LONG,
                    ValueLayout.JAVA_LONG));
        close =
            linker.downcallHandle(
                lookup.find("close").orElseThrow(),
                FunctionDescriptor.of(ValueLayout.JAVA_INT, ValueLayout.JAVA_INT));
      } catch (final Throwable error) {
        LOGGER.info("posix_fallocate 本地绑定不可用（{}），文件预分配将使用零填充回退", error.getMessage());
      }
      OPENAT = openat;
      FALLOCATE = fallocate;
      CLOSE = close;
      AVAILABLE = openat != null && fallocate != null && close != null;
    }

    private Native() {}

    /**
     * 对 {@code file} 执行 {@code posix_fallocate(fd, 0, size)}。
     *
     * @return 0 表示成功；正值为本机 errno；{@code -1} 表示本地路径不可用（打开失败或调用异常），应回退
     */
    static int posixFallocate(final Path file, final long size) {
      try (final Arena arena = Arena.ofConfined()) {
        final MemorySegment cPath = arena.allocateFrom(file.toAbsolutePath().toString());
        final int fd = (int) OPENAT.invoke(AT_FDCWD, cPath, O_RDWR, 0600);
        if (fd < 0) {
          // 打开失败的真实原因由零填充路径的 Java IO 异常给出
          return FALLBACK;
        }
        try {
          return (int) FALLOCATE.invoke(fd, 0L, size);
        } finally {
          CLOSE.invoke(fd);
        }
      } catch (final Throwable error) {
        LOGGER.debug("posix_fallocate 调用异常，回退零填充：{}", file, error);
        return FALLBACK;
      }
    }
  }
}
