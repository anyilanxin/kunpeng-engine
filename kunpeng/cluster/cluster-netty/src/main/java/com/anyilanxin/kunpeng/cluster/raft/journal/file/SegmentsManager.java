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
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.concurrent.atomic.AtomicLong;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * segment 全生命周期管理者。
 *
 * <p>内部是一张以“segment 首索引”为键的有序表。对外承担：启动时扫描目录装载并校验所有 segment、写入时按需滚动新段、按索引裁剪或整体重建，以及启动后清扫上次停机遗留的软删除
 * 文件。
 */
final class SegmentsManager implements AutoCloseable {

  private static final Logger LOG = LoggerFactory.getLogger(SegmentsManager.class);

  /** segment 编号从此值起分配。 */
  private static final long SEGMENT_ID_ORIGIN = 1;

  /** 空日志的第一条记录索引。 */
  private static final long EMPTY_LOG_START_INDEX = 1;

  /** 一条记录都还没有时可继承的应用层序号。 */
  private static final long EMPTY_LOG_ASQN = SegmentedJournal.ASQN_IGNORE;

  private final NavigableMap<Long, Segment> segmentsByIndex = new ConcurrentSkipListMap<>();
  private final JournalMetrics metrics;
  private final JournalIndex indexLookup;
  private final SegmentLoader loader;
  private final JournalMetaStore metaStore;
  private final int segmentSizeLimit;
  private final File storageDir;
  private final String journalName;
  private final int journalIndexDensity;

  /** 追加写入当前落在这个 segment 上；未打开时为 null。 */
  private volatile @Nullable Segment activeSegment;

  /**
   * active 段的索引写入映射；非 active 段的索引文件在建段/滚动时已封存（Kafka offset index 风格：预分配 + mmap +
   * 追加式提交），只有接收写入的段需要常驻映射。
   */
  private @Nullable SegmentIndexFile activeIndexFile;

  // 磁盘占用增量账本：segment 建段时即预分配到 segmentSizeLimit，长度不随写入变化，
  // 因此加减即可维持精确值，不必每次全量汇总
  private final AtomicLong diskUsageBytes = new AtomicLong();

  SegmentsManager(
      final JournalIndex journalIndex,
      final int maxSegmentSize,
      final File directory,
      final String name,
      final SegmentLoader segmentLoader,
      final JournalMetrics journalMetrics,
      final JournalMetaStore metaStore,
      final int journalIndexDensity) {
    this.journalName = requireNonNull(name, "journal 名不能为空");
    this.indexLookup = journalIndex;
    this.segmentSizeLimit = maxSegmentSize;
    this.storageDir = directory;
    this.loader = segmentLoader;
    this.metrics = journalMetrics;
    this.metaStore = metaStore;
    this.journalIndexDensity = journalIndexDensity;
  }

  /* ---------- 查询 ---------- */

  /**
   * @return 正在接收写入的 segment；尚未 open 时为 null
   */
  @Nullable Segment getCurrentSegment() {
    return activeSegment;
  }

  /**
   * @return 首索引最小的 segment；一张空表时为 null
   */
  @Nullable Segment getFirstSegment() {
    final Map.Entry<Long, Segment> entry = segmentsByIndex.firstEntry();
    return entry == null ? null : entry.getValue();
  }

  /**
   * @return 首索引最大的 segment；一张空表时为 null
   */
  @Nullable Segment getLastSegment() {
    final Map.Entry<Long, Segment> entry = segmentsByIndex.lastEntry();
    return entry == null ? null : entry.getValue();
  }

  /**
   * @return 首索引比 {@code index} 大的最小 segment；没有更靠后的段时为 null
   */
  @Nullable Segment getNextSegment(final long index) {
    final Map.Entry<Long, Segment> entry = segmentsByIndex.higherEntry(index);
    return entry == null ? null : entry.getValue();
  }

  /**
   * 定位覆盖 {@code index} 的 segment。
   *
   * <p>先看写入段（省一次查表），否则取首索引不大于 {@code index} 的最近一段；两者都不命中 说明 index 落在整个日志之前，兜底返回首段。
   */
  @Nullable Segment getSegment(final long index) {
    final Segment writing = activeSegment;
    if (writing != null && index > writing.index()) {
      return writing;
    }

    final Map.Entry<Long, Segment> floor = segmentsByIndex.floorEntry(index);
    if (floor != null) {
      return floor.getValue();
    }
    return getFirstSegment();
  }

