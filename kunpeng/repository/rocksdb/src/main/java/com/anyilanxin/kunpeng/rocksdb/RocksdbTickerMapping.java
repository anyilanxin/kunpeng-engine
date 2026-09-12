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

import org.rocksdb.RocksDB;
import org.rocksdb.StatsLevel;
import org.rocksdb.TickerType;

/**
 * RocksDB ticker 与指标名称的映射枚举，用于将 RocksDB 的 TickerType 统计项注册为监控指标
 *
 * @author zxuanhong
 */
public enum RocksdbTickerMapping {
  BLOCK_CACHE_MISS("block.cache.miss", TickerType.BLOCK_CACHE_MISS),

  /**
   * block cache 总命中数
   *
   * <p>要求：BLOCK_CACHE_HIT == BLOCK_CACHE_INDEX_HIT + BLOCK_CACHE_FILTER_HIT + BLOCK_CACHE_DATA_HIT；
   */
  BLOCK_CACHE_HIT("block.cache.hit", TickerType.BLOCK_CACHE_HIT),

  BLOCK_CACHE_ADD("block.cache.add", TickerType.BLOCK_CACHE_ADD),

  /** 向 block cache 添加 block 失败的次数。 */
  BLOCK_CACHE_ADD_FAILURES("block.cache.add.failures", TickerType.BLOCK_CACHE_ADD_FAILURES),

  /** 从 block cache 访问索引 block 时未命中的次数。 */
  BLOCK_CACHE_INDEX_MISS("block.cache.index.miss", TickerType.BLOCK_CACHE_INDEX_MISS),

  /** 从 block cache 访问索引 block 时命中的次数。 */
  BLOCK_CACHE_INDEX_HIT("block.cache.index.hit", TickerType.BLOCK_CACHE_INDEX_HIT),

  /** 添加到 block cache 的索引 block 数量。 */
  BLOCK_CACHE_INDEX_ADD("block.cache.index.add", TickerType.BLOCK_CACHE_INDEX_ADD),

  /** 插入缓存中的索引 block 字节数 */
  BLOCK_CACHE_INDEX_BYTES_INSERT(
      "block.cache.index.bytes.insert", TickerType.BLOCK_CACHE_INDEX_BYTES_INSERT),

  /** 从 block cache 访问过滤器 block 时未命中的次数。 */
  BLOCK_CACHE_FILTER_MISS("block.cache.filter.miss", TickerType.BLOCK_CACHE_FILTER_MISS),

  /** 从 block cache 访问过滤器 block 时命中的次数。 */
  BLOCK_CACHE_FILTER_HIT("block.cache.filter.hit", TickerType.BLOCK_CACHE_FILTER_HIT),

  /** 添加到 block cache 的过滤器 block 数量。 */
  BLOCK_CACHE_FILTER_ADD("block.cache.filter.add", TickerType.BLOCK_CACHE_FILTER_ADD),

  /** 插入缓存中的 bloom filter block 字节数 */
  BLOCK_CACHE_FILTER_BYTES_INSERT(
      "block.cache.filter.bytes.insert", TickerType.BLOCK_CACHE_FILTER_BYTES_INSERT),

  /** 从 block cache 访问数据 block 时未命中的次数。 */
  BLOCK_CACHE_DATA_MISS("block.cache.data.miss", TickerType.BLOCK_CACHE_DATA_MISS),

  /** 从 block cache 访问数据 block 时命中的次数。 */
  BLOCK_CACHE_DATA_HIT("block.cache.data.hit", TickerType.BLOCK_CACHE_DATA_HIT),

  /** 添加到 block cache 的数据 block 数量。 */
  BLOCK_CACHE_DATA_ADD("block.cache.data.add", TickerType.BLOCK_CACHE_DATA_ADD),

  /** 插入缓存中的数据 block 字节数 */
  BLOCK_CACHE_DATA_BYTES_INSERT(
      "block.cache.data.bytes.insert", TickerType.BLOCK_CACHE_DATA_BYTES_INSERT),

  /** 从缓存读取的字节数。 */
  BLOCK_CACHE_BYTES_READ("block.cache.bytes.read", TickerType.BLOCK_CACHE_BYTES_READ),

  /** 写入缓存的字节数。 */
  BLOCK_CACHE_BYTES_WRITE("block.cache.bytes.write", TickerType.BLOCK_CACHE_BYTES_WRITE),

  /** 压缩字典相关的 block cache 统计 */
  BLOCK_CACHE_COMPRESSION_DICT_MISS(
      "block.cache.compression.dict.miss", TickerType.BLOCK_CACHE_COMPRESSION_DICT_MISS),
  BLOCK_CACHE_COMPRESSION_DICT_HIT(
      "block.cache.compression.dict.hit", TickerType.BLOCK_CACHE_COMPRESSION_DICT_HIT),
  BLOCK_CACHE_COMPRESSION_DICT_ADD(
      "block.cache.compression.dict.add", TickerType.BLOCK_CACHE_COMPRESSION_DICT_ADD),
  BLOCK_CACHE_COMPRESSION_DICT_BYTES_INSERT(
      "block.cache.compression.dict.bytes.insert",
      TickerType.BLOCK_CACHE_COMPRESSION_DICT_BYTES_INSERT),

