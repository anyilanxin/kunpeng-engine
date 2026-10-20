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
package com.anyilanxin.kunpeng.repository.business;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.anyilanxin.kunpeng.configuration.broker.rocksdb.RocksdbConfiguration;
import com.anyilanxin.kunpeng.kvstore.ColumnCopyType;
import com.anyilanxin.kunpeng.kvstore.KvStore;
import com.anyilanxin.kunpeng.rocksdb.DefaultRocksdbFactory;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * 数据手术存储原语端到端测试（真实 RocksDB）：拆分载荷恰好只含指定资源的数据、接收端经既有 merge 路径摄入后状态续跑、
 * donor 删除恰好移除该资源且其余资源不受影响。
 *
 * @author zxuanhong
 * @since 2026.9.0
 */
class ResourceDataSurgeryTest {
  @TempDir Path tempDir;

  @Test
  void shouldSplitRemoveAndMergeExactlyTheTargetResource() {
    // donor：资源 1 两个 key、资源 2 一个 key
    final KvStore<BusinessRepositoryColumnFamilies> donorDb = createDb("donor");
    final BusinessRepository donor = repository(donorDb);
    final long r1First = donor.keyGeneratorRepository().nextKey(1);
    final long r1Second = donor.keyGeneratorRepository().nextKey(1);
    final long r2First = donor.keyGeneratorRepository().nextKey(2);

    // 拆分载荷写出：只含资源 1 的行（KEY 生成器按资源整型直映射，域间天然不相交）
    final Path payload = tempDir.resolve("payload");
    donor.writeResourceData(1, payload);

    // 接收端：既有 merge 路径摄入后，资源 1 的生成器越过 donor 已用最大值，资源 2 从零开始
    final KvStore<BusinessRepositoryColumnFamilies> receiverDb = createDb("receiver");
    receiverDb.merge(
        payload, BusinessRepositoryColumnFamilies.migrationFamilies(), ColumnCopyType.VIRTUAL_FAMILY);
    final BusinessRepository receiver = repository(receiverDb);
    assertEquals(r1Second + 1, receiver.keyGeneratorRepository().nextKey(1));
    // 资源 2 未随载荷迁移：接收端从该资源的初始值起步，与 donor 侧的首个 key 同值而非续跑
    final long r2Fresh = receiver.keyGeneratorRepository().nextKey(2);
    assertEquals(r2First, r2Fresh);

    // donor 删除被移走数据：资源 1 生成器回到初始（首个 key 重发），资源 2 不受影响继续递增
    donor.removeResourceData(1);
    assertEquals(r1First, donor.keyGeneratorRepository().nextKey(1));
    assertEquals(r2First + 1, donor.keyGeneratorRepository().nextKey(2));
  }

  private KvStore<BusinessRepositoryColumnFamilies> createDb(final String name) {
    return new DefaultRocksdbFactory<BusinessRepositoryColumnFamilies>()
        .createDb(
            tempDir.resolve(name).toString(),
            new SimpleMeterRegistry(),
            0,
            new RocksdbConfiguration(),
            BusinessRepositoryColumnFamilies.DEFAULT);
  }

  private BusinessRepository repository(final KvStore<BusinessRepositoryColumnFamilies> db) {
    return new RocksdbBusinessRepository(null, db, null, new SimpleMeterRegistry());
  }
}
