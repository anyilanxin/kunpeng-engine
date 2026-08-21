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
package com.anyilanxin.kunpeng.cluster.raft.journal.file;

import static com.google.common.base.Preconditions.checkNotNull;
import static java.util.Objects.requireNonNull;

import com.anyilanxin.kunpeng.cluster.raft.journal.CorruptedJournalException;
import com.anyilanxin.kunpeng.cluster.raft.journal.JournalException;
import com.anyilanxin.kunpeng.cluster.raft.journal.JournalMetaStore;
import java.io.File;
import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.NavigableMap;
import java.util.SortedMap;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ConcurrentSkipListMap;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * segment 生命周期的管理中枢。
 *
 * <p>持有以首索引为键的 segment 有序表，负责：启动时从磁盘加载并校验全部 segment、滚动
 * 新 segment（含后台预创建下一个）、按索引删除/重置 segment，以及清理此前未及删除的
 * 残留文件。
 */
final class SegmentsManager implements AutoCloseable {

  private static final Logger LOG = LoggerFactory.getLogger(SegmentsManager.class);

  /** 首个 segment 的编号。 */
  private static final long FIRST_SEGMENT_ID = 1;

  /** 全新日志的起始索引。 */
  private static final long INITIAL_INDEX = 1;

  /** 尚无任何记录时的应用层序号占位值。 */
  private static final long INITIAL_ASQN = SegmentedJournal.ASQN_IGNORE;

  private final NavigableMap<Long, Segment> segmentsByFirstIndex = new ConcurrentSkipListMap<>();
  private final JournalMetrics journalMetrics;
  private final JournalIndex journalIndex;
  private final SegmentLoader segmentLoader;
  private final JournalMetaStore metaStore;
  private final int maxSegmentSize;
  private final File directory;
  private final String name;

  /** 后台预创建的下一个 segment（尚未写入描述符）。 */
  private @Nullable CompletableFuture<UninitializedSegment> preallocatedSegment = null;

  /** 当前正在追加写入的 segment。 */
  private volatile @Nullable Segment currentSegment;

  SegmentsManager(
      final JournalIndex journalIndex,
      final int maxSegmentSize,
      final File directory,
      final String name,
      final SegmentLoader segmentLoader,
      final JournalMetrics journalMetrics,
      final JournalMetaStore metaStore) {
    this.name = checkNotNull(name, "name cannot be null");
    this.journalIndex = journalIndex;
    this.maxSegmentSize = maxSegmentSize;
    this.directory = directory;
    this.segmentLoader = segmentLoader;
    this.journalMetrics = journalMetrics;
    this.metaStore = metaStore;
  }

  /* ---------- 查询 ---------- */

  /** @return 当前正在写入的 segment，管理器尚未打开时为 null */
  @Nullable Segment getCurrentSegment() {
    return currentSegment;
  }

  /** @return 首索引最小的 segment，日志为空时为 null */
  @Nullable Segment getFirstSegment() {
    final Map.Entry<Long, Segment> first = segmentsByFirstIndex.firstEntry();
    return first != null ? first.getValue() : null;
  }

  /** @return 首索引最大的 segment，日志为空时为 null */
  @Nullable Segment getLastSegment() {
    final Map.Entry<Long, Segment> last = segmentsByFirstIndex.lastEntry();
    return last != null ? last.getValue() : null;
  }

  /** @return 首索引大于 {@code index} 的最小 segment，不存在则返回 null */
  @Nullable Segment getNextSegment(final long index) {
    final Map.Entry<Long, Segment> higher = segmentsByFirstIndex.higherEntry(index);
    return higher != null ? higher.getValue() : null;
  }

  /**
   * 取覆盖给定索引的 segment。
   *
   * <p>先看当前写入 segment（省一次查表），否则取首索引不大于 {@code index} 的最近
   * segment；都不命中时兜底返回首个 segment。
   */
  @Nullable Segment getSegment(final long index) {
    final Segment writing = currentSegment;
    if (writing != null && index > writing.index()) {
      return writing;
    }

    final Map.Entry<Long, Segment> floor = segmentsByFirstIndex.floorEntry(index);
    if (floor != null) {
      return floor.getValue();
    }
    return getFirstSegment();
  }

  /** @return 从覆盖 {@code index} 的 segment 起到末尾的所有 segment（只读视图） */
  SortedMap<Long, Segment> getTailSegments(final long index) {
    final Segment containing = getSegment(index);
    if (containing == null) {
      return Collections.emptySortedMap();
    }
    // 不能直接以 index 取尾视图：index 可能落在某个 segment 中间，需先定位到该 segment 的首索引
    return Collections.unmodifiableSortedMap(segmentsByFirstIndex.tailMap(containing.index(), true));
  }

