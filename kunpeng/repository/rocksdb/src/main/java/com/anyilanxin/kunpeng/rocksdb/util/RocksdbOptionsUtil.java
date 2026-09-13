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
package com.anyilanxin.kunpeng.rocksdb.util;

import com.anyilanxin.kunpeng.configuration.broker.rocksdb.RocksdbConfiguration;
import com.anyilanxin.kunpeng.kvstore.PredefinedColumnFamily;
import com.anyilanxin.kunpeng.rocksdb.RocksDBMetricsBinder;
import com.anyilanxin.kunpeng.rocksdb.RocksdbOptions;
import io.micrometer.core.instrument.MeterRegistry;
import java.time.Duration;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import org.rocksdb.*;

/**
 * RocksDB 配置选项工具类，根据 {@link RocksdbConfiguration} 构建 DBOptions、 ColumnFamilyOptions 以及读写选项
 *
 * @author zxuanhong
 */
public class RocksdbOptionsUtil {

  private RocksdbOptionsUtil() {}

  /**
   * 准备打开数据库所需的全部 RocksDB 配置项，并把各配置对象登记到 closeables 以便关闭时释放
   *
   * @param closeables 待登记可关闭资源的列表
   * @param registry 指标注册表，可为 null
   * @param partitionId 分区 id，用于指标命名
   * @param rocksDbConfiguration RocksDB 配置
   * @param allFamilies 上层声明使用的实体列族，只为这些列族创建选项
   * @return 组装好的 RocksDB 配置选项
   */
  public static RocksdbOptions prepareOptions(
      final List<AutoCloseable> closeables,
      final MeterRegistry registry,
      final int partitionId,
      final RocksdbConfiguration rocksDbConfiguration,
      final PredefinedColumnFamily[] allFamilies) {
    // 所有列族共享同一个 block cache，避免缓存内存随列族数量成倍放大
    final var blockCache = createBlockCache(closeables, rocksDbConfiguration);
    // default 列族是 RocksDB 打开数据库的必需列族，即使上层未声明也始终创建选项
    final var neededFamilies = EnumSet.of(PredefinedColumnFamily.DEFAULT_COLUMN_FAMILY);
    Collections.addAll(neededFamilies, allFamilies);
    // column family 选项必须最后关闭
    final Map<Integer, ColumnFamilyOptions> cfOptions = new HashMap<>();
    for (final PredefinedColumnFamily family : neededFamilies) {
      final var options =
          createColumnFamilyOptions(family, closeables, rocksDbConfiguration, blockCache);
      closeables.add(options);
      cfOptions.put(family.getFamily(), options);
    }
    final var dbOptions =
        createDefaultDbOptions(closeables, registry, partitionId, rocksDbConfiguration);
    closeables.add(dbOptions);

    final var writeOptions = createWriteOptions(rocksDbConfiguration);
    final var prefixReadOptions = createPrefixReadOptions();
    final var defaultReadOptions = createDefaultReadOptions();
    closeables.add(writeOptions);
    closeables.add(defaultReadOptions);
    closeables.add(prefixReadOptions);
    return new RocksdbOptions(
        dbOptions, cfOptions, writeOptions, prefixReadOptions, defaultReadOptions);
  }

  /** 创建所有列族共享的 LRU block cache */
  private static LRUCache createBlockCache(
      final List<AutoCloseable> closeables, final RocksdbConfiguration rocksDbConfiguration) {
    final var totalMemoryBudget = rocksDbConfiguration.getMemoryLimit();
    // RocksDB 推荐的比例，可再调整；注意索引和过滤器也会缓存在 block cache 中，无需额外为其
    // 预留内存
    final var blockCacheMemory =
        (long) (totalMemoryBudget * rocksDbConfiguration.getBlockCacheMemoryRatio());
    // 可通过 perf context 检查是否频繁阻塞在 block cache 互斥锁上，若是则需要增加分片数
    // （分片数 == 2^shardBits）
    final var cache =
        new LRUCache(blockCacheMemory, rocksDbConfiguration.getBlockCacheShardBits(), false, 0.15);
    closeables.add(cache);
    return cache;
  }