  /** 向 block cache 重复添加的次数 */
  BLOCK_CACHE_ADD_REDUNDANT("block.cache.add.redundant", TickerType.BLOCK_CACHE_ADD_REDUNDANT),
  BLOCK_CACHE_INDEX_ADD_REDUNDANT(
      "block.cache.index.add.redundant", TickerType.BLOCK_CACHE_INDEX_ADD_REDUNDANT),
  BLOCK_CACHE_FILTER_ADD_REDUNDANT(
      "block.cache.filter.add.redundant", TickerType.BLOCK_CACHE_FILTER_ADD_REDUNDANT),
  BLOCK_CACHE_DATA_ADD_REDUNDANT(
      "block.cache.data.add.redundant", TickerType.BLOCK_CACHE_DATA_ADD_REDUNDANT),
  BLOCK_CACHE_COMPRESSION_DICT_ADD_REDUNDANT(
      "block.cache.compression.dict.add.redundant",
      TickerType.BLOCK_CACHE_COMPRESSION_DICT_ADD_REDUNDANT),

  /** 二级缓存命中次数 */
  SECONDARY_CACHE_HITS("secondary.cache.hits", TickerType.SECONDARY_CACHE_HITS),
  SECONDARY_CACHE_FILTER_HITS(
      "secondary.cache.filter.hits", TickerType.SECONDARY_CACHE_FILTER_HITS),
  SECONDARY_CACHE_INDEX_HITS("secondary.cache.index.hits", TickerType.SECONDARY_CACHE_INDEX_HITS),
  SECONDARY_CACHE_DATA_HITS("secondary.cache.data.hits", TickerType.SECONDARY_CACHE_DATA_HITS),

  COMPRESSED_SECONDARY_CACHE_DUMMY_HITS(
      "compressed.secondary.cache.dummy.hits", TickerType.COMPRESSED_SECONDARY_CACHE_DUMMY_HITS),
  COMPRESSED_SECONDARY_CACHE_HITS(
      "compressed.secondary.cache.hits", TickerType.COMPRESSED_SECONDARY_CACHE_HITS),
  COMPRESSED_SECONDARY_CACHE_PROMOTIONS(
      "compressed.secondary.cache.promotions", TickerType.COMPRESSED_SECONDARY_CACHE_PROMOTIONS),
  COMPRESSED_SECONDARY_CACHE_PROMOTION_SKIPS(
      "compressed.secondary.cache.promotion.skips",
      TickerType.COMPRESSED_SECONDARY_CACHE_PROMOTION_SKIPS),

  /** bloom filter 避免文件读取的次数。 */
  BLOOM_FILTER_USEFUL("bloom.filter.useful", TickerType.BLOOM_FILTER_USEFUL),

  /** bloom FullFilter 未能避免读取的次数。 */
  BLOOM_FILTER_FULL_POSITIVE("bloom.filter.full.positive", TickerType.BLOOM_FILTER_FULL_POSITIVE),

  /** bloom FullFilter 未能避免读取且数据确实存在的次数。 */
  BLOOM_FILTER_FULL_TRUE_POSITIVE(
      "bloom.filter.full.true.positive", TickerType.BLOOM_FILTER_FULL_TRUE_POSITIVE),

  /** 在文件上创建迭代器前检查 bloom 的次数，以及该检查成功避免创建迭代器（从而可能节省 IOPS） 的次数。 */
  BLOOM_FILTER_PREFIX_CHECKED(
      "bloom.filter.prefix.checked", TickerType.BLOOM_FILTER_PREFIX_CHECKED),
  BLOOM_FILTER_PREFIX_USEFUL("bloom.filter.prefix.useful", TickerType.BLOOM_FILTER_PREFIX_USEFUL),
  BLOOM_FILTER_PREFIX_TRUE_POSITIVE(
      "bloom.filter.prefix.true.positive", TickerType.BLOOM_FILTER_PREFIX_TRUE_POSITIVE),

  /** 持久缓存命中次数 */
  PERSISTENT_CACHE_HIT("persistent.cache.hit", TickerType.PERSISTENT_CACHE_HIT),

  /** 持久缓存未命中次数 */
  PERSISTENT_CACHE_MISS("persistent.cache.miss", TickerType.PERSISTENT_CACHE_MISS),

  /** 模拟 block cache 总命中次数 */
  SIM_BLOCK_CACHE_HIT("sim.block.cache.hit", TickerType.SIM_BLOCK_CACHE_HIT),

  /** 模拟 block cache 总未命中次数 */
  SIM_BLOCK_CACHE_MISS("sim.block.cache.miss", TickerType.SIM_BLOCK_CACHE_MISS),

  /** memtable 命中次数。 */
  MEMTABLE_HIT("memtable.hit", TickerType.MEMTABLE_HIT),

  /** memtable 未命中次数。 */
  MEMTABLE_MISS("memtable.miss", TickerType.MEMTABLE_MISS),

  /** 由 L0 处理的 Get() 查询数量 */
  GET_HIT_L0("get.hit.l0", TickerType.GET_HIT_L0),

  /** 由 L1 处理的 Get() 查询数量 */
  GET_HIT_L1("get.hit.l1", TickerType.GET_HIT_L1),

  /** 由 L2 及以上层级处理的 Get() 查询数量 */
  GET_HIT_L2_AND_UP("get.hit.l2.and.up", TickerType.GET_HIT_L2_AND_UP),

  /** COMPACTION_KEY_DROP_* 统计 compaction 期间 key 被丢弃的原因，目前共 4 种。 */

  /** 该 key 已被更新的值覆盖。 */
  COMPACTION_KEY_DROP_NEWER_ENTRY(
      "compaction.key.drop.newer.entry", TickerType.COMPACTION_KEY_DROP_NEWER_ENTRY),

  /** 也包括因 range del 丢弃的 key。该 key 已作废。 */
  COMPACTION_KEY_DROP_OBSOLETE(
      "compaction.key.drop.obsolete", TickerType.COMPACTION_KEY_DROP_OBSOLETE),

