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
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import com.anyilanxin.kunpeng.configuration.broker.rocksdb.RocksdbConfiguration;
import com.anyilanxin.kunpeng.kvstore.KvStore;
import com.anyilanxin.kunpeng.protocol.business.ValueType;
import com.anyilanxin.kunpeng.protocol.business.impl.record.command.incodent.IncidentRecord;
import com.anyilanxin.kunpeng.protocol.business.record.command.incident.IncidentType;
import com.anyilanxin.kunpeng.protocol.business.record.command.usertask.UserTaskLifeCycle;
import com.anyilanxin.kunpeng.rocksdb.DefaultRocksdbFactory;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * incident 属主映射查询测试：按属主业务 id 查挂着的 incident、删除后归位（真实 RocksDB 落库）。
 *
 * @author zxuanhong
 * @since 2026.9.0
 */
class IncidentOwnerMappingRepositoryTest {
  private static final long INCIDENT_ID = 777L;
  private static final long TASK_ID = 1L;
  private static final long PROCESS_INSTANCE_ID = 9L;

  @TempDir Path tempDir;

  private KvStore<BusinessRepositoryColumnFamilies> db;

  @Test
  void shouldFindAndDropIncidentByBusinessId() throws Exception {
    final BusinessRepository repository = repository();
    repository.incidentRepository().save(INCIDENT_ID, incident());

    // 按属主（任务 id）查回同一份 incident
    final IncidentRecord found = repository.incidentRepository().getRecordByBusinessId(TASK_ID);
    assertNotNull(found);
    assertEquals(INCIDENT_ID, found.getIncidentId());
    assertEquals(IncidentType.USER_TASK, found.getIncidentType());
    assertEquals(TASK_ID, found.getTaskId());
    assertEquals(PROCESS_INSTANCE_ID, found.getProcessInstanceId());

    // 删除（DELETED/RESOLVED 应用器路径）后再查归位；直接按 id 查也无
    repository.incidentRepository().delete(INCIDENT_ID);
    assertNull(repository.incidentRepository().getRecordByBusinessId(TASK_ID));
    assertNull(repository.incidentRepository().getRecord(INCIDENT_ID));
    closeDb();
  }

  /** 未挂 incident 的属主：查询返回 null */
  @Test
  void shouldReturnNullWhenOwnerHasNoIncident() throws Exception {
    assertNull(repository().incidentRepository().getRecordByBusinessId(TASK_ID));
    closeDb();
  }

  private IncidentRecord incident() {
    return new IncidentRecord()
        .setIncidentId(INCIDENT_ID)
        .setIncidentType(IncidentType.USER_TASK)
        .setTaskId(TASK_ID)
        .setProcessInstanceId(PROCESS_INSTANCE_ID)
        .setProcessDefinitionId(5L)
        .setProcessDefinitionKey("p1")
        .setIncidentRecordValueType(ValueType.USER_TASK)
        .setIncidentRecordLifeCycle(UserTaskLifeCycle.CREATING)
        .setIncidentMessage("User task assignee expression failed: boom");
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