  private static WriteOptions createWriteOptions(final RocksdbConfiguration rocksDbConfiguration) {
    final var writeOptions = new WriteOptions().setDisableWAL(rocksDbConfiguration.isWalDisabled());
    // WAL 启用时（如多列族一致性场景）优先异步刷盘——持久性由 log-streams 层的 Raft 复制保证；
    // 每次写入都 fsync 没有必要。
    if (!rocksDbConfiguration.isWalDisabled()) {
      writeOptions.setSync(false);
    }
    return writeOptions;
  }

  private static ReadOptions createPrefixReadOptions() {
    return new ReadOptions()
        .setPrefixSameAsStart(true)
        .setTotalOrderSeek(false)
        // 设置正的预读大小仅在使用高延迟网络存储时才有意义，代价是迭代器更耗内存和 CPU
        .setReadaheadSize(0);
  }

  private static ReadOptions createDefaultReadOptions() {
    return new ReadOptions();
  }

  /**
   * 创建列族选项；用户提供了列族选项时优先使用用户选项
   *
   * <p>family: global-全局，local-本地，default-本地不可迁移（仅存本分区）
   *
   * @return 应用于列族的选项
   */
  private static ColumnFamilyOptions createColumnFamilyOptions(
      final PredefinedColumnFamily family,
      final List<AutoCloseable> closeables,
      final RocksdbConfiguration rocksDbConfiguration,
      final LRUCache blockCache) {
    final var userProvidedColumnFamilyOptions = rocksDbConfiguration.getColumnFamilyOptions();
    if (!userProvidedColumnFamilyOptions.isEmpty()) {
      return createFromUserOptions(userProvidedColumnFamilyOptions);
    }

    final var columnFamilyOptions = new ColumnFamilyOptions();

    final var totalMemoryBudget = rocksDbConfiguration.getMemoryLimit();
    // 与 createBlockCache 相同的缓存容量口径，用于从总预算扣除缓存后推导 memtable 预算
    final var blockCacheMemory =
        (long) (totalMemoryBudget * rocksDbConfiguration.getBlockCacheMemoryRatio());
    // memtable 的刷盘是异步的，因此内存中可能同时存在多个 memtable，但只有一个可写。memtable
    // 过多时写入会被暂停。由于前缀迭代是最常用路径，每个 memtable 会额外构建一个过滤器，这部分
    // 内存需要计入 memtable 的内存预算
    final var maxConcurrentMemtableCount = rocksDbConfiguration.getMaxWriteBufferNumber();
    // 目前是经验值，可进一步调优
    // 取值范围为 0 到 0.25（超过 0.25 会被截断为 0.25），这里取 0.15
    // prefix seek 必须足够快，因此从单个 memtable 预算中分配一部分额外内存，为每个 memtable
    // 构建过滤器，以便尽可能跳过无关前缀
    final var memtablePrefixFilterMemory = 0.15;
    final var memtableMemory =
        Math.round(
            ((totalMemoryBudget - blockCacheMemory) / (double) maxConcurrentMemtableCount)
                * (1 - memtablePrefixFilterMemory));

    final var tableConfig = createTableFormatConfig(blockCache, closeables);

    if (rocksDbConfiguration.isSstPartitioningEnabled()) {
      columnFamilyOptions.setSstPartitionerFactory(
          new SstPartitionerFixedPrefixFactory(Integer.BYTES));
    }

    // 按列族类别选择深层（L3 及以上）压缩算法与层级容量：
    // 全局列族数据量大、读取频率相对低，用 ZSTD 换取更高压缩比；本地列族读多，用低 CPU
    // 开销的 LZ4
    final CompressionType deepLevelCompression;
    final long maxBytesForLevelBase;
    final long targetFileSizeBase;
    switch (family) {
      // 全局：L1 = 64mb，L2 = 640mb，L3 = 6.4Gb，L4 >= 6.4Gb；L1 => 64Mb
      case GLOBAL_COLUMN_FAMILY -> {
        deepLevelCompression = CompressionType.ZSTD_COMPRESSION;
        maxBytesForLevelBase = 64 * 1024 * 1024L;
        targetFileSizeBase = 64 * 1024 * 1024L;
      }
      // 本地：L1 = 32mb，L2 = 320mb，L3 = 3.2Gb，L4 >= 3.2Gb；L1 => 8Mb
      case LOCAL_COLUMN_FAMILY -> {
        deepLevelCompression = CompressionType.LZ4_COMPRESSION;
        maxBytesForLevelBase = 32 * 1024 * 1024L;
        targetFileSizeBase = 8 * 1024 * 1024L;
      }
      // default（本地不可迁移）：层级容量与全局一致，压缩用 LZ4
      default -> {
        deepLevelCompression = CompressionType.LZ4_COMPRESSION;
        maxBytesForLevelBase = 64 * 1024 * 1024L;
        targetFileSizeBase = 64 * 1024 * 1024L;
      }
    }

    return columnFamilyOptions
        // 提取列族类型（用作前缀）以加快 seek
        .useFixedLengthPrefixExtractor(Integer.BYTES)
        .setMemtablePrefixBloomSizeRatio(memtablePrefixFilterMemory)
        // memtable 相关
        // 每个 L0 文件至少合并 3 个 memtable，否则所有 memtable 会刷成独立文件
        // 这也是调优候选项，当前只是粗略估计
        .setMinWriteBufferNumberToMerge(rocksDbConfiguration.getMinWriteBufferNumberToMerge())
        .setMaxWriteBufferNumber(maxConcurrentMemtableCount)
        .setWriteBufferSize(memtableMemory)
        // compaction
        .setLevelCompactionDynamicLevelBytes(true)
        .setCompactionPriority(CompactionPriority.OldestSmallestSeqFirst)
        .setCompactionStyle(CompactionStyle.LEVEL)
        // L0 存放刚刷盘的 memtable
        .setLevel0FileNumCompactionTrigger(maxConcurrentMemtableCount)
        .setLevel0SlowdownWritesTrigger(
            maxConcurrentMemtableCount + (maxConcurrentMemtableCount / 2))
        .setLevel0StopWritesTrigger(maxConcurrentMemtableCount * 2)
        // 配置 4 层。L1、L2 不压缩，L3 及以上使用低 CPU 开销的压缩算法。压缩块存于 OS 页缓存，
        // 未压缩块存于共享的 LRUCache。注意 L0 始终不压缩
        .setNumLevels(4)
        .setMaxBytesForLevelBase(maxBytesForLevelBase)
        .setMaxBytesForLevelMultiplier(10)
        .setCompressionPerLevel(
            List.of(
                CompressionType.NO_COMPRESSION,
                CompressionType.NO_COMPRESSION,
                deepLevelCompression,
                deepLevelCompression))
        // compaction 的目标文件大小。
        // 定义各层的期望 SST 文件大小（不保证，通常会偏小）
        // L0 是被合并与刷盘的部分，例如 3 个 memtable 合并为 X；目标文件大小与倍率针对 L1 及
        // 以上层级，基准值见上方按列族类别的取值。
        // 层级越大，需要在文件数量与单文件大小之间取得良好平衡
        // https://github.com/facebook/rocksdb/blob/fd0d35d390e212b617e90d7567102d3e5fd1c706/include/rocksdb/advanced_options.h#L417-L429
        .setTargetFileSizeBase(targetFileSizeBase)
        .setTargetFileSizeMultiplier(2)
        // 其他
        .setTableFormatConfig(tableConfig);
  }