  /** 该 key 被范围墓碑（range tombstone）覆盖。 */
  COMPACTION_KEY_DROP_RANGE_DEL(
      "compaction.key.drop.range.del", TickerType.COMPACTION_KEY_DROP_RANGE_DEL),

  /** 用户 compaction 函数丢弃了该 key。 */
  COMPACTION_KEY_DROP_USER("compaction.key.drop.user", TickerType.COMPACTION_KEY_DROP_USER),

  /** 范围内的所有 key 均被删除。 */
  COMPACTION_RANGE_DEL_DROP_OBSOLETE(
      "compaction.range.del.drop.obsolete", TickerType.COMPACTION_RANGE_DEL_DROP_OBSOLETE),

  /** 因文件间隙优化，在到达最底层前作废的删除记录。 */
  COMPACTION_OPTIMIZED_DEL_DROP_OBSOLETE(
      "compaction.optimized.del.drop.obsolete", TickerType.COMPACTION_OPTIMIZED_DEL_DROP_OBSOLETE),

  /** 为防止磁盘空间不足（ENOSPC）而取消的 compaction 次数 */
  COMPACTION_CANCELLED("compaction.cancelled", TickerType.COMPACTION_CANCELLED),

  /** 通过 Put 与 Write 调用写入数据库的 key 数量。 */
  NUMBER_KEYS_WRITTEN("number.keys.written", TickerType.NUMBER_KEYS_WRITTEN),

  /** 读取的 key 数量。 */
  NUMBER_KEYS_READ("number.keys.read", TickerType.NUMBER_KEYS_READ),

  /** 更新的 key 数量，需启用原地更新（inplace update） */
  NUMBER_KEYS_UPDATED("number.keys.updated", TickerType.NUMBER_KEYS_UPDATED),

  /** DB::Put()、DB::Delete()、DB::Merge() 和 DB::Write() 产生的未压缩字节数。 */
  BYTES_WRITTEN("bytes.written", TickerType.BYTES_WRITTEN),

  /**
   * 从 DB::Get() 读取的未压缩字节数，来源可能是 memtable、缓存或表文件。
   *
   * <p>如需 DB::MultiGet() 读取的逻辑字节数，请使用 {@link #NUMBER_MULTIGET_BYTES_READ}。
   */
  BYTES_READ("bytes.read", TickerType.BYTES_READ),

  /** seek 调用次数。 */
  NUMBER_DB_SEEK("number.db.seek", TickerType.NUMBER_DB_SEEK),

  /** next 调用次数。 */
  NUMBER_DB_NEXT("number.db.next", TickerType.NUMBER_DB_NEXT),

  /** prev 调用次数。 */
  NUMBER_DB_PREV("number.db.prev", TickerType.NUMBER_DB_PREV),

  /** 返回了数据的 seek 调用次数。 */
  NUMBER_DB_SEEK_FOUND("number.db.seek.found", TickerType.NUMBER_DB_SEEK_FOUND),

  /** 返回了数据的 next 调用次数。 */
  NUMBER_DB_NEXT_FOUND("number.db.next.found", TickerType.NUMBER_DB_NEXT_FOUND),

  /** 返回了数据的 prev 调用次数。 */
  NUMBER_DB_PREV_FOUND("number.db.prev.found", TickerType.NUMBER_DB_PREV_FOUND),

  /** 迭代器读取的未压缩字节数，包含 key 与 value 的大小。 */
  ITER_BYTES_READ("iter.bytes.read", TickerType.ITER_BYTES_READ),

  /** 迭代期间跳过的内部记录数量 */
  NUMBER_ITER_SKIP("number.iter.skip", TickerType.NUMBER_ITER_SKIP),

  /** 迭代中为跳过大量相同 userkey 而不得不 reseek 的次数。 */
  NUMBER_OF_RESEEKS_IN_ITERATION(
      "number.of.reseeks.in.iteration", TickerType.NUMBER_OF_RESEEKS_IN_ITERATION),

  /** 创建的迭代器数量。 */
  NO_ITERATOR_CREATED("no.iterator.created", TickerType.NO_ITERATOR_CREATED),

  /** 删除的迭代器数量。 */
  NO_ITERATOR_DELETED("no.iterator.deleted", TickerType.NO_ITERATOR_DELETED),

  NO_FILE_OPENS("no.file.opens", TickerType.NO_FILE_OPENS),

  NO_FILE_ERRORS("no.file.errors", TickerType.NO_FILE_ERRORS),

  /** 写入线程等待 compaction 或 flush 完成的时间。 */
  STALL_MICROS("stall.micros", TickerType.STALL_MICROS),

  /**
   * 等待 db mutex 的时间。
   *
   * <p>默认禁用。如需启用，请将统计级别设为 {@link StatsLevel#ALL}
   */
  DB_MUTEX_WAIT_MICROS("db.mutex.wait.micros", TickerType.DB_MUTEX_WAIT_MICROS),

  /** MultiGet 调用次数。 */
  NUMBER_MULTIGET_CALLS("number.multiget.calls", TickerType.NUMBER_MULTIGET_CALLS),

  /** MultiGet 读取的 key 数量。 */
  NUMBER_MULTIGET_KEYS_READ("number.multiget.keys.read", TickerType.NUMBER_MULTIGET_KEYS_READ),

  /** MultiGet 读取的字节数量。 */
  NUMBER_MULTIGET_BYTES_READ("number.multiget.bytes.read", TickerType.NUMBER_MULTIGET_BYTES_READ),

  /** MultiGet 找到的 key 数量（相对请求的数量） */
  NUMBER_MULTIGET_KEYS_FOUND("number.multiget.keys.found", TickerType.NUMBER_MULTIGET_KEYS_FOUND),

  NUMBER_MERGE_FAILURES("number.merge.failures", TickerType.NUMBER_MERGE_FAILURES),

