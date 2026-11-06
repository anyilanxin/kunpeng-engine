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
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
package com.anyilanxin.kunpeng.repository.business;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.anyilanxin.kunpeng.configuration.broker.rocksdb.RocksdbConfiguration;
import com.anyilanxin.kunpeng.kvstore.KvStore;
import com.anyilanxin.kunpeng.protocol.business.impl.record.command.usertask.UserTaskRecord;
import com.anyilanxin.kunpeng.protocol.business.record.command.usertask.AddSignRole;
import com.anyilanxin.kunpeng.rocksdb.DefaultRocksdbFactory;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * 加签组员关联列族测试：加签任务按自身活动实例（组实例）前缀收录、删除即摘除、跨库重开保留；
 * 非加签任务不入关联；列表每条独立实例（不共享 recordBuffer 坍缩）。
 *
 * @author zxuanhong
 * @since 2026.9.0
 */
class UserTaskAddSignCorrelationTest {
  @TempDir Path tempDir;

  private KvStore<BusinessRepositoryColumnFamilies> db;

  @Test
  void shouldTrackAddSignCorrelationLifecycle() throws Exception {
    var repository = repository();
    // 原任务（非加签）不应入关联
    repository.userTaskRepository().save(1, new UserTaskRecord().setTaskId(1));
    // 组活动实例 100 名下两个加签任务（键=任务自身活动实例）
    repository
        .userTaskRepository()
        .save(
            11,
            new UserTaskRecord()
                .setTaskId(11)
                .setActivityInstanceId(100)
                .setAddSignRole(AddSignRole.ADD_SIGN));
    repository
        .userTaskRepository()
        .save(
            12,
            new UserTaskRecord()
                .setTaskId(12)
                .setActivityInstanceId(100)
                .setAddSignRole(AddSignRole.ADD_SIGN));

    final List<UserTaskRecord> tasks = repository.userTaskRepository().getAddSignTasks(100);
    assertEquals(2, tasks.size());
    assertEquals(
        List.of(11L, 12L), tasks.stream().map(UserTaskRecord::getTaskId).sorted().toList(),
        "每条加签任务必须是独立实例（不得共享 recordBuffer 坍缩为同一条）");
    assertTrue(repository.userTaskRepository().getAddSignTasks(999).isEmpty());
    closeDb();

    // 重开库：关联仍在（跨重启持久化）
    repository = repository();
    assertEquals(2, repository.userTaskRepository().getAddSignTasks(100).size());

    // 删除加签任务即摘除关联
    repository.userTaskRepository().delete(11);
    final List<UserTaskRecord> remaining = repository.userTaskRepository().getAddSignTasks(100);
    assertEquals(1, remaining.size());
    assertEquals(12, remaining.getFirst().getTaskId());
    closeDb();
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