  /**
   * @return 从覆盖 {@code index} 的那段起、直到末尾的全部 segment（只读视图）
   */
  SortedMap<Long, Segment> getTailSegments(final long index) {
    final Segment anchor = getSegment(index);
    if (anchor == null) {
      return Collections.emptySortedMap();
    }
    // 不能直接 tailMap(index)：index 可能停在某个 segment 中间，必须先锚定该段的首索引
    return Collections.unmodifiableSortedMap(segmentsByIndex.tailMap(anchor.index(), true));
  }

  /* ---------- 打开与关闭 ---------- */

  /** 装载磁盘上的全部 segment 并确定写入段；目录里一个段都没有时新建首段。 */
  void open() {
    final var openTimer = metrics.startJournalOpenDurationTimer();
    for (final Segment segment : scanAndLoadSegments()) {
      segmentsByIndex.put(segment.descriptor().index(), segment);
      trackSegmentAdded(segment);
    }

    final Segment tail = getLastSegment();
    if (tail != null) {
      activeSegment = tail;
      openActiveIndexFile(tail);
    } else {
      activeSegment = createSegment(SEGMENT_ID_ORIGIN, EMPTY_LOG_START_INDEX, EMPTY_LOG_ASQN);
      segmentsByIndex.put(activeSegment.index(), activeSegment);
      trackSegmentAdded(activeSegment);
    }
    openTimer.close();

    // 此时不可能还有读取者持有旧文件，软删除残留可以安全清掉
    purgeMarkedFiles();
  }

  @Override
  public void close() {
    // 封存 active 段剩余条目；其余段的索引文件在滚动时已封存
    persistActiveSegmentIndex();
    closeIndexFile();
    for (final Segment segment : segmentsByIndex.values()) {
      LOG.debug("关闭 segment：{}", segment);
      segment.close();
    }

    activeSegment = null;
  }

  /* ---------- 滚动与删除 ---------- */

  /**
   * 滚出下一个 segment 并切换为写入段。
   *
   * @throws IllegalStateException 尚未 open、没有写入段可用
   */
  Segment getNextSegment() {
    // 滚动前先封存旧段的索引，把崩溃时的索引损失限制在单段之内
    final Segment tail = getLastSegment();
    persistActiveSegmentIndex();
    closeIndexFile();

    final long inheritedAsqn = tail == null ? EMPTY_LOG_ASQN : tail.lastAsqn();
    final Segment writing = requireNonNull(activeSegment, "journal 尚未打开，无法滚动 segment");
    final long nextStart = writing.lastIndex() + 1;
    final long nextId = tail == null ? SEGMENT_ID_ORIGIN : tail.descriptor().id() + 1;

    activeSegment = createSegment(nextId, nextStart, inheritedAsqn);
    segmentsByIndex.put(nextStart, activeSegment);
    trackSegmentAdded(activeSegment);
    return activeSegment;
  }

  /**
   * 裁掉首索引小于 {@code index} 的所有 segment。
   *
   * @return 实际删除了至少一段时为 true
   */
  boolean deleteUntil(final long index) {
    final Map.Entry<Long, Segment> boundary = segmentsByIndex.floorEntry(index);
    if (boundary == null) {
      return false;
    }

    final SortedMap<Long, Segment> doomed = segmentsByIndex.headMap(boundary.getValue().index());
    if (doomed.isEmpty()) {
      LOG.debug("无可裁剪 segment：阈值 index {}，日志首索引 {}", index, firstIndexOrZero());
      return false;
    }

    LOG.debug(
        "{}：裁剪首索引 {} 之前的 {} 个 segment（末段止于 {}）",
        journalName,
        firstIndexOrZero(),
        doomed.size(),
        requireNonNull(doomed.get(doomed.lastKey())).index());
    for (final Segment segment : doomed.values()) {
      LOG.trace("{}：删除 segment {}", journalName, segment);
      // 先记账再删文件：文件一旦删除，长度读出来就是 0
      trackSegmentRemoved(segment);
      segment.delete();
      removeSegmentIndex(segment);
    }
    doomed.clear();

    indexLookup.deleteUntil(index);
    return true;
  }