  /** 记录 {@link RocksDB#getUpdatesSince(long)} 的调用次数，便于追踪事务日志迭代器的刷新。 */
  GET_UPDATES_SINCE_CALLS("get.updates.since.calls", TickerType.GET_UPDATES_SINCE_CALLS),

  /** WAL 执行 sync 的次数。 */
  WAL_FILE_SYNCED("wal.file.synced", TickerType.WAL_FILE_SYNCED),

  /** 写入 WAL 的字节数。 */
  WAL_FILE_BYTES("wal.file.bytes", TickerType.WAL_FILE_BYTES),

  /** 写入可由请求线程自身处理，也可由 writers 队列头部的线程处理。 */
  WRITE_DONE_BY_SELF("write.done.by.self", TickerType.WRITE_DONE_BY_SELF),

  /** 等价于替其他线程完成的写入。 */
  WRITE_DONE_BY_OTHER("write.done.by.other", TickerType.WRITE_DONE_BY_OTHER),

  /** 请求 WAL 的 Write 调用数量。 */
  WRITE_WITH_WAL("write.with.wal", TickerType.WRITE_WITH_WAL),

  /** compaction 期间读取的字节数。 */
  COMPACT_READ_BYTES("compact.read.bytes", TickerType.COMPACT_READ_BYTES),

  /** compaction 期间写入的字节数。 */
  COMPACT_WRITE_BYTES("compact.write.bytes", TickerType.COMPACT_WRITE_BYTES),

  /** flush 期间写入的字节数。 */
  FLUSH_WRITE_BYTES("flush.write.bytes", TickerType.FLUSH_WRITE_BYTES),

  /** 按 compaction 触发原因细分的读写统计 */
  COMPACT_READ_BYTES_MARKED("compact.read.bytes.marked", TickerType.COMPACT_READ_BYTES_MARKED),
  COMPACT_READ_BYTES_PERIODIC(
      "compact.read.bytes.periodic", TickerType.COMPACT_READ_BYTES_PERIODIC),
  COMPACT_READ_BYTES_TTL("compact.read.bytes.ttl", TickerType.COMPACT_READ_BYTES_TTL),
  COMPACT_WRITE_BYTES_MARKED("compact.write.bytes.marked", TickerType.COMPACT_WRITE_BYTES_MARKED),
  COMPACT_WRITE_BYTES_PERIODIC(
      "compact.write.bytes.periodic", TickerType.COMPACT_WRITE_BYTES_PERIODIC),
  COMPACT_WRITE_BYTES_TTL("compact.write.bytes.ttl", TickerType.COMPACT_WRITE_BYTES_TTL),

  /** 直接从文件加载表属性（不创建 table reader 对象）的次数。 */
  NUMBER_DIRECT_LOAD_TABLE_PROPERTIES(
      "number.direct.load.table.properties", TickerType.NUMBER_DIRECT_LOAD_TABLE_PROPERTIES),
  NUMBER_SUPERVERSION_ACQUIRES(
      "number.superversion.acquires", TickerType.NUMBER_SUPERVERSION_ACQUIRES),
  NUMBER_SUPERVERSION_RELEASES(
      "number.superversion.releases", TickerType.NUMBER_SUPERVERSION_RELEASES),
  NUMBER_SUPERVERSION_CLEANUPS(
      "number.superversion.cleanups", TickerType.NUMBER_SUPERVERSION_CLEANUPS),

  /** 执行的压缩/解压次数 */
  NUMBER_BLOCK_COMPRESSED("number.block.compressed", TickerType.NUMBER_BLOCK_COMPRESSED),
  NUMBER_BLOCK_DECOMPRESSED("number.block.decompressed", TickerType.NUMBER_BLOCK_DECOMPRESSED),

  BYTES_COMPRESSED_FROM("bytes.compressed.from", TickerType.BYTES_COMPRESSED_FROM),
  BYTES_COMPRESSED_TO("bytes.compressed.to", TickerType.BYTES_COMPRESSED_TO),
  BYTES_COMPRESSION_BYPASSED("bytes.compression.bypassed", TickerType.BYTES_COMPRESSION_BYPASSED),
  BYTES_COMPRESSION_REJECTED("bytes.compression.rejected", TickerType.BYTES_COMPRESSION_REJECTED),
  NUMBER_BLOCK_COMPRESSION_BYPASSED(
      "number.block.compression.bypassed", TickerType.NUMBER_BLOCK_COMPRESSION_BYPASSED),
  NUMBER_BLOCK_COMPRESSION_REJECTED(
      "number.block.compression.rejected", TickerType.NUMBER_BLOCK_COMPRESSION_REJECTED),
  BYTES_DECOMPRESSED_FROM("bytes.decompressed.from", TickerType.BYTES_DECOMPRESSED_FROM),
  BYTES_DECOMPRESSED_TO("bytes.decompressed.to", TickerType.BYTES_DECOMPRESSED_TO),

  MERGE_OPERATION_TOTAL_TIME("merge.operation.total.time", TickerType.MERGE_OPERATION_TOTAL_TIME),
  FILTER_OPERATION_TOTAL_TIME(
      "filter.operation.total.time", TickerType.FILTER_OPERATION_TOTAL_TIME),
  COMPACTION_CPU_TOTAL_TIME("compaction.cpu.total.time", TickerType.COMPACTION_CPU_TOTAL_TIME),

  /** 行缓存。 */
  ROW_CACHE_HIT("row.cache.hit", TickerType.ROW_CACHE_HIT),
  ROW_CACHE_MISS("row.cache.miss", TickerType.ROW_CACHE_MISS),

