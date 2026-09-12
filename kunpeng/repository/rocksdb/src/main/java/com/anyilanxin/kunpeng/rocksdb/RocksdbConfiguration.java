/*
 * Copyright © 2026 anyilanxin zxh(anyilanxin@aliyun.com)
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
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package com.anyilanxin.kunpeng.rocksdb;

import java.util.Properties;

/**
 * RocksDB 运行时配置，供 rocksdb 模块构建 DB/列族 Options 使用。
 *
 * @author zxuanhong
 */
public final class RocksdbConfiguration {

  public static final long DEFAULT_MEMORY_LIMIT = 512 * 1024 * 1024L;
  public static final int DEFAULT_UNLIMITED_MAX_OPEN_FILES = -1;
  public static final int DEFAULT_MAX_WRITE_BUFFER_NUMBER = 6;
  public static final int DEFAULT_MIN_WRITE_BUFFER_NUMBER_TO_MERGE = 3;
  public static final boolean DEFAULT_STATISTICS_ENABLED = false;

  /**
   * 注意：只有一个列族时禁用 WAL 是安全的；存在多个列族时，checkpoint 期间跨列族的一致性由 WAL 保证。
   *
   * <p>http://rocksdb.org/blog/2015/11/10/use-checkpoints-for-efficient-snapshots.html >>>
   * Checkpoint 特性让 RocksDB 能在指定目录创建数据库的一致性快照：快照与原库在同一文件系统时 SST 文件以硬链接方式创建，否则复制 SST 文件；manifest 与
   * CURRENT 文件始终复制。此外，存在多个列族时，还会复制覆盖 checkpoint 起止区间的日志文件，以保证跨列族快照的一致性。<<<
   */
  public static final boolean DEFAULT_WAL_DISABLED = true;

  /**
   * 开启后向 RocksDB compaction 提示按虚拟列族前缀进行 compaction，结果是 SST 文件更多但按相关数据集切分。
   *
   * <p>基准测试表明即使在很大的 RocksDB 状态下也能获得更好的性能。
   */
  public static final boolean DEFAULT_SST_PARTITIONING_ENABLED = true;

  public static final int DEFAULT_IO_RATE_BYTES_PER_SECOND = 0;

  /**
   * {@link #maxBackgroundJobs} 的哨兵值，表示"按 CPU 自适应"。取到该值时由 {@link #adaptiveMaxBackgroundJobs()}
   * 惰性计算实际任务数。
   *
   * <p>使用哨兵值而非静态计算默认值，是为了在类加载与构建 RocksDB options 之间 CPU 亲和性发生变化时（例如测试中自定义 {@code
   * availableProcessors}）仍能取到正确值。
   */
  public static final int ADAPTIVE_MAX_BACKGROUND_JOBS = 0;

  /**
   * 自适应后台任务数：至少 4 个（1 个 flush + 3 个 compaction），并随可用 CPU 扩容。写密集场景下 RocksDB 建议约为核数的一半用于 compaction
   * 吞吐。需要覆盖时，通过 {@code broker.rocksdb.max-background-jobs} 将 {@link #maxBackgroundJobs} 设为正值。
   */
  public static int adaptiveMaxBackgroundJobs() {
    return Math.max(4, Runtime.getRuntime().availableProcessors() / 2);
  }

  /**
   * {@link #memoryLimit} 中分配给 block cache 的比例。默认 1/3（约 0.333），与之前硬编码行为一致。读多场景可向 0.5 调高；写多、memtable
   * 预算更重要的场景可向 0.2 调低。
   */
  public static final double DEFAULT_BLOCK_CACHE_MEMORY_RATIO = 1.0 / 3.0;

  /** LRU block cache 的分片位数。缓存拆分为 2^bits 个分片以降低锁竞争。默认 8（256 个分片）在 2GB 以内缓存下适用；缓存非常大或并发读很高时调大。 */
  public static final int DEFAULT_BLOCK_CACHE_SHARD_BITS = 8;

  private Properties columnFamilyOptions = new Properties();
  private boolean statisticsEnabled = DEFAULT_STATISTICS_ENABLED;
  private long memoryLimit = DEFAULT_MEMORY_LIMIT;
  private int maxWriteBufferNumber = DEFAULT_MAX_WRITE_BUFFER_NUMBER;
  private int minWriteBufferNumberToMerge = DEFAULT_MIN_WRITE_BUFFER_NUMBER_TO_MERGE;
  private boolean walDisabled = DEFAULT_WAL_DISABLED;

  private boolean sstPartitioningEnabled = DEFAULT_SST_PARTITIONING_ENABLED;