  /* ---------- 打开与关闭 ---------- */

  /** 从磁盘加载全部 segment 并初始化当前写入 segment；目录为空时创建首个 segment。 */
  void open() {
    final var openDurationTimer = journalMetrics.startJournalOpenDurationTimer();
    for (final Segment segment : loadSegments()) {
      segmentsByFirstIndex.put(segment.descriptor().index(), segment);
      updateSegmentCount(1);
    }

    final Map.Entry<Long, Segment> lastLoaded = segmentsByFirstIndex.lastEntry();
    if (lastLoaded != null) {
      currentSegment = lastLoaded.getValue();
    } else {
      currentSegment = createSegmentAt(FIRST_SEGMENT_ID, INITIAL_INDEX, INITIAL_ASQN);
      segmentsByFirstIndex.put(INITIAL_INDEX, currentSegment);
      updateSegmentCount(1);
    }
    openDurationTimer.close();

    // 清理上次停机前来不及删除的文件；此时没有任何读取器持有它们，可以安全删除
    deleteDeferredFiles();
  }

  @Override
  public void close() {
    for (final Segment segment : segmentsByFirstIndex.values()) {
      LOG.debug("Closing segment: {}", segment);
      segment.close();
    }

    if (preallocatedSegment != null) {
      try {
        preallocatedSegment.join();
      } catch (final Exception e) {
        LOG.warn("Next segment preparation failed during close, ignoring and proceeding to close", e);
      }
      preallocatedSegment = null;
    }

    currentSegment = null;
  }

  /* ---------- 滚动与删除 ---------- */

  /**
   * 滚动到下一个 segment 并返回它。
   *
   * <p>优先消费后台预创建的半成品 segment；若预创建失败则回退为同步创建。
   *
   * @throws IllegalStateException 管理器未打开（无当前 segment）
   */
  Segment getNextSegment() {
    final Segment tail = getLastSegment();
    final long inheritedAsqn = tail != null ? tail.lastAsqn() : INITIAL_ASQN;
    final Segment writing = requireNonNull(currentSegment, "currentSegment is null");
    final long nextFirstIndex = writing.lastIndex() + 1;
    final long nextId = tail != null ? tail.descriptor().id() + 1 : FIRST_SEGMENT_ID;

    if (preallocatedSegment == null) {
      currentSegment = createSegmentAt(nextId, nextFirstIndex, inheritedAsqn);
    } else {
      try {
        currentSegment =
            preallocatedSegment
                .join()
                .initializeForUse(nextFirstIndex, inheritedAsqn, journalMetrics);
      } catch (final CompletionException e) {
        LOG.error("Failed to acquire next segment, retrying synchronously now.", e);
        preallocatedSegment = null;
        currentSegment = createSegmentAt(nextId, nextFirstIndex, inheritedAsqn);
      }
    }

    prepareNextSegment();

    segmentsByFirstIndex.put(nextFirstIndex, currentSegment);
    updateSegmentCount(1);
    return currentSegment;
  }

  /**
   * 删除首索引小于 {@code index} 的所有 segment。
   *
   * @return 实际删除了 segment 时返回 true
   */
  boolean deleteUntil(final long index) {
    final Map.Entry<Long, Segment> boundary = segmentsByFirstIndex.floorEntry(index);
    if (boundary == null) {
      return false;
    }

    final SortedMap<Long, Segment> expired =
        segmentsByFirstIndex.headMap(boundary.getValue().index());
    if (expired.isEmpty()) {
      LOG.debug(
          "No segments can be deleted with index < {} (first log index: {})",
          index,
          firstIndexOrZero());
      return false;
    }

    LOG.debug(
        "{} - Deleting log up from {} up to {} (removing {} segments)",
        name,
        firstIndexOrZero(),
        requireNonNull(expired.get(expired.lastKey())).index(),
        expired.size());
    for (final Segment segment : expired.values()) {
      LOG.trace("{} - Deleting segment: {}", name, segment);
      segment.delete();
      updateSegmentCount(-1);
    }
    expired.clear();

    journalIndex.deleteUntil(index);
    return true;
  }