  /**
   * 读放大统计。
   *
   * <p>读放大可按公式（READ_AMP_TOTAL_READ_BYTES / READ_AMP_ESTIMATE_USEFUL_BYTES）计算
   *
   * <p>要求：启用 ReadOptions::read_amp_bytes_per_bit
   */

  /** 实际使用字节数的估算值。 */
  READ_AMP_ESTIMATE_USEFUL_BYTES(
      "read.amp.estimate.useful.bytes", TickerType.READ_AMP_ESTIMATE_USEFUL_BYTES),

  /** 加载数据 block 的总大小。 */
  READ_AMP_TOTAL_READ_BYTES("read.amp.total.read.bytes", TickerType.READ_AMP_TOTAL_READ_BYTES),

  /** 在补充间隔内速率限制器字节被完全消耗的次数。 */
  NUMBER_RATE_LIMITER_DRAINS("number.rate.limiter.drains", TickerType.NUMBER_RATE_LIMITER_DRAINS),

  /** BlobDB 专属统计：对 BlobDB 的 Put/PutTTL/PutUntil 次数。 */
  BLOB_DB_NUM_PUT("blob.db.num.put", TickerType.BLOB_DB_NUM_PUT),

  /** 对 BlobDB 的 Write 次数。 */
  BLOB_DB_NUM_WRITE("blob.db.num.write", TickerType.BLOB_DB_NUM_WRITE),

  /** 对 BlobDB 的 Get 次数。 */
  BLOB_DB_NUM_GET("blob.db.num.get", TickerType.BLOB_DB_NUM_GET),

  /** 对 BlobDB 的 MultiGet 次数。 */
  BLOB_DB_NUM_MULTIGET("blob.db.num.multiget", TickerType.BLOB_DB_NUM_MULTIGET),

  /** 对 BlobDB 迭代器的 Seek/SeekToFirst/SeekToLast/SeekForPrev 次数。 */
  BLOB_DB_NUM_SEEK("blob.db.num.seek", TickerType.BLOB_DB_NUM_SEEK),

  /** 对 BlobDB 迭代器的 Next 次数。 */
  BLOB_DB_NUM_NEXT("blob.db.num.next", TickerType.BLOB_DB_NUM_NEXT),

  /** 对 BlobDB 迭代器的 Prev 次数。 */
  BLOB_DB_NUM_PREV("blob.db.num.prev", TickerType.BLOB_DB_NUM_PREV),

  /** 写入 BlobDB 的 key 数量。 */
  BLOB_DB_NUM_KEYS_WRITTEN("blob.db.num.keys.written", TickerType.BLOB_DB_NUM_KEYS_WRITTEN),

  /** 从 BlobDB 读取的 key 数量。 */
  BLOB_DB_NUM_KEYS_READ("blob.db.num.keys.read", TickerType.BLOB_DB_NUM_KEYS_READ),

  /** 写入 BlobDB 的字节数（key + value）。 */
  BLOB_DB_BYTES_WRITTEN("blob.db.bytes.written", TickerType.BLOB_DB_BYTES_WRITTEN),

  /** 从 BlobDB 读取的字节数（key + value）。 */
  BLOB_DB_BYTES_READ("blob.db.bytes.read", TickerType.BLOB_DB_BYTES_READ),

  /** BlobDB 以非 TTL 内联值写入的 key 数量。 */
  BLOB_DB_WRITE_INLINED("blob.db.write.inlined", TickerType.BLOB_DB_WRITE_INLINED),

  /** BlobDB 以 TTL 内联值写入的 key 数量。 */
  BLOB_DB_WRITE_INLINED_TTL("blob.db.write.inlined.ttl", TickerType.BLOB_DB_WRITE_INLINED_TTL),

  /** BlobDB 以非 TTL blob 值写入的 key 数量。 */
  BLOB_DB_WRITE_BLOB("blob.db.write.blob", TickerType.BLOB_DB_WRITE_BLOB),

  /** BlobDB 以 TTL blob 值写入的 key 数量。 */
  BLOB_DB_WRITE_BLOB_TTL("blob.db.write.blob.ttl", TickerType.BLOB_DB_WRITE_BLOB_TTL),

  /** 写入 blob 文件的字节数。 */
  BLOB_DB_BLOB_FILE_BYTES_WRITTEN(
      "blob.db.blob.file.bytes.written", TickerType.BLOB_DB_BLOB_FILE_BYTES_WRITTEN),

  /** 从 blob 文件读取的字节数。 */
  BLOB_DB_BLOB_FILE_BYTES_READ(
      "blob.db.blob.file.bytes.read", TickerType.BLOB_DB_BLOB_FILE_BYTES_READ),

  /** blob 文件执行 sync 的次数。 */
  BLOB_DB_BLOB_FILE_SYNCED("blob.db.blob.file.synced", TickerType.BLOB_DB_BLOB_FILE_SYNCED),

  /** 因过期被 BlobDB compaction 过滤器从基础 DB 中淘汰的 blob index 数量。 */
  BLOB_DB_BLOB_INDEX_EXPIRED_COUNT(
      "blob.db.blob.index.expired.count", TickerType.BLOB_DB_BLOB_INDEX_EXPIRED_COUNT),

  /** 因过期被 BlobDB compaction 过滤器从基础 DB 中淘汰的 blob index 大小。 */
  BLOB_DB_BLOB_INDEX_EXPIRED_SIZE(
      "blob.db.blob.index.expired.size", TickerType.BLOB_DB_BLOB_INDEX_EXPIRED_SIZE),

  /** 因对应文件被删除而被 BlobDB compaction 过滤器从基础 DB 中淘汰的 blob index 数量。 */
  BLOB_DB_BLOB_INDEX_EVICTED_COUNT(
      "blob.db.blob.index.evicted.count", TickerType.BLOB_DB_BLOB_INDEX_EVICTED_COUNT),