  /**
   * 删除全部 segment，再以 {@code index} 为起始索引重建首段。
   *
   * @return 重建出的首段
   */
  Segment resetSegments(final long index) {
    // 先抹掉已刷盘边界再动数据文件：即便中途崩溃，重启也能凭“无刷盘边界”识别空日志，
    // 哪怕描述符都已读不出（例如文件建好但描述符未写就崩了）
    metaStore.resetLastFlushedIndex();

    // 从尾往头删：任意时刻被打断，剩下的日志都不带空洞（日志与快照之间同样如此）
    // 索引映射先于文件删除关闭，避免向已删除文件继续追加
    closeIndexFile();
    final Iterator<Segment> fromTail = segmentsByIndex.descendingMap().values().iterator();
    while (fromTail.hasNext()) {
      // 刻意不 close：这里的删除是软删除，在途读取器读完自然退出，
      // 也避免了与底层映射 unmap 的竞态
      //noinspection resource
      final Segment segment = fromTail.next();
      trackSegmentRemoved(segment);
      segment.delete();
      removeSegmentIndex(segment);
      fromTail.remove();
    }

    activeSegment = createSegment(SEGMENT_ID_ORIGIN, index, EMPTY_LOG_ASQN);
    segmentsByIndex.put(index, activeSegment);
    trackSegmentAdded(activeSegment);
    return activeSegment;
  }

  /**
   * 移除指定 segment；若写段被移走，则把写段指回末段或重建首段。
   *
   * @param segment 待移除的 segment
   */
  void removeSegment(final Segment segment) {
    segmentsByIndex.remove(segment.index());
    trackSegmentRemoved(segment);
    if (segment == activeSegment) {
      closeIndexFile();
    }
    segment.delete();
    removeSegmentIndex(segment);
    repointActiveSegment();
  }

  /** 写段指回末段；一段不剩时新建首段。 */
  private void repointActiveSegment() {
    final Segment tail = getLastSegment();
    if (tail != null) {
      activeSegment = tail;
      // 指回的是已封存段：续接其提交点，若日志截断使条目收缩，下次持久化时全量重写自愈
      openActiveIndexFile(tail);
      return;
    }

    activeSegment = createSegment(SEGMENT_ID_ORIGIN, EMPTY_LOG_START_INDEX, EMPTY_LOG_ASQN);
    segmentsByIndex.put(activeSegment.index(), activeSegment);
    trackSegmentAdded(activeSegment);
  }

  /* ---------- 建段 ---------- */

  private Segment createSegment(final long id, final long firstIndex, final long lastAsqn) {
    final var descriptor =
        SegmentDescriptor.builder()
            .withId(id)
            .withIndex(firstIndex)
            .withMaxSegmentSize(segmentSizeLimit)
            .build();
    final var segmentFile = SegmentFile.createSegmentFile(journalName, storageDir, descriptor.id());
    final var segment =
        loader.createSegment(segmentFile.toPath(), descriptor, lastAsqn, indexLookup);
    openActiveIndexFile(segment);
    return segment;
  }

  /* ---------- 装载与损坏处理 ---------- */

  /** 按编号升序装载目录中的 segment，校验相邻连续性与刷盘边界；必要时丢弃未刷盘的损坏尾部。 */
  private Collection<Segment> scanAndLoadSegments() {
    final long lastFlushedIndex = metaStore.loadLastFlushedIndex();

    // 装载前确保日志目录存在
    //noinspection ResultOfMethodCallIgnored
    storageDir.mkdirs();

    final List<File> files = sortedSegmentFiles();
    final List<Segment> loaded = new ArrayList<>(files.size());
    Segment previousSegment = null;

    for (int i = 0; i < files.size(); i++) {
      final File file = files.get(i);
      final boolean isLastFile = i == files.size() - 1;
      LOG.debug("发现 segment 文件：{}", file.getName());

      try {
        final Segment segment =
            loader.loadExistingSegment(
                file.toPath(),
                previousSegment == null ? EMPTY_LOG_ASQN : previousSegment.lastAsqn(),
                indexLookup);

        if (previousSegment != null) {
          // 相邻段首尾脱节视同日志损坏
          verifySegmentsContiguous(previousSegment, segment);
        }

        if (isLastFile && segment.lastIndex() < lastFlushedIndex) {
          // 已刷盘边界必须被最后一个 segment 覆盖到
          throw new CorruptedJournalException(
              "日志不完整：已刷盘边界为 %d，末段最后索引只有 %d".formatted(lastFlushedIndex, segment.lastIndex()));
        }

        loaded.add(segment);
        restoreSegmentIndex(segment);
        previousSegment = segment;
      } catch (final CorruptedJournalException e) {
        if (canSafelyDiscardCorruptedTail(files, i, loaded, lastFlushedIndex)) {
          return loaded;
        }
        throw e;
      }
    }

    return loaded;
  }

