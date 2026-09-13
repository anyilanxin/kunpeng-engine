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
package com.anyilanxin.kunpeng.configuration.broker;

import com.anyilanxin.kunpeng.configuration.broker.rocksdb.AccessMetricsConfiguration;
import com.anyilanxin.kunpeng.configuration.broker.rocksdb.RocksdbConfiguration;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Properties;
import java.util.regex.Pattern;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import org.springframework.util.unit.DataSize;

/**
 * Broker 的 RocksDB 存储配置项，聚合列族选项、内存与后台线程等参数。
 *
 * @author zxuanhong
 * @since
 */
@Getter
@Setter
@ToString
@EqualsAndHashCode
public final class RocksdbCfg implements ConfigurationEntry {

  private Properties columnFamilyOptions;
  private boolean enableStatistics = RocksdbConfiguration.DEFAULT_STATISTICS_ENABLED;
  private AccessMetricsConfiguration.Kind accessMetrics = AccessMetricsConfiguration.Kind.NONE;
  private DataSize memoryLimit = DataSize.ofBytes(RocksdbConfiguration.DEFAULT_MEMORY_LIMIT);
  private int maxOpenFiles = RocksdbConfiguration.DEFAULT_UNLIMITED_MAX_OPEN_FILES;
  private int maxWriteBufferNumber = RocksdbConfiguration.DEFAULT_MAX_WRITE_BUFFER_NUMBER;
  private int minWriteBufferNumberToMerge =
      RocksdbConfiguration.DEFAULT_MIN_WRITE_BUFFER_NUMBER_TO_MERGE;
  private int ioRateBytesPerSecond = RocksdbConfiguration.DEFAULT_IO_RATE_BYTES_PER_SECOND;
  private boolean disableWal = RocksdbConfiguration.DEFAULT_WAL_DISABLED;
  private boolean enableSstPartitioning = RocksdbConfiguration.DEFAULT_SST_PARTITIONING_ENABLED;
  private ConsistencyCheckCfg consistencyChecks = new ConsistencyCheckCfg();

  /**
   * RocksDB 后台任务数上限（flush + compaction 线程）。默认 {@code 0}，表示通过 {@link
   * RocksdbConfiguration#adaptiveMaxBackgroundJobs()} 按 CPU 自适应，通常为 {@code max(4, 核数/2)}。
   *
   * <p>当 RocksDB 需要与其他 CPU 消耗方（JVM GC、gRPC Netty 事件循环、Actor 调度器、集群消息）共享主机时，应显式设置为正值。例如在 4
   * 核小虚拟机上，扣除其余组件后自适应默认的 2 个后台任务可能已经偏多，此时应显式设置以预留余量。
   */
  private int maxBackgroundJobs = RocksdbConfiguration.ADAPTIVE_MAX_BACKGROUND_JOBS;

  /**
   * LRU block cache 的分片位数。缓存会被拆分为 {@code 2^bits} 个分片以降低锁竞争。默认 {@code 8}（256 个分片）在 2GB
   * 以内的缓存下都适用；缓存非常大或并发读很高时调大，缓存很小时调小以避免分片本身的开销。
   */
  private int blockCacheShardBits = RocksdbConfiguration.DEFAULT_BLOCK_CACHE_SHARD_BITS;

  /**
   * {@link #memoryLimit} 中分配给 block cache 的比例（其余给 memtable）。默认 {@code 1.0/3.0}（约 0.333），与 RocksDB
   * 官方建议一致。读多写少的场景可向 {@code 0.5} 调高；写多读少、memtable 预算更重要的场景可向 {@code 0.2} 调低。
   */
  private double blockCacheMemoryRatio = RocksdbConfiguration.DEFAULT_BLOCK_CACHE_MEMORY_RATIO;

  @Override
  public void init(final BrokerCfg globalConfig, final String brokerBase) {
    if (columnFamilyOptions == null) {
      // 惰性初始化，避免使用时还要判空
      columnFamilyOptions = new Properties();
    } else {
      // columnFamilyOptions 可能（部分）来自环境变量，需要先对其中的条目做转换处理
      columnFamilyOptions = initColumnFamilyOptions(columnFamilyOptions);
    }
  }

  /** 转换列族选项：将环境变量形式的关键键名归一化为合法属性名 */
  private static Properties initColumnFamilyOptions(final Properties original) {
    final var result = new Properties();
    original.entrySet().stream()
        .map(RocksDBColumnFamilyOption::new)
        .forEach(entry -> result.put(entry.key, entry.value));
    return result;
  }

  /** 基于当前配置构建 rocksdb 模块使用的 {@link RocksdbConfiguration} */
  public RocksdbConfiguration createRocksDbConfiguration() {
    return new RocksdbConfiguration()
        .setColumnFamilyOptions(columnFamilyOptions)
        .setMaxOpenFiles(maxOpenFiles)
        .setMaxWriteBufferNumber(maxWriteBufferNumber)
        .setMemoryLimit(memoryLimit.toBytes())
        .setMinWriteBufferNumberToMerge(minWriteBufferNumberToMerge)
        .setStatisticsEnabled(enableStatistics)
        .setIoRateBytesPerSecond(ioRateBytesPerSecond)
        .setWalDisabled(disableWal)
        .setSstPartitioningEnabled(enableSstPartitioning)
        .setMaxBackgroundJobs(maxBackgroundJobs)
        .setBlockCacheShardBits(blockCacheShardBits)
        .setBlockCacheMemoryRatio(blockCacheMemoryRatio);
  }

  private static final class RocksDBColumnFamilyOption {

    private static final Pattern DOT_CHAR_PATTERN = Pattern.compile("\\.");
    private static final String UNDERSCORE_CHAR = "_";
    private final String key;
    private final Object value;

    private RocksDBColumnFamilyOption(final Entry<Object, Object> entry) {
      Objects.requireNonNull(entry.getKey());
      // 以环境变量方式提供时，键名中可能带有 '.'，需要统一替换为 '_' 以匹配合法的属性名。
      // 例如：`KUNPENG_BROKER_ROCKSDB_COLUMNFAMILYOPTIONS_WRITE_BUFFER_SIZE=8388608`
      // 会解析出 `write.buffer.size`，而合法属性名应为 `write_buffer_size`。
      key = replaceAllDotCharsWithUnderscore(entry.getKey().toString());
      value = entry.getValue(); // 值无需转换
    }

    private static String replaceAllDotCharsWithUnderscore(final String key) {
      return DOT_CHAR_PATTERN.matcher(key).replaceAll(UNDERSCORE_CHAR);
    }
  }
}