  /** 因对应文件被删除而被 BlobDB compaction 过滤器从基础 DB 中淘汰的 blob index 大小。 */
  BLOB_DB_BLOB_INDEX_EVICTED_SIZE(
      "blob.db.blob.index.evicted.size", TickerType.BLOB_DB_BLOB_INDEX_EVICTED_SIZE),

  /** 被垃圾回收的 blob 文件数量。 */
  BLOB_DB_GC_NUM_FILES("blob.db.gc.num.files", TickerType.BLOB_DB_GC_NUM_FILES),

  /** 垃圾回收生成的 blob 文件数量。 */
  BLOB_DB_GC_NUM_NEW_FILES("blob.db.gc.num.new.files", TickerType.BLOB_DB_GC_NUM_NEW_FILES),

  /** BlobDB 垃圾回收失败次数。 */
  BLOB_DB_GC_FAILURES("blob.db.gc.failures", TickerType.BLOB_DB_GC_FAILURES),

  /** 被垃圾回收迁移到新 blob 文件的 key 数量。 */
  BLOB_DB_GC_NUM_KEYS_RELOCATED(
      "blob.db.gc.num.keys.relocated", TickerType.BLOB_DB_GC_NUM_KEYS_RELOCATED),

  /** 被垃圾回收迁移到新 blob 文件的字节数。 */
  BLOB_DB_GC_BYTES_RELOCATED("blob.db.gc.bytes.relocated", TickerType.BLOB_DB_GC_BYTES_RELOCATED),

  /** 因 BlobDB 已满而被淘汰的 blob 文件数量。 */
  BLOB_DB_FIFO_NUM_FILES_EVICTED(
      "blob.db.fifo.num.files.evicted", TickerType.BLOB_DB_FIFO_NUM_FILES_EVICTED),

  /** 因 BlobDB 已满而被淘汰的 blob 文件中的 key 数量。 */
  BLOB_DB_FIFO_NUM_KEYS_EVICTED(
      "blob.db.fifo.num.keys.evicted", TickerType.BLOB_DB_FIFO_NUM_KEYS_EVICTED),

  /** 因 BlobDB 已满而被淘汰的 blob 文件中的字节数。 */
  BLOB_DB_FIFO_BYTES_EVICTED("blob.db.fifo.bytes.evicted", TickerType.BLOB_DB_FIFO_BYTES_EVICTED),

  /** 从 blob cache 访问 blob 时未命中的次数。 */
  BLOB_DB_CACHE_MISS("blob.db.cache.miss", TickerType.BLOB_DB_CACHE_MISS),

  /** 从 blob cache 访问 blob 时命中的次数。 */
  BLOB_DB_CACHE_HIT("blob.db.cache.hit", TickerType.BLOB_DB_CACHE_HIT),

  /** 添加到 blob cache 的数据 block 数量。 */
  BLOB_DB_CACHE_ADD("blob.db.cache.add", TickerType.BLOB_DB_CACHE_ADD),

  /** 向 blob cache 添加 blob 失败的次数。 */
  BLOB_DB_CACHE_ADD_FAILURES("blob.db.cache.add.failures", TickerType.BLOB_DB_CACHE_ADD_FAILURES),

  /** 从 blob cache 读取的字节数。 */
  BLOB_DB_CACHE_BYTES_READ("blob.db.cache.bytes.read", TickerType.BLOB_DB_CACHE_BYTES_READ),

  /** 写入 blob cache 的字节数。 */
  BLOB_DB_CACHE_BYTES_WRITE("blob.db.cache.bytes.write", TickerType.BLOB_DB_CACHE_BYTES_WRITE),

  /** 这些计数器反映 WritePrepared 事务的性能问题，正常情况下不应明显增长。快速路径下获取 prepare_mutex_ 的次数。 */
  TXN_PREPARE_MUTEX_OVERHEAD("txn.prepare.mutex.overhead", TickerType.TXN_PREPARE_MUTEX_OVERHEAD),

  /** 快速路径下获取 old_commit_map_mutex_ 的次数。 */
  TXN_OLD_COMMIT_MAP_MUTEX_OVERHEAD(
      "txn.old.commit.map.mutex.overhead", TickerType.TXN_OLD_COMMIT_MAP_MUTEX_OVERHEAD),

  /** 检查批次中重复 key 的次数。 */
  TXN_DUPLICATE_KEY_OVERHEAD("txn.duplicate.key.overhead", TickerType.TXN_DUPLICATE_KEY_OVERHEAD),

  /** 快速路径下获取 snapshot_mutex_ 的次数。 */
  TXN_SNAPSHOT_MUTEX_OVERHEAD(
      "txn.snapshot.mutex.overhead", TickerType.TXN_SNAPSHOT_MUTEX_OVERHEAD),

  /** ::Get 因 snapshot 序列号过期而返回 TryAgain 的次数 */
  TXN_GET_TRY_AGAIN("txn.get.try.again", TickerType.TXN_GET_TRY_AGAIN),

  /** 被删除调度器标记为 trash 的文件数量 */
  FILES_MARKED_TRASH("files.marked.trash", TickerType.FILES_MARKED_TRASH),

  /** 后台线程从 trash 队列中删除的 trash 文件数量 */
  FILES_DELETED_FROM_TRASH_QUEUE(
      "files.deleted.from.trash.queue", TickerType.FILES_DELETED_FROM_TRASH_QUEUE),

  /** 被删除调度器立即删除的文件数量 */
  FILES_DELETED_IMMEDIATELY("files.deleted.immediately", TickerType.FILES_DELETED_IMMEDIATELY),