  /**
   * 删除全部 segment，并以 {@code index} 为起始索引重建首个 segment。
   *
   * @return 重建后的首个 segment
   */
  Segment resetSegments(final long index) {
    // 先把最后已刷盘索引重置为语义空值再删数据：即使中途崩溃，重启时也能据此判定
    // "尚未写入任何内容"，即便读不到描述符（例如建完文件但还没写描述符就崩溃）
    metaStore.resetLastFlushedIndex();

    // 倒序删除：即使中途被打断，日志（以及日志与快照之间）也不会出现空洞
    final Iterator<Segment> descending = segmentsByFirstIndex.descendingMap().values().iterator();
    while (descending.hasNext()) {
      // 刻意不 close：这里可能只是软删除，让在途读取器读完后自行退出，
      // 避免与底层缓冲 unmap 产生竞态
      //noinspection resource
      descending.next().delete();
      descending.remove();
      updateSegmentCount(-1);
    }

    currentSegment = createSegmentAt(FIRST_SEGMENT_ID, index, INITIAL_ASQN);
    segmentsByFirstIndex.put(index, currentSegment);
    updateSegmentCount(1);
    return currentSegment;
  }

  /**
   * 移除并删除指定 segment，必要时重建当前写入 segment。
   *
   * @param segment 待移除的 segment
   */
  void removeSegment(final Segment segment) {
    segmentsByFirstIndex.remove(segment.index());
    updateSegmentCount(-1);
    segment.delete();
    refreshCurrentSegment();
  }

  /** 把当前写入 segment 指回末尾 segment；若已无 segment 则新建首个 segment。 */
  private void refreshCurrentSegment() {
    final Segment tail = getLastSegment();
    if (tail != null) {
      currentSegment = tail;
      return;
    }

    currentSegment = createSegmentAt(FIRST_SEGMENT_ID, INITIAL_INDEX, INITIAL_ASQN);
    segmentsByFirstIndex.put(INITIAL_INDEX, currentSegment);
    updateSegmentCount(1);
  }

  /* ---------- 创建 ---------- */

  /** 异步预创建下一个 segment（只分配文件与空间，不写描述符）。 */
  private void prepareNextSegment() {
    final long nextId = requireNonNull(currentSegment, "current segment is null").id() + 1;
    final var preDescriptor =
        SegmentDescriptor.builder()
            .withId(nextId)
            .withIndex(INITIAL_INDEX)
            .withMaxSegmentSize(maxSegmentSize)
            .build();
    preallocatedSegment =
        CompletableFuture.supplyAsync(() -> createUninitializedSegment(preDescriptor));
  }

  private Segment createSegmentAt(final long id, final long firstIndex, final long lastAsqn) {
    final var descriptor =
        SegmentDescriptor.builder()
            .withId(id)
            .withIndex(firstIndex)
            .withMaxSegmentSize(maxSegmentSize)
            .build();
    final var segmentFile = SegmentFile.createSegmentFile(name, directory, descriptor.id());
    return segmentLoader.createSegment(
        segmentFile.toPath(), descriptor, lastAsqn, journalIndex);
  }

  private UninitializedSegment createUninitializedSegment(final SegmentDescriptor descriptor) {
    final var segmentFile = SegmentFile.createSegmentFile(name, directory, descriptor.id());
    return segmentLoader.createUninitializedSegment(segmentFile.toPath(), descriptor, journalIndex);
  }

  /* ---------- 加载与损坏恢复 ---------- */

  /** 按编号升序扫描目录，逐一加载 segment 并校验索引连续性与已刷盘边界。 */
  private Collection<Segment> loadSegments() {
    final long lastFlushedIndex = metaStore.loadLastFlushedIndex();

    // 确保日志目录存在
    //noinspection ResultOfMethodCallIgnored
    directory.mkdirs();

    final List<File> files = getSortedLogSegments();
    final List<Segment> loaded = new ArrayList<>(files.size());
    Segment previousSegment = null;

    for (final Iterator<File> it = files.iterator(); it.hasNext(); ) {
      final File file = it.next();
      final int position = loaded.size();
      LOG.debug("Found segment file: {}", file.getName());

      try {
        final Segment segment =
            segmentLoader.loadExistingSegment(
                file.toPath(),
                previousSegment != null ? previousSegment.lastAsqn() : INITIAL_ASQN,
                journalIndex);

        if (previousSegment != null) {
          // segment 之间索引出现空洞则视为损坏
          checkSegmentsContiguous(previousSegment, segment);
        }

        if (!it.hasNext() && segment.lastIndex() < lastFlushedIndex) {
          // 最后一个 segment 必须覆盖到已刷盘边界
          throw new CorruptedJournalException(
              "Expected to find records until index %d, but last index is %d"
                  .formatted(lastFlushedIndex, segment.lastIndex()));
        }

        loaded.add(segment);
        previousSegment = segment;
      } catch (final CorruptedJournalException e) {
        if (discardUnflushedTailIfSafe(files, position, loaded, lastFlushedIndex)) {
          return loaded;
        }
        throw e;
      }
    }

    return loaded;
  }

