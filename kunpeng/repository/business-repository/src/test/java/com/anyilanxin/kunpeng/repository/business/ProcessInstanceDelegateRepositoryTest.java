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
import com.anyilanxin.kunpeng.protocol.business.impl.record.command.processinstance.ProcessInstanceRecord;
import com.anyilanxin.kunpeng.protocol.business.record.command.processinstance.ProcessInstanceLifeCycle;
import com.anyilanxin.kunpeng.protocol.business.record.command.processinstance.ProcessInstanceState;
import com.anyilanxin.kunpeng.repository.business.modules.processinstance.applier.impl.RocksdbRepositoryProcessInstanceDelegateUserApplierImpl;
import com.anyilanxin.kunpeng.repository.business.modules.processinstance.applier.impl.RocksdbRepositoryProcessInstanceStartUserApplierImpl;
import com.anyilanxin.kunpeng.repository.business.modules.processinstance.record.ProcessInstanceRecordEntity;
import com.anyilanxin.kunpeng.rocksdb.DefaultRocksdbFactory;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * 流程实例发起人与代办人的持久化往返：实体 wrap/unwrap 覆盖新字段（漏同步即静默丢数据）；实例行经真实存储落库再读回一致，关库重开仍在；
 * 发起人已修改、代办人已设置两个事件经应用器更新行（代办人的修改与清空同一事件）。
 *
 * @author zxuanhong
 * @since 2026.9.0
 */
class ProcessInstanceDelegateRepositoryTest {
  @TempDir Path tempDir;

  private KvStore<BusinessRepositoryColumnFamilies> db;

  @Test
  void entityWrapUnwrapCarriesStartUserAndDelegateUserId() {
    final var entity = new ProcessInstanceRecordEntity();
    entity.wrap(instance("alice", "carol"));

    final var decoded = entity.unwrap(new ProcessInstanceRecord());
    assertEquals("alice", decoded.getStartUserId());
    assertEquals("carol", decoded.getDelegateUserId());
  }

  @Test
  void instanceFieldsSurviveStoreRoundTripAndEventsApply() throws Exception {
    var repository = repository();
    repository.processInstanceRepository().save(1, instance("alice", "carol"));
    assertEquals(
        "carol", repository.processInstanceRepository().getRecord(1).getDelegateUserId());

    // 关库重开：代办人仍在（重放一致性依赖行里真有这个字段）
    closeDb();
    repository = repository();
    assertEquals(
        "carol", repository.processInstanceRepository().getRecord(1).getDelegateUserId());

    // 发起人已修改事件：发起人换人，代办人不动
    new RocksdbRepositoryProcessInstanceStartUserApplierImpl(repository)
        .applyState(1, instance("bob", "carol"));
    assertEquals("bob", repository.processInstanceRepository().getRecord(1).getStartUserId());
    assertEquals(
        "carol", repository.processInstanceRepository().getRecord(1).getDelegateUserId());

    // 代办人已设置事件：修改与清空都经应用器更新行
    final var delegateApplier = new RocksdbRepositoryProcessInstanceDelegateUserApplierImpl(repository);
    delegateApplier.applyState(1, instance("bob", "dave"));
    assertEquals(
        "dave", repository.processInstanceRepository().getRecord(1).getDelegateUserId());
    delegateApplier.applyState(1, instance("bob", ""));
    assertEquals(
        "", repository.processInstanceRepository().getRecord(1).getDelegateUserId());
    closeDb();
  }

  private static ProcessInstanceRecord instance(
      final String startUserId, final String delegateUserId) {
    final var record =
        new ProcessInstanceRecord()
            .setProcessInstanceId(1)
            .setRootProcessInstanceId(1)
            .setLifeCycle(ProcessInstanceLifeCycle.ACTIVATED)
            .setState(ProcessInstanceState.ACTIVATED)
            .setStartUserId(startUserId)
            .setProcessDefinitionId(5)
            .setProcessDefinitionKey("p1");
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
