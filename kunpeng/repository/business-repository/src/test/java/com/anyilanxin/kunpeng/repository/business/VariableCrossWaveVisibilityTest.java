/*
 * Copyright © 2026 anyilanxin zxh (anyilanxin@aliyun.com)
 *
 * This program is free software: you can use redistribute it  and/or modify it under the terms of the
 * GNU Affero General Public License as published by the Free Software Foundation, either version 3
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the
 * implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU Affero General Public
 * License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License along with this program.
 * If not, see <https://www.gnu.org/licenses/>.
 */
package com.anyilanxin.kunpeng.repository.business;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.anyilanxin.kunpeng.cluster.business.step.RaftPartitionSource;
import com.anyilanxin.kunpeng.cluster.cluster.PartitionId;
import com.anyilanxin.kunpeng.configuration.broker.rocksdb.RocksdbConfiguration;
import com.anyilanxin.kunpeng.kvstore.KvStore;
import com.anyilanxin.kunpeng.kvstore.TransactionContext;
import com.anyilanxin.kunpeng.protocol.business.ValueType;
import com.anyilanxin.kunpeng.protocol.business.impl.record.command.variable.VariableRecord;
import com.anyilanxin.kunpeng.protocol.business.record.command.variable.VariableLifeCycle;
import com.anyilanxin.kunpeng.rocksdb.DefaultRocksdbFactory;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * 跨波次变量可见性微测（排查并行加签失控）：完全按引擎波次节奏——每波 getCurrentTransaction → run{applyState} →
 * commit，验证后一波能否读到前一波 applier 写入的计数器游标。若读回恒为初值即坐实仓储/事务层缺陷。
 */
class VariableCrossWaveVisibilityTest {
  private static final long SCOPE_ID = 200L;
  @TempDir Path tempDir;
  private KvStore<BusinessRepositoryColumnFamilies> db;

  @AfterEach
  void tearDown() throws Exception {
    if (db != null) {
      db.close();
    }
  }

  private BusinessRepository openRepository() throws Exception {
    db =
        new DefaultRocksdbFactory<BusinessRepositoryColumnFamilies>()
            .createDb(
                tempDir.resolve("live").toString(),
                new SimpleMeterRegistry(),
                0,
                new RocksdbConfiguration(),
                BusinessRepositoryColumnFamilies.DEFAULT);
    final RaftPartitionSource partitionSource = mock(RaftPartitionSource.class);
    when(partitionSource.getPartitionId()).thenReturn(PartitionId.from("business-raft", 1));
    return new RocksdbBusinessRepositoryFactory(db, partitionSource, null, new SimpleMeterRegistry())
        .create();
  }

  @Test
  void laterWaveMustSeeEarlierWaveCounterWrite() throws Exception {
    final BusinessRepository repository = openRepository();
    final BusinessRepositoryAppliers appliers = repository.getAppliers();
    final TransactionContext context = repository.getContext();

    // 波 A（spawnGroup 等价）：CREATED 载荷+计数器 游标=-1
    runWave(
        context,
        appliers,
        VariableLifeCycle.CREATED,
        Map.of(
            "__kunpeng_addsign_payload",
                Map.of(
                    "assignees", List.of("yyyyykkkk", "uu"),
                    "mode", "PARALLEL",
                    "phase", "BEFORE",
                    "taskKey", "task1",
                    "taskName", "审批",
                    "depth", 1),
            "__kunpeng_addsign_counters",
                Map.of("total", 2, "created", 0, "completed", 0, "terminated", 0, "cursor", -1)));

    // 波 B（执行器第 1 轮等价）：UPDATED 游标=0
    runWave(
        context,
        appliers,
        VariableLifeCycle.UPDATED,
        Map.of(
            "__kunpeng_addsign_payload",
                Map.of(
                    "assignees", List.of("yyyyykkkk", "uu"),
                    "mode", "PARALLEL",
                    "phase", "BEFORE",
                    "taskKey", "task1",
                    "taskName", "审批",
                    "depth", 1),
            "__kunpeng_addsign_counters",
                Map.of("total", 2, "created", 1, "completed", 0, "terminated", 0, "cursor", 0)));

    // 波 C（执行器第 2 轮读）：必须读到 cursor=0
    final AtomicReference<Map<String, Object>> seen = new AtomicReference<>();
    final var tx = context.getCurrentTransaction();
    tx.run(
        () -> {
          final var optional = repository.variableRepository().getRecord(SCOPE_ID);
          seen.set(optional.map(VariableRecord::getVariables).orElse(null));
        });
    tx.commit();

    final Map<String, Object> variables = seen.get();
    assertTrue(variables != null && variables.containsKey("__kunpeng_addsign_counters"),
        "应能读到变量: " + variables);
    @SuppressWarnings("unchecked")
    final Map<String, Object> counters =
        (Map<String, Object>) variables.get("__kunpeng_addsign_counters");
    assertEquals(0, ((Number) counters.get("cursor")).intValue(), "后一波必须读到前一波写入的游标 0，实际=" + counters);
    System.out.println("WAVE-C-READ: " + counters);
  }

  private void runWave(
      final TransactionContext context,
      final BusinessRepositoryAppliers appliers,
      final VariableLifeCycle lifeCycle,
      final Map<String, Object> variables)
      throws Exception {
    final var tx = context.getCurrentTransaction();
    tx.run(
        () -> {
          final VariableRecord record = new VariableRecord();
          record.setScopId(SCOPE_ID);
          record.setParentScopId(100L);
          record.setProcessDefinitionId(5L);
          record.setProcessInstanceId(9L);
          record.setVariables(variables);
          appliers.applyState(SCOPE_ID, ValueType.VARIABLE, lifeCycle, record);
        });
    tx.commit();
  }
}