  /** 校验相邻两个 segment 的索引首尾相接（前一个的 lastIndex + 1 == 后一个的首索引）。 */
  private void checkSegmentsContiguous(final Segment prevSegment, final Segment segment) {
    if (prevSegment.lastIndex() != segment.index() - 1) {
      throw new CorruptedJournalException(
          String.format(
              "Log segment %s is not aligned with previous segment %s (last index: %d).",
              segment, prevSegment, prevSegment.lastIndex()));
    }
  }

  /**
   * 判定损坏的尾部是否可以安全丢弃。
   *
   * <p>只有当损坏部分位于最后已刷盘索引之后（即从未被确认过）时才允许直接删除，否则返回
   * false 让异常继续抛出。
   *
   * @return 已删除损坏尾部文件时返回 true
   */
  private boolean discardUnflushedTailIfSafe(
      final List<File> files,
      final int failedPosition,
      final List<Segment> loadedSegments,
      final long lastFlushedIndex) {
    if (metaStore.hasLastFlushedIndex()) {
      long highestLoadedIndex = 0;
      final Segment lastLoaded = loadedSegments.isEmpty() ? null : loadedSegments.get(loadedSegments.size() - 1);
      if (lastLoaded != null) {
        highestLoadedIndex = lastLoaded.lastIndex();
      }

      if (lastFlushedIndex > highestLoadedIndex) {
        // 已确认的索引落在损坏区内，无法安全丢弃
        return false;
      }
    }

    deleteUnflushedSegments(files, failedPosition, lastFlushedIndex);
    return true;
  }

  /** 删除从首个损坏 segment 起到目录末尾的所有文件。 */
  private void deleteUnflushedSegments(
      final List<File> files, final int failedPosition, final long lastFlushedIndex) {
    LOG.debug(
        "Found corrupted segment after last ack'ed index {}. Deleting segments {} - {}",
        lastFlushedIndex,
        files.get(failedPosition).getName(),
        files.get(files.size() - 1).getName());

    for (final File file : files.subList(failedPosition, files.size())) {
      try {
        Files.delete(file.toPath());
      } catch (final IOException e) {
        throw new JournalException(
            String.format(
                "Failed to delete log segment '%s' when handling corruption.", file.getName()),
            e);
      }
    }
  }

  /** @return 目录下按编号升序排列的合法 segment 文件列表，可能为空但不为 null */
  private List<File> getSortedLogSegments() {
    final File[] found =
        directory.listFiles(file -> file.isFile() && SegmentFile.isSegmentFile(name, file));

    if (found == null) {
      throw new IllegalStateException(
          String.format(
              "Could not list files in directory '%s'. Either the path doesn't point to a directory or an I/O error occurred.",
              directory));
    }

    Arrays.sort(found, Comparator.comparingInt(f -> SegmentFile.getSegmentIdFromPath(f.getName())));
    return List.of(found);
  }

  /* ---------- 指标与残留清理 ---------- */

  /** 更新 segment 数量并重算 journal 磁盘占用 */
  private void updateSegmentCount(final int delta) {
    if (delta > 0) {
      journalMetrics.incSegmentCount();
    } else {
      journalMetrics.decSegmentCount();
    }
    journalMetrics.setJournalSize(
        segmentsByFirstIndex.values().stream().mapToLong(s -> s.file().file().length()).sum());
  }

  /** 删除目录中所有标记为待删除（软删除残留）的 segment 文件。 */
  private void deleteDeferredFiles() {
    try (final DirectoryStream<Path> markedForDeletion =
        Files.newDirectoryStream(
            directory.toPath(),
            path -> SegmentFile.isDeletedSegmentFile(name, path.getFileName().toString()))) {
      markedForDeletion.forEach(this::deleteDeferredFile);
    } catch (final IOException e) {
      LOG.warn(
          "Could not delete segment files marked for deletion in {}. This can result in unnecessary disk usage.",
          directory.toPath(),
          e);
    }
  }

  private void deleteDeferredFile(final Path segmentFileToDelete) {
    try {
      Files.deleteIfExists(segmentFileToDelete);
    } catch (final IOException e) {
      LOG.warn(
          "Could not delete file {} which is marked for deletion. This can result in unnecessary disk usage.",
          segmentFileToDelete,
          e);
    }
  }

  private long firstIndexOrZero() {
    final Segment first = getFirstSegment();
    return first != null ? first.index() : 0;
  }
}