  /** DB 错误处理器统计 */
  ERROR_HANDLER_BG_ERROR_COUNT(
      "error.handler.bg.error.count", TickerType.ERROR_HANDLER_BG_ERROR_COUNT),
  ERROR_HANDLER_BG_IO_ERROR_COUNT(
      "error.handler.bg.io.error.count", TickerType.ERROR_HANDLER_BG_IO_ERROR_COUNT),
  ERROR_HANDLER_BG_RETRYABLE_IO_ERROR_COUNT(
      "error.handler.bg.retryable.io.error.count",
      TickerType.ERROR_HANDLER_BG_RETRYABLE_IO_ERROR_COUNT),
  ERROR_HANDLER_AUTORESUME_COUNT(
      "error.handler.autoresume.count", TickerType.ERROR_HANDLER_AUTORESUME_COUNT),
  ERROR_HANDLER_AUTORESUME_RETRY_TOTAL_COUNT(
      "error.handler.autoresume.retry.total.count",
      TickerType.ERROR_HANDLER_AUTORESUME_RETRY_TOTAL_COUNT),
  ERROR_HANDLER_AUTORESUME_SUCCESS_COUNT(
      "error.handler.autoresume.success.count", TickerType.ERROR_HANDLER_AUTORESUME_SUCCESS_COUNT),

  /**
   * flush 时 memtable 中的原始数据（payload）字节数，包含垃圾 payload（flush 时丢弃的字节）与 有效 payload（最终写入 SSTable
   * 的数据字节）之和。
   */
  MEMTABLE_PAYLOAD_BYTES_AT_FLUSH(
      "memtable.payload.bytes.at.flush", TickerType.MEMTABLE_PAYLOAD_BYTES_AT_FLUSH),
  /** flush 时 memtable 中已过期的数据字节数。 */
  MEMTABLE_GARBAGE_BYTES_AT_FLUSH(
      "memtable.garbage.bytes.at.flush", TickerType.MEMTABLE_GARBAGE_BYTES_AT_FLUSH),

  /** `VerifyChecksum()` 与 `VerifyFileChecksums()` API 读取的字节数。 */
  VERIFY_CHECKSUM_READ_BYTES("verify.checksum.read.bytes", TickerType.VERIFY_CHECKSUM_READ_BYTES),

  /** 创建备份期间读写的字节数 */
  BACKUP_READ_BYTES("backup.read.bytes", TickerType.BACKUP_READ_BYTES),
  BACKUP_WRITE_BYTES("backup.write.bytes", TickerType.BACKUP_WRITE_BYTES),

  /** 远程 compaction 读写统计 */
  REMOTE_COMPACT_READ_BYTES("remote.compact.read.bytes", TickerType.REMOTE_COMPACT_READ_BYTES),
  REMOTE_COMPACT_WRITE_BYTES("remote.compact.write.bytes", TickerType.REMOTE_COMPACT_WRITE_BYTES),

  /** 分层存储相关统计 */
  HOT_FILE_READ_BYTES("hot.file.read.bytes", TickerType.HOT_FILE_READ_BYTES),
  WARM_FILE_READ_BYTES("warm.file.read.bytes", TickerType.WARM_FILE_READ_BYTES),
  COLD_FILE_READ_BYTES("cold.file.read.bytes", TickerType.COLD_FILE_READ_BYTES),
  HOT_FILE_READ_COUNT("hot.file.read.count", TickerType.HOT_FILE_READ_COUNT),
  WARM_FILE_READ_COUNT("warm.file.read.count", TickerType.WARM_FILE_READ_COUNT),
  COLD_FILE_READ_COUNT("cold.file.read.count", TickerType.COLD_FILE_READ_COUNT),

  /** （非）最底层读取统计 */
  LAST_LEVEL_READ_BYTES("last.level.read.bytes", TickerType.LAST_LEVEL_READ_BYTES),
  LAST_LEVEL_READ_COUNT("last.level.read.count", TickerType.LAST_LEVEL_READ_COUNT),
  NON_LAST_LEVEL_READ_BYTES("non.last.level.read.bytes", TickerType.NON_LAST_LEVEL_READ_BYTES),
  NON_LAST_LEVEL_READ_COUNT("non.last.level.read.count", TickerType.NON_LAST_LEVEL_READ_COUNT),

  /**
   * 各 sorted run 上迭代器 Seek()（及其变体）的统计，即一次用户 Seek() 可能产生多次 sorted run
   * Seek()。统计按最底层与非最底层拆分。Filtered：前缀 Bloom filter 等过滤器判断 Seek() 不会 命中任何相关数据，从而避免了可能的数据块+索引块访问。
   */
  LAST_LEVEL_SEEK_FILTERED("last.level.seek.filtered", TickerType.LAST_LEVEL_SEEK_FILTERED),
  /** Filter match：查询了前缀 Bloom filter 等过滤器，但未能过滤掉该 seek。 */
  LAST_LEVEL_SEEK_FILTER_MATCH(
      "last.level.seek.filter.match", TickerType.LAST_LEVEL_SEEK_FILTER_MATCH),
  /** sorted run 上的 Seek()（或变体）至少访问了一个数据 block。 */
  LAST_LEVEL_SEEK_DATA("last.level.seek.data", TickerType.LAST_LEVEL_SEEK_DATA),
  /** 该 seek 至少访问了一个 value()（说明有实际收益），且未查询前缀 Bloom 等过滤器。 */
  LAST_LEVEL_SEEK_DATA_USEFUL_NO_FILTER(
      "last.level.seek.data.useful.no.filter", TickerType.LAST_LEVEL_SEEK_DATA_USEFUL_NO_FILTER),
  /** 在查询前缀 Bloom 等过滤器后，该 seek 至少访问了一个 value()（说明有实际收益）。 */
  LAST_LEVEL_SEEK_DATA_USEFUL_FILTER_MATCH(
      "last.level.seek.data.useful.filter.match",
      TickerType.LAST_LEVEL_SEEK_DATA_USEFUL_FILTER_MATCH),