  /** 校验两段首尾相接：前段 lastIndex + 1 必须等于后段首索引。 */
  private void verifySegmentsContiguous(final Segment prevSegment, final Segment segment) {
    if (segment.index() != prevSegment.lastIndex() + 1) {
      throw new CorruptedJournalException(
          "segment %s 与前一段 %s 索引脱节（前段止于 %d）"
              .formatted(segment, prevSegment, prevSegment.lastIndex()));
    }
  }

  /**
   * 判断损坏的尾部能否直接丢弃。
   *
   * <p>只有损坏范围整体位于刷盘边界之后（即从未被确认过）才允许删文件了事；否则返回 false， 让异常继续上抛交由人工处置。
   *
   * @return 已把损坏尾部的文件删掉时为 true
   */
  private boolean canSafelyDiscardCorruptedTail(
      final List<File> files,
      final int failedAt,
      final List<Segment> loadedSegments,
      final long lastFlushedIndex) {
    if (metaStore.hasLastFlushedIndex()) {
      final long highestLoadedIndex =
          loadedSegments.isEmpty() ? 0 : loadedSegments.get(loadedSegments.size() - 1).lastIndex();
      if (lastFlushedIndex > highestLoadedIndex) {
        // 已确认的索引淹没在损坏区里，丢弃不安全
        return false;
      }
    }

    discardCorruptedTail(files, failedAt, lastFlushedIndex);
    return true;
  }

  /** 删除自首个损坏文件起到目录末尾的全部 segment 文件（含对应索引文件）。 */
  private void discardCorruptedTail(
      final List<File> files, final int failedAt, final long lastFlushedIndex) {
    LOG.warn(
        "在已确认索引 {} 之后发现损坏 segment，删除 {} 至 {}",
        lastFlushedIndex,
        files.get(failedAt).getName(),
        files.get(files.size() - 1).getName());

    for (final File file : files.subList(failedAt, files.size())) {
      try {
        Files.delete(file.toPath());
        Files.deleteIfExists(SegmentFile.indexFileOf(file).toPath());
      } catch (final IOException e) {
        throw new JournalException("清理损坏 segment 文件 '%s' 失败".formatted(file.getName()), e);
      }
    }
  }

  /* ---------- 索引文件读写 ---------- */

  /**
   * 把当前写入段的稀疏索引增量落盘。
   *
   * <p>在 {@link SegmentedJournal#flush()} 中于日志 fsync 之后调用：log 数据先行持久化，索引引用的 position 才一定有效；未滚动也未关闭的
   * active 段由此获得运行期落盘点，重启后免于扫描重建。追加式更新只写新增条目， 条目收缩（日志截断）时全量重写自愈。
   */
  void persistActiveSegmentIndex() {
    final Segment segment = activeSegment;
    if (segment == null) {
      return;
    }

    final var entries = indexLookup.entriesInRange(segment.index(), segment.lastIndex());
    if (entries.isEmpty() && activeIndexFile == null) {
      // 没有新条目，也没有需要收缩的既有提交点
      return;
    }

    try {
      ensureActiveIndexFile(segment).persist(entries);
    } catch (final Exception e) {
      // 索引只是读路径的加速缓存，写不进去不影响正确性；下次落盘重试，重启后扫描重建
      LOG.warn("segment {} 的索引落盘失败", segment, e);
      closeIndexFile();
    }
  }

  /** 为 active 段打开（或新建）索引写入映射；先关闭旧映射。 */
  private void openActiveIndexFile(final Segment segment) {
    closeIndexFile();
    try {
      activeIndexFile =
          SegmentIndexFile.openOrCreate(
              segment.file().indexFile().toPath(), indexEntryCapacity(segment));
    } catch (final IOException e) {
      LOG.warn("segment {} 的索引文件打开失败，本段索引暂退化为仅内存", segment, e);
    }
  }

