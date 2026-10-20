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

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;
import java.util.Set;
import org.junit.jupiter.api.Test;

/**
 * 数据手术迁移列族集合测试：恰为全部 local 物理列族的虚拟列族、唯排除 PROCESS_POSITION（分区级单行水位不随手术搬运）。
 *
 * @author zxuanhong
 * @since 2026.9.0
 */
class DataSurgeryMigrationFamiliesTest {

  @Test
  void shouldContainAllLocalFamiliesExceptProcessPosition() {
    final Set<BusinessRepositoryColumnFamilies> migration = BusinessRepositoryColumnFamilies.migrationFamilies();
    final long localCount =
        Arrays.stream(BusinessRepositoryColumnFamilies.values())
            .filter(
                cf -> "local".equals(new String(cf.entityFamilyName(), UTF_8)))
            .count();
    assertEquals(localCount - 1, migration.size());
    assertFalse(migration.contains(BusinessRepositoryColumnFamilies.PROCESS_POSITION));
    migration.forEach(
        cf -> assertEquals("local", new String(cf.entityFamilyName(), UTF_8), cf + " 必须是 local 列族"));
  }

  @Test
  void shouldCoverSurgeryStateFamiliesAndExcludeNonTransferable() {
    final Set<BusinessRepositoryColumnFamilies> migration = BusinessRepositoryColumnFamilies.migrationFamilies();
    // 手术状态主体列族必须在迁移集合内
    assertTrue(migration.contains(BusinessRepositoryColumnFamilies.KEY));
    assertTrue(migration.contains(BusinessRepositoryColumnFamilies.PROCESS_INSTANCE));
    assertTrue(migration.contains(BusinessRepositoryColumnFamilies.VARIABLE));
    assertTrue(migration.contains(BusinessRepositoryColumnFamilies.TIMER));
    assertTrue(migration.contains(BusinessRepositoryColumnFamilies.JOB));
    assertTrue(migration.contains(BusinessRepositoryColumnFamilies.INCIDENT));
    // 全局（部署类）与不可迁移（default 块）不得混入
    assertFalse(migration.contains(BusinessRepositoryColumnFamilies.DEPLOYMENT));
    assertFalse(migration.contains(BusinessRepositoryColumnFamilies.SINK));
    assertFalse(migration.contains(BusinessRepositoryColumnFamilies.DEFAULT));
  }
}
