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

import static java.nio.file.FileVisitResult.CONTINUE;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.channels.FileChannel;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributeView;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.Semaphore;
import java.util.function.Predicate;
import org.agrona.SystemUtil;

/**
 * 文件工具。
 *
 * <ul>
 *   <li>目录树复制：{@link FileChannel#transferTo} 零拷贝（Linux/macOS 走 sendfile，内核态直传， 不经用户态缓冲）替代逐文件 {@link
 *       Files#copy} 的流式搬运；大文件（快照 SST/日志）收益显著
 *   <li>并发复制：{@link #copyTreeConcurrent} 虚拟线程并行复制多文件目录（大量小文件场景）
 *   <li>目录判定：{@link DirectoryStream} 直迭代替 {@link Files#list} 流包装
 *   <li>建目录：单次 createDirectories（容忍已存在）替代 exists→isDirectory→create 三连 stat
 *   <li>删树：{@link Files#walkFileTree} 后序访问器边走边删（先文件后目录），无路径物化
 * </ul>
 */
public final class FileUtil {

  private FileUtil() {}

  // ===== 持久化 =====

  /** fsync 单个文件；已有打开通道时直接用 {@link FileChannel#force(boolean)} 更省一次 open */
  public static void flush(final Path path) throws IOException {
    try (final var channel = FileChannel.open(path, StandardOpenOption.READ)) {
      channel.force(true);
    }
  }

  /** fsync 目录（Windows 为 no-op：NTFS 无需且 JDK 不支持） */
  public static void flushDirectory(final Path path) throws IOException {
    if (SystemUtil.isWindows()) {
      return;
    }
    flush(path);
  }

  /** 移动 + fsync 父目录（防 0 长度经典问题） */
  public static void moveDurably(
      final Path source, final Path target, final StandardCopyOption... options)
      throws IOException {
    Files.move(source, target, options);
    flushDirectory(target.getParent());
  }

  // ===== 目录 =====

  /** 确保目录存在：单次 createDirectories，已存在且非目录时抛 {@link NotDirectoryException} */
  public static void ensureDirectory(final Path directory) throws IOException {
    try {
      Files.createDirectories(directory);
    } catch (final FileAlreadyExistsException e) {
      if (!Files.isDirectory(directory)) {
        throw new NotDirectoryException(directory.toString());
      }
    }
  }

  /** 目录不存在或为空即 true；非目录路径仅当不存在时 true */
  public static boolean isEmpty(final Path path) throws IOException {
    if (!Files.isDirectory(path)) {
      return !Files.exists(path);
    }
    try (final DirectoryStream<Path> entries = Files.newDirectoryStream(path)) {
      return !entries.iterator().hasNext();
    }
  }

  // ===== 删除 =====

  /** 递归删除整棵目录树（含根）；任一文件删除失败立即失败 */
  public static void deleteTree(final Path root) throws IOException {
    Files.walkFileTree(root, new FolderDeleter(path -> true));
  }

  /** 同 {@link #deleteTree}，根不存在时静默成功 */
  public static void deleteTreeIfExists(final Path root) throws IOException {
    try {
      deleteTree(root);
    } catch (final NoSuchFileException ignored) { // NOSONAR
      // 根不存在视为成功
    }
  }

  /** 清空目录内容但保留根目录（挂载卷等不可删场景） */
  public static void clearDirectory(final Path root) throws IOException {
    Files.walkFileTree(root, new FolderDeleter(path -> !path.equals(root)));
  }

  // ===== 复制 =====

  /** 零拷贝复制目录树（保留基本时间属性） */
  public static void copyTree(final Path source, final Path target) throws IOException {
    Files.walkFileTree(
        source,
        new SimpleFileVisitor<>() {
          @Override
          public FileVisitResult preVisitDirectory(final Path dir, final BasicFileAttributes attrs)
              throws IOException {
            Files.createDirectories(target.resolve(source.relativize(dir)));
            return CONTINUE;
          }

          @Override
          public FileVisitResult visitFile(final Path file, final BasicFileAttributes attrs)
              throws IOException {
            copyFileZeroCopy(file, target.resolve(source.relativize(file)));
            return CONTINUE;
          }
        });
  }

