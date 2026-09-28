/*
 * Copyright © 2026 anyilanxin zxh (anyilanxin@aliyun.com)
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the Free Software Foundation as either version 3
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
package com.anyilanxin.kunpeng.sink.rdbms.mapper;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.anyilanxin.kunpeng.sink.rdbms.RdbmsSinkSettings.TableNameCase;
import java.util.Properties;
import org.junit.jupiter.api.Test;

/**
 * 标识符枚举的大小写渲染与变量注册行为。
 *
 * @author zxuanhong
 * @since 2026.9.0
 */
class TableNameTest {

  @Test
  void physicalNameFollowsCaseStrategy() {
    assertEquals("KP_USER_TASK", TableName.USER_TASK.physicalName("KP_", TableNameCase.UPPER));
    assertEquals("kp_user_task", TableName.USER_TASK.physicalName("kp_", TableNameCase.LOWER));
    assertEquals(
        "KP_DATABASECHANGELOG",
        TableName.DATABASECHANGELOG.physicalName("KP_", TableNameCase.UPPER));
    assertEquals(
        "kp_databasechangelog",
        TableName.DATABASECHANGELOG.physicalName("kp_", TableNameCase.LOWER));
    assertEquals(
        "kp_databasechangeloglock",
        TableName.DATABASECHANGELOGLOCK.physicalName("kp_", TableNameCase.LOWER));
  }

  @Test
  void registerAllInjectsFullPhysicalNames() {
    final var variables = new Properties();
    TableName.registerAll(variables::setProperty, "KP_", TableNameCase.UPPER);
    assertEquals("KP_USER_TASK", variables.getProperty("table.user_task"));
    assertEquals("KP_IDX_UT_INSTANCE", variables.getProperty("table.idx_ut_instance"));
    assertEquals("KP_DATABASECHANGELOG", variables.getProperty("table.databasechangelog"));
    assertEquals("KP_DATABASECHANGELOGLOCK", variables.getProperty("table.databasechangeloglock"));
  }
}