  /** 同类统计，但针对非最底层的 seek。 */
  NON_LAST_LEVEL_SEEK_FILTERED(
      "non.last.level.seek.filtered", TickerType.NON_LAST_LEVEL_SEEK_FILTERED),
  NON_LAST_LEVEL_SEEK_FILTER_MATCH(
      "non.last.level.seek.filter.match", TickerType.NON_LAST_LEVEL_SEEK_FILTER_MATCH),
  NON_LAST_LEVEL_SEEK_DATA("non.last.level.seek.data", TickerType.NON_LAST_LEVEL_SEEK_DATA),
  NON_LAST_LEVEL_SEEK_DATA_USEFUL_NO_FILTER(
      "non.last.level.seek.data.useful.no.filter",
      TickerType.NON_LAST_LEVEL_SEEK_DATA_USEFUL_NO_FILTER),
  NON_LAST_LEVEL_SEEK_DATA_USEFUL_FILTER_MATCH(
      "non.last.level.seek.data.useful.filter.match",
      TickerType.NON_LAST_LEVEL_SEEK_DATA_USEFUL_FILTER_MATCH),

  /** block 校验和验证次数 */
  BLOCK_CHECKSUM_COMPUTE_COUNT(
      "block.checksum.compute.count", TickerType.BLOCK_CHECKSUM_COMPUTE_COUNT),

  /** RocksDB 在验证 block 校验和时检测到数据损坏的次数。RocksDB 不会记录用户读取期间发生的损坏， 因此同一 block 损坏可能被多次检测到。 */
  BLOCK_CHECKSUM_MISMATCH_COUNT(
      "block.checksum.mismatch.count", TickerType.BLOCK_CHECKSUM_MISMATCH_COUNT),

  MULTIGET_COROUTINE_COUNT("multiget.coroutine.count", TickerType.MULTIGET_COROUTINE_COUNT),

  /** 在 ReadAsync 文件系统调用上花费的时间 */
  READ_ASYNC_MICROS("read.async.micros", TickerType.READ_ASYNC_MICROS),

  /** 返回给异步读回调的错误数量 */
  ASYNC_READ_ERROR_COUNT("async.read.error.count", TickerType.ASYNC_READ_ERROR_COUNT),

  /** 在预取的尾部数据（见 `TABLE_OPEN_PREFETCH_TAIL_READ_BYTES`）中查找但未能找到数据的次数 （表打开场景） */
  TABLE_OPEN_PREFETCH_TAIL_MISS(
      "table.open.prefetch.tail.miss", TickerType.TABLE_OPEN_PREFETCH_TAIL_MISS),

  /** 在预取的尾部数据（见 `TABLE_OPEN_PREFETCH_TAIL_READ_BYTES`）中成功找到数据的次数（表打开 场景） */
  TABLE_OPEN_PREFETCH_TAIL_HIT(
      "table.open.prefetch.tail.hit", TickerType.TABLE_OPEN_PREFETCH_TAIL_HIT),

  /** 访问表时检查时间戳的次数 */
  TIMESTAMP_FILTER_TABLE_CHECKED(
      "timestamp.filter.table.checked", TickerType.TIMESTAMP_FILTER_TABLE_CHECKED),

  /** 时间戳成功帮助跳过表访问的次数 */
  TIMESTAMP_FILTER_TABLE_FILTERED(
      "timestamp.filter.table.filtered", TickerType.TIMESTAMP_FILTER_TABLE_FILTERED),

  READAHEAD_TRIMMED("readahead.trimmed", TickerType.READAHEAD_TRIMMED),

  FIFO_MAX_SIZE_COMPACTIONS("fifo.max.size.compactions", TickerType.FIFO_MAX_SIZE_COMPACTIONS),

  FIFO_TTL_COMPACTIONS("fifo.ttl.compactions", TickerType.FIFO_TTL_COMPACTIONS),

  PREFETCH_BYTES("prefetch.bytes", TickerType.PREFETCH_BYTES),

  PREFETCH_BYTES_USEFUL("prefetch.bytes.useful", TickerType.PREFETCH_BYTES_USEFUL),

  PREFETCH_HITS("prefetch.hits", TickerType.PREFETCH_HITS),

  SST_FOOTER_CORRUPTION_COUNT(
      "sst.footer.corruption.count", TickerType.SST_FOOTER_CORRUPTION_COUNT),

  FILE_READ_CORRUPTION_RETRY_COUNT(
      "file.read.corruption.retry.count", TickerType.FILE_READ_CORRUPTION_RETRY_COUNT),

  FILE_READ_CORRUPTION_RETRY_SUCCESS_COUNT(
      "file.read.corruption.retry.success.count",
      TickerType.FILE_READ_CORRUPTION_RETRY_SUCCESS_COUNT),

  TICKER_ENUM_MAX("ticker.enum.max", TickerType.TICKER_ENUM_MAX);

  private final String name;
  private final TickerType tickerType;

  RocksdbTickerMapping(final String name, final TickerType tickerType) {
    this.name = name;
    this.tickerType = tickerType;
  }

  /** 拼接指标名前缀，返回完整指标名 */
  public String getName(final String prefix) {
    return prefix + name;
  }

  /** 获取对应的 RocksDB ticker 类型 */
  public TickerType getTickerType() {
    return tickerType;
  }
}