  private static TableFormatConfig createTableFormatConfig(
      final LRUCache blockCache, final List<AutoCloseable> closeables) {
    final var filter = new BloomFilter(10, false);
    closeables.add(filter);

    return new BlockBasedTableConfig()
        .setBlockCache(blockCache)
        // 增大 block 大小可降低内存占用，但会增加读 IOPS
        .setBlockSize(32 * 1024L)
        // full 与 partitioned 过滤器在 format 5 下使用更高效的 bloom filter 实现
        .setFormatVersion(5)
        .setFilterPolicy(filter)
        // 缓存并 pin 索引和过滤器对 memtable 较多时保持读/seek 速度很重要，pin 住可确保它们
        // 不会被从 block cache 中淘汰
        .setCacheIndexAndFilterBlocks(true)
        .setPinL0FilterAndIndexBlocksInCache(true)
        .setCacheIndexAndFilterBlocksWithHighPriority(true)
        // 默认是二分查找，但本模块的扫描都基于前缀，更适合高效哈希
        .setIndexType(IndexType.kHashSearch)
        .setDataBlockIndexType(DataBlockIndexType.kDataBlockBinaryAndHash)
        // RocksDB 开发基准显示该值在 0.5 到 1 之间有收益，先取中间值，后续再优化
        .setDataBlockHashTableUtilRatio(0.75)
        // 虽然主要关注前缀，但前缀已由 setMemtablePrefixBloomSizeRatio 单独建索引覆盖；
        // 将完整 key 保留在过滤器中对高效 get 仍有意义，可视为两级索引
        .setWholeKeyFiltering(true);
  }

