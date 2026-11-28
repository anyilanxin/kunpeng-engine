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
import com.anyilanxin.kunpeng.kvstore.KvStore;
import com.anyilanxin.kunpeng.protocol.business.impl.record.command.usertask.UserTaskRecord;
import com.anyilanxin.kunpeng.repository.business.modules.usertask.applier.impl.RocksdbRepositoryUserTaskDelegateUserApplierImpl;
import com.anyilanxin.kunpeng.repository.business.modules.usertask.record.UserTaskRecordEntity;
import com.anyilanxin.kunpeng.rocksdb.DefaultRocksdbFactory;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * 代办人 id 的持久化往返：实体 wrap/unwrap 覆盖新字段（漏同步即静默丢数据）；任务行经真实存储落库再读回一致，关库重开仍在； 代办人已设置事件经应用器更新行，修改与清空同一事件。
 *
 * @author zxuanhong
 * @since 2026.9.0
 */
class UserTaskDelegateRepositoryTest {
  @TempDir Path tempDir;

  private KvStore<BusinessRepositoryColumnFamilies> db;

  @Test
  void entityWrapUnwrapCarriesDelegateUserId() {
    final var entity = new UserTaskRecordEntity();
    entity.wrap(task("carol"));

    final var decoded = entity.unwrap(new UserTaskRecord());
    assertEquals("carol", decoded.getDelegateUserId());
    assertEquals("alice", decoded.getAssignee());
  }

  @Test
  void delegateUserIdSurvivesStoreRoundTripAndEventApplies() throws Exception {
    var repository = repository();
    repository.userTaskRepository().save(1, task("carol"));
    assertEquals("carol", repository.userTaskRepository().getRecord(1).getDelegateUserId());

    // 关库重开：代办人仍在（重放一致性依赖行里真有这个字段）
    closeDb();
    repository = repository();
    assertEquals("carol", repository.userTaskRepository().getRecord(1).getDelegateUserId());

    // 代办人已设置事件：修改与清空都经应用器更新行
    final var applier = new RocksdbRepositoryUserTaskDelegateUserApplierImpl(repository);
    applier.applyState(1, task("dave"));
    assertEquals("dave", repository.userTaskRepository().getRecord(1).getDelegateUserId());
    applier.applyState(1, task(""));
    assertEquals("", repository.userTaskRepository().getRecord(1).getDelegateUserId());
    closeDb();
  }

  private static UserTaskRecord task(final String delegateUserId) {
    final var record = new UserTaskRecord().setTaskId(1).setAssignee("alice");
    record.setDelegateUserId(delegateUserId);
    return record;
  }

  private RocksdbBusinessRepository repository() {
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