  /**
   * 并发复制目录树：先串行建全部目录，再以虚拟线程并行复制文件（每文件一个任务）。 大量小文件目录（快照/检查点）吞吐显著优于串行；单文件正确性与 {@link #copyTree} 一致。
   *
   * @param concurrency 最大并行文件数
   */
  public static void copyTreeConcurrent(final Path source, final Path target, final int concurrency)
      throws IOException {
    final List<Path> directories = new ArrayList<>();
    final List<Path> files = new ArrayList<>();
    Files.walkFileTree(
        source,
        new SimpleFileVisitor<>() {
          @Override
          public FileVisitResult preVisitDirectory(
              final Path dir, final BasicFileAttributes attrs) {
            directories.add(dir);
            return CONTINUE;
          }

          @Override
          public FileVisitResult visitFile(final Path file, final BasicFileAttributes attrs) {
            files.add(file);
            return CONTINUE;
          }
        });
    for (final Path dir : directories) {
      Files.createDirectories(target.resolve(source.relativize(dir)));
    }
    try (final ExecutorService executor =
        concurrency > 1 ? Executors.newVirtualThreadPerTaskExecutor() : null) {
      if (executor == null) {
        for (final Path file : files) {
          copyFileZeroCopy(file, target.resolve(source.relativize(file)));
        }
        return;
      }
      final List<Future<?>> pending = new ArrayList<>(files.size());
      final Semaphore permits = new Semaphore(concurrency);
      for (final Path file : files) {
        permits.acquireUninterruptibly();
        pending.add(
            executor.submit(
                () -> {
                  try {
                    copyFileZeroCopy(file, target.resolve(source.relativize(file)));
                  } catch (final IOException e) {
                    throw new UncheckedIOException(e);
                  } finally {
                    permits.release();
                  }
                }));
      }
      for (final Future<?> task : pending) {
        task.get();
      }
    } catch (final ExecutionException e) {
      throw new IOException("并发复制失败: " + source, e.getCause());
    } catch (final InterruptedException e) {
      Thread.currentThread().interrupt();
      throw new IOException("并发复制被中断: " + source, e);
    }
  }

  /** 单文件零拷贝 + 基本时间属性保留 */
  private static void copyFileZeroCopy(final Path source, final Path target) throws IOException {
    try (final FileChannel in = FileChannel.open(source, StandardOpenOption.READ);
        final FileChannel out =
            FileChannel.open(
                target,
                StandardOpenOption.CREATE,
                StandardOpenOption.WRITE,
                StandardOpenOption.TRUNCATE_EXISTING)) {
      long transferred = 0;
      final long size = in.size();
      while (transferred < size) {
        transferred += in.transferTo(transferred, size - transferred, out);
      }
    }
    final BasicFileAttributes attributes = Files.readAttributes(source, BasicFileAttributes.class);
    Files.getFileAttributeView(target, BasicFileAttributeView.class)
        .setTimes(attributes.lastModifiedTime(), attributes.lastAccessTime(), null);
  }

  /** 后序删除访问器：先删文件与子目录，最后删（或跳过）目录自身 */
  private static final class FolderDeleter extends SimpleFileVisitor<Path> {
    private final Predicate<Path> shouldDelete;

    private FolderDeleter(final Predicate<Path> shouldDelete) {
      this.shouldDelete = shouldDelete;
    }

    @Override
    public FileVisitResult visitFile(final Path file, final BasicFileAttributes attrs)
        throws IOException {
      if (shouldDelete.test(file)) {
        Files.deleteIfExists(file);
      }
      return CONTINUE;
    }

    @Override
    public FileVisitResult postVisitDirectory(final Path dir, final IOException exc)
        throws IOException {
      if (exc != null) {
        throw exc;
      }
      if (shouldDelete.test(dir)) {
        Files.deleteIfExists(dir);
      }
      return CONTINUE;
    }
  }
}
