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

import org.agrona.SystemUtil;

import java.io.IOException;
import java.nio.channels.FileChannel;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributeView;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

/**
 * 文件工具（高效版）：相对 {@link FileUtil} 的性能差异——
 *
 * <ul>
 *   <li>目录树复制：{@link FileChannel#transferTo} 零拷贝（Linux/macOS 走 sendfile，内核态直传， 不经用户态缓冲）替代逐文件 {@link
 *       Files#copy} 的流式搬运；大文件（快照 SST/日志）收益显著
 *   <li>并发复制：{@link #copyTreeConcurrent} 虚拟线程并行复制多文件目录（大量小文件场景）
 *   <li>目录判定：{@link DirectoryStream} 直迭代替 {@link Files#list} 流包装
 *   <li>建目录：单次 createDirectories（容忍已存在）替代 exists→isDirectory→create 三连 stat
 *   <li>删树：fail-fast 访问器 + 深度逆序单遍删除，路径预收集后按序删（避免 walk 中途失败的半删状态不可预测）
 * </ul>
 */
public final class FileUtil {

  private FileUtil() {
  }

  // ===== 持久化 =====

  /**
   * fsync 单个文件；已有打开通道时直接用 {@link FileChannel#force(boolean)} 更省一次 open
   */
  public static void flush(final Path path) throws IOException {
    try (final var channel = FileChannel.open(path, StandardOpenOption.READ)) {
      channel.force(true);
    }
  }

  /**
   * fsync 目录（Windows 为 no-op：NTFS 无需且 JDK 不支持）
   */
  public static void flushDirectory(final Path path) throws IOException {
    if (SystemUtil.isWindows()) {
      return;
    }
    flush(path);
  }

  /**
   * 移动 + fsync 父目录（防 0 长度经典问题）
   */
  public static void moveDurably(
    final Path source, final Path target, final StandardCopyOption... options)
    throws IOException {
    Files.move(source, target, options);
    flushDirectory(target.getParent());
  }

  // ===== 目录 =====

  /**
   * 确保目录存在：单次 createDirectories，已存在且非目录时抛 {@link NotDirectoryException}
   */
  /** 确保目录存在（ensureDirectory 别名） */
  public static void ensureDirectoryExists(final Path directory) throws IOException {
    ensureDirectory(directory);
  }

  public static void ensureDirectory(final Path directory) throws IOException {
    try {
      Files.createDirectories(directory);
    } catch (final FileAlreadyExistsException e) {
      if (!Files.isDirectory(directory)) {
        throw new NotDirectoryException(directory.toString());
      }
    }
  }

  /**
   * 目录不存在或为空即 true；非目录路径仅当不存在时 true
   */
  public static boolean isEmpty(final Path path) throws IOException {
    if (!Files.isDirectory(path)) {
      return !Files.exists(path);
    }
    try (final DirectoryStream<Path> entries = Files.newDirectoryStream(path)) {
      return !entries.iterator().hasNext();
    }
  }

  // ===== 删除 =====

  /**
   * 递归删除整棵目录树（含根）；任一文件删除失败立即失败
   */
  public static void deleteTree(final Path root) throws IOException {
    final List<Path> ordered = collectPostOrder(root);
    for (final Path path : ordered) {
      Files.deleteIfExists(path);
    }
  }

  /**
   * 同 {@link #deleteTree}，根不存在时静默成功
   */
  public static void deleteTreeIfExists(final Path root) throws IOException {
    if (Files.exists(root)) {
      deleteTree(root);
    }
  }

  /**
   * 清空目录内容但保留根目录（挂载卷等不可删场景）
   */
  public static void clearDirectory(final Path root) throws IOException {
    final List<Path> ordered = collectPostOrder(root);
    for (final Path path : ordered) {
      if (!path.equals(root)) {
        Files.deleteIfExists(path);
      }
    }
  }

  /**
   * 深度逆序列表（先文件后由深到浅的目录），删除前全量收集
   */
  private static List<Path> collectPostOrder(final Path root) throws IOException {
    final List<Path> paths = new ArrayList<>();
    Files.walkFileTree(
      root,
      new SimpleFileVisitor<>() {
        @Override
        public FileVisitResult visitFile(final Path file, final BasicFileAttributes attrs) {
          paths.add(file);
          return FileVisitResult.CONTINUE;
        }

        @Override
        public FileVisitResult preVisitDirectory(
          final Path dir, final BasicFileAttributes attrs) {
          paths.add(dir);
          return FileVisitResult.CONTINUE;
        }
      });
    // 收集序为前序(父在前); 逆序即得后序删除序(子先于父), 无需逐元素比较
    paths.sort(Comparator.reverseOrder());
    return paths;
  }

  // ===== 复制 =====

  /**
   * 零拷贝复制目录树（保留基本时间属性）
   */
  public static void copyTree(final Path source, final Path target) throws IOException {
    try (final var walk = Files.walk(source)) {
      walk.forEach(path -> copyEntry(source, target, path));
    }
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
    try (final var walk = Files.walk(source)) {
      walk.forEach(
        path -> {
          if (Files.isDirectory(path)) {
            directories.add(path);
          } else {
            files.add(path);
          }
        });
    }
    for (final Path dir : directories) {
      Files.createDirectories(target.resolve(source.relativize(dir)));
    }
    final ExecutorService executor =
      concurrency > 1 ? Executors.newVirtualThreadPerTaskExecutor() : null;
    try {
      if (executor == null) {
        for (final Path file : files) {
          copyFileZeroCopy(file, target.resolve(source.relativize(file)));
        }
        return;
      }
      final List<Future<?>> pending = new ArrayList<>(files.size());
      final java.util.concurrent.Semaphore permits =
        new java.util.concurrent.Semaphore(concurrency);
      for (final Path file : files) {
        permits.acquireUninterruptibly();
        pending.add(
          executor.submit(
            () -> {
              try {
                copyFileZeroCopy(file, target.resolve(source.relativize(file)));
              } catch (final IOException e) {
                throw new java.io.UncheckedIOException(e);
              } finally {
                permits.release();
              }
            }));
      }
      for (final Future<?> task : pending) {
        task.get();
      }
    } catch (final java.util.concurrent.ExecutionException e) {
      throw new IOException("并发复制失败: " + source, e.getCause());
    } catch (final InterruptedException e) {
      Thread.currentThread().interrupt();
      throw new IOException("并发复制被中断: " + source, e);
    } finally {
      if (executor != null) {
        executor.close();
      }
    }
  }

  private static void copyEntry(final Path sourceRoot, final Path targetRoot, final Path path) {
    try {
      final Path destination = targetRoot.resolve(sourceRoot.relativize(path));
      if (Files.isDirectory(path)) {
        Files.createDirectories(destination);
        return;
      }
      copyFileZeroCopy(path, destination);
    } catch (final IOException e) {
      throw new java.io.UncheckedIOException("复制失败: " + path, e);
    }
  }

  /**
   * 单文件零拷贝 + 基本时间属性保留
   */
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
}