  private static ColumnFamilyOptions createFromUserOptions(
      final Properties userProvidedColumnFamilyOptions) {
    final var columnFamilyOptions =
        ColumnFamilyOptions.getColumnFamilyOptionsFromProps(userProvidedColumnFamilyOptions);
    if (columnFamilyOptions == null) {
      throw new IllegalStateException(
          String.format(
              "Expected to create column family options for RocksDB, "
                  + "but one or many values are undefined in the context of RocksDB "
                  + "[User-provided ColumnFamilyOptions: %s]. "
                  + "See RocksDB's cf_options.h and options_helper.cc for available keys and values.",
              userProvidedColumnFamilyOptions));
    }
    return columnFamilyOptions;
  }

  private static DBOptions createDefaultDbOptions(
      final List<AutoCloseable> closeables,
      final MeterRegistry registry,
      final int partitionId,
      final RocksdbConfiguration rocksDbConfiguration) {
    final var props = new Properties();
    props.put("file_checksum_gen_factory", "FileChecksumGenCrc32cFactory");
    //    启用全量文件校验和

    final var dbOptions =
        DBOptions.getDBOptionsFromProps(props)
            .setErrorIfExists(false)
            .setCreateIfMissing(true)
            .setParanoidChecks(true)
            .setMaxOpenFiles(rocksDbConfiguration.getMaxOpenFiles())
            // 默认按 CPU 自适应，除非用户通过 broker.rocksdb.max-background-jobs 覆盖
            // （0 为哨兵值 → max(4, cores/2)）
            .setMaxBackgroundJobs(rocksDbConfiguration.getMaxBackgroundJobs())
            // 只使用默认列族
            .setCreateMissingColumnFamilies(true)
            // WAL 禁用时可能不必要，但仍建议开启，以避免产生大量小 SST 文件
            .setAvoidFlushDuringRecovery(true)
            // 限制 manifest（记录所有操作）的大小，否则会无限增长
            .setMaxManifestFileSize(256 * 1024 * 1024L)
            // 保留 1 小时的日志——完全任意的选择。应在性能收益与复制数据量之间取得平衡
            .setLogFileTimeToRoll(Duration.ofMinutes(30).toSeconds())
            .setKeepLogFileNum(2)
            // 默认禁用 WAL（见 RocksdbConfiguration#walDisabled）；不配置 WAL 归档，避免产生空的
            // 'archive' 目录和不必要的开销。
            .setWalTtlSeconds(0);

    // 限制 I/O 写入
    if (rocksDbConfiguration.getIoRateBytesPerSecond() > 0) {
      final RateLimiter rateLimiter =
          new RateLimiter(rocksDbConfiguration.getIoRateBytesPerSecond());
      dbOptions.setRateLimiter(rateLimiter);
    }

    if (rocksDbConfiguration.isStatisticsEnabled()) {
      final var statistics = new Statistics();
      if (registry != null) {
        final var metricsBinder = new RocksDBMetricsBinder(statistics, registry, partitionId);
        closeables.add(metricsBinder);
      }
      closeables.add(statistics);
      statistics.setStatsLevel(StatsLevel.ALL);
      dbOptions
          .setStatistics(statistics)
          // 加快数据库打开速度
          .setSkipStatsUpdateOnDbOpen(true)
          // 不做性能分析时可关闭
          .setStatsDumpPeriodSec(20);
    }
    return dbOptions;
  }
}
