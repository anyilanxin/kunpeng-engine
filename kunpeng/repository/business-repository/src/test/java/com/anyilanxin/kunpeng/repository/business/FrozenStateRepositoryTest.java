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

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.anyilanxin.kunpeng.configuration.broker.rocksdb.RocksdbConfiguration;
import com.anyilanxin.kunpeng.kvstore.KvStore;
import com.anyilanxin.kunpeng.rocksdb.DefaultRocksdbFactory;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * 冻结标记持久化测试：标记跨库重开保留（broker 重启恢复冻结态的事实源）、清除后归位。
 *
 * @author zxuanhong
 * @since 2026.9.0
 */
class FrozenStateRepositoryTest {
  @TempDir Path tempDir;

  private KvStore<BusinessRepositoryColumnFamilies> db;

  @Test
  void shouldPersistFrozenMarkerAcrossReopen() throws Exception {
    assertNull(repository().frozenStateRepository().frozenMarker(), "初始未冻结");
    closeDb();

    repository().frozenStateRepository().markFrozen(true);
    closeDb();

    // 模拟 broker 重启：重开同一目录的库，标记必须仍在（drain=true）
    final BusinessRepository restarted = repository();
    final Boolean marker = restarted.frozenStateRepository().frozenMarker();
    assertTrue(marker != null && marker, "冻结标记应跨库重开保留");
    restarted.frozenStateRepository().markFrozen(false);
    closeDb();
    assertFalse(repository().frozenStateRepository().frozenMarker());
    closeDb();

    repository().frozenStateRepository().clearFrozen();
    closeDb();
    assertNull(repository().frozenStateRepository().frozenMarker());
  }

  private BusinessRepository repository() {
    db =
        new DefaultRocksdbFactory<BusinessRepositoryColumnFamilies>()
            .createDb(
                tempDir.resolve("live").toString(),
                new SimpleMeterRegistry(),
                0,
                new RocksdbConfiguration(),
                BusinessRepositoryColumnFamilies.DEFAULT);
    return new RocksdbBusinessRepository(null, db, null, new SimpleMeterRegistry());
  }

  private void closeDb() throws Exception {
    db.close();
  }
}