  /**
   * RocksDB 保持打开的文件数上限，默认不限制（-1）。这是出于性能考虑：设为大于 0 的值时，RocksDB 需要在 TableCache 中跟踪已打开文件并在访问时查找。
   *
   * <p>https://github.com/facebook/rocksdb/wiki/RocksDB-Tuning-Guide#general-options
   */
  private int maxOpenFiles = DEFAULT_UNLIMITED_MAX_OPEN_FILES;

  /**
   * 限制 RocksDB 的写 I/O 速率，作用于其全部写入（flush、compaction、WAL 等）。配置后可避免写尖峰影响读，获得更可预测的性能。
   *
   * <p>设为 0（默认）或更小值表示不限制。
   *
   * <p>https://github.com/facebook/rocksdb/wiki/Rate-Limiter
   */
  private int ioRateBytesPerSecond = DEFAULT_IO_RATE_BYTES_PER_SECOND;

  private int maxBackgroundJobs = ADAPTIVE_MAX_BACKGROUND_JOBS;

  private double blockCacheMemoryRatio = DEFAULT_BLOCK_CACHE_MEMORY_RATIO;

  private int blockCacheShardBits = DEFAULT_BLOCK_CACHE_SHARD_BITS;

  public RocksdbConfiguration() {}

  public Properties getColumnFamilyOptions() {
    return columnFamilyOptions;
  }

  public RocksdbConfiguration setColumnFamilyOptions(final Properties columnFamilyOptions) {
    this.columnFamilyOptions = columnFamilyOptions;
    return this;
  }

  public boolean isStatisticsEnabled() {
    return statisticsEnabled;
  }

  public RocksdbConfiguration setStatisticsEnabled(final boolean statisticsEnabled) {
    this.statisticsEnabled = statisticsEnabled;
    return this;
  }

  public long getMemoryLimit() {
    return memoryLimit;
  }

  public RocksdbConfiguration setMemoryLimit(final long memoryLimit) {
    this.memoryLimit = memoryLimit;
    return this;
  }

  public int getMaxOpenFiles() {
    return maxOpenFiles;
  }

  public RocksdbConfiguration setMaxOpenFiles(final int maxOpenFiles) {
    this.maxOpenFiles = maxOpenFiles;
    return this;
  }

  public int getMaxWriteBufferNumber() {
    return maxWriteBufferNumber;
  }

  public RocksdbConfiguration setMaxWriteBufferNumber(final int maxWriteBufferNumber) {
    this.maxWriteBufferNumber = maxWriteBufferNumber;
    return this;
  }

  public int getMinWriteBufferNumberToMerge() {
    return minWriteBufferNumberToMerge;
  }

  public RocksdbConfiguration setMinWriteBufferNumberToMerge(
      final int minWriteBufferNumberToMerge) {
    this.minWriteBufferNumberToMerge = minWriteBufferNumberToMerge;
    return this;
  }

  public int getIoRateBytesPerSecond() {
    return ioRateBytesPerSecond;
  }

  public RocksdbConfiguration setIoRateBytesPerSecond(final int ioRateBytesPerSecond) {
    this.ioRateBytesPerSecond = ioRateBytesPerSecond;
    return this;
  }

  public boolean isWalDisabled() {
    return walDisabled;
  }

  public RocksdbConfiguration setWalDisabled(final boolean walDisabled) {
    this.walDisabled = walDisabled;
    return this;
  }

  public boolean isSstPartitioningEnabled() {
    return sstPartitioningEnabled;
  }

  public RocksdbConfiguration setSstPartitioningEnabled(final boolean sstPartitioningEnabled) {
    this.sstPartitioningEnabled = sstPartitioningEnabled;
    return this;
  }

  /**
   * 返回配置的后台任务数上限；若仍为 {@link #ADAPTIVE_MAX_BACKGROUND_JOBS} 哨兵值，则返回 {@link
   * #adaptiveMaxBackgroundJobs()} 的 CPU 自适应结果。
   */
  public int getMaxBackgroundJobs() {
    return maxBackgroundJobs > 0 ? maxBackgroundJobs : adaptiveMaxBackgroundJobs();
  }

  public RocksdbConfiguration setMaxBackgroundJobs(final int maxBackgroundJobs) {
    this.maxBackgroundJobs = maxBackgroundJobs;
    return this;
  }

  public double getBlockCacheMemoryRatio() {
    return blockCacheMemoryRatio;
  }

  public RocksdbConfiguration setBlockCacheMemoryRatio(final double blockCacheMemoryRatio) {
    this.blockCacheMemoryRatio = blockCacheMemoryRatio;
    return this;
  }

  public int getBlockCacheShardBits() {
    return blockCacheShardBits;
  }

  public RocksdbConfiguration setBlockCacheShardBits(final int blockCacheShardBits) {
    this.blockCacheShardBits = blockCacheShardBits;
    return this;
  }
}