  /**
   * @return active 段的索引写入映射，缺失时延迟打开
   */
  private SegmentIndexFile ensureActiveIndexFile(final Segment segment) throws IOException {
    if (activeIndexFile == null) {
      activeIndexFile =
          SegmentIndexFile.openOrCreate(
              segment.file().indexFile().toPath(), indexEntryCapacity(segment));
    }
    return activeIndexFile;
  }

  /** 按该段创建时的容量估算索引条目数：段的 maxSegmentSize 封存在描述符里一次性定死， 重启后配置变更不改变既有段的 idx 预分配长度。 */
  private int indexEntryCapacity(final Segment segment) {
    return SegmentIndexFile.capacityEntries(
        segment.descriptor().maxSegmentSize(), journalIndexDensity);
  }

  private void closeIndexFile() {
    if (activeIndexFile != null) {
      activeIndexFile.close();
      activeIndexFile = null;
    }
  }

  /** 尽力装载该段的磁盘索引；文件不合法则删除（映射打开时按空索引重建），读路径自会扫描重建。 */
  private void restoreSegmentIndex(final Segment segment) {
    final var indexFile = segment.file().indexFile().toPath();
    if (!Files.exists(indexFile)) {
      return;
    }

    final var descriptor = segment.descriptor();
    final var entries =
        SegmentIndexFile.read(
            indexFile,
            segment.index(),
            segment.lastIndex(),
            descriptor.encodingLength(),
            descriptor.maxSegmentSize());
    if (entries == null) {
      LOG.debug("索引文件 {} 缺失或不合法，删除并改为扫描重建", indexFile);
      try {
        Files.deleteIfExists(indexFile);
      } catch (final IOException e) {
        LOG.warn("非法索引文件 {} 删除失败", indexFile, e);
      }
      return;
    }
    indexLookup.indexAll(entries);
  }

  private void removeSegmentIndex(final Segment segment) {
    try {
      Files.deleteIfExists(segment.file().indexFile().toPath());
    } catch (final IOException e) {
      LOG.warn("segment {} 的索引文件删除失败，将残留为无用磁盘占用", segment, e);
    }
  }

  /**
   * @return 目录中全部合法 segment 文件，按段编号升序；可为空列表，不为 null
   */
  private List<File> sortedSegmentFiles() {
    final File[] present =
        storageDir.listFiles(file -> file.isFile() && SegmentFile.isSegmentFile(journalName, file));

    if (present == null) {
      throw new IllegalStateException("无法列出目录 '%s' 中的文件：不是目录，或列举时发生 IO 错误".formatted(storageDir));
    }

    return Arrays.stream(present)
        .sorted(Comparator.comparingInt(file -> SegmentFile.getSegmentIdFromPath(file.getName())))
        .toList();
  }

  /* ---------- 指标与残留清扫 ---------- */

  /** 段加入后记账：段文件长度在建段时即固定为上限值。 */
  private void trackSegmentAdded(final Segment segment) {
    metrics.incSegmentCount();
    diskUsageBytes.addAndGet(segment.file().file().length());
    metrics.setJournalSize(diskUsageBytes.get());
  }

  /** 段移除前记账；必须在 {@code segment.delete()} 之前调用，删文件后长度归零。 */
  private void trackSegmentRemoved(final Segment segment) {
    metrics.decSegmentCount();
    diskUsageBytes.addAndGet(-segment.file().file().length());
    metrics.setJournalSize(diskUsageBytes.get());
  }

  /** 清扫目录中所有带软删除标记的 segment 残留文件。 */
  private void purgeMarkedFiles() {
    try (final DirectoryStream<Path> markedForDeletion =
        Files.newDirectoryStream(
            storageDir.toPath(),
            path -> SegmentFile.isDeletedSegmentFile(journalName, path.getFileName().toString()))) {
      markedForDeletion.forEach(this::purgeMarkedFile);
    } catch (final IOException e) {
      LOG.warn("目录 {} 中软删除残留清扫失败，将留下无用磁盘占用", storageDir.toPath(), e);
    }
  }

  private void purgeMarkedFile(final Path markedFile) {
    try {
      Files.deleteIfExists(markedFile);
    } catch (final IOException e) {
      LOG.warn("软删除残留文件 {} 清除失败，将留下无用磁盘占用", markedFile, e);
    }
  }

  private long firstIndexOrZero() {
    final Segment first = getFirstSegment();
    return first == null ? 0 : first.index();
  }
}
