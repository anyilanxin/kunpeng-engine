/*
 * Copyright © 2026 anyilanxin zxh (anyilanxin@aliyun.com)
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
package com.anyilanxin.kunpeng.sink.rdbms;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.anyilanxin.kunpeng.sink.config.MapSinkConfig;
import java.util.Map;
import org.junit.jupiter.api.Test;

/**
 * broker 配置 args 到 {@link RdbmsSinkSettings} 的绑定： 老配置键（autoDdl/caseSensitive）必须真实生效而不是被静默忽略。
 *
 * @author zxuanhong
 * @since 2026.9.0
 */
class RdbmsSinkSettingsBindingTest {

  @Test
  void bindsReferenceEraConfigKeys() {
    final var config =
        new MapSinkConfig(
            "rdbms",
            Map.of(
                "url", "jdbc:h2:mem:test",
                "autoDdl", false,
                "caseSensitive", true,
                "tablePrefix", "ANYI_"));
    final RdbmsSinkSettings settings = config.createSettings(RdbmsSinkSettings.class);

    assertFalse(settings.isAutoDdl());
    assertTrue(settings.isCaseSensitive());
    assertEquals("ANYI_", settings.getTablePrefix());
  }

  @Test
  void defaultsMatchReferenceBehavior() {
    final var settings = new MapSinkConfig("rdbms", Map.of()).createSettings(RdbmsSinkSettings.class);

    assertTrue(settings.isAutoDdl());
    assertFalse(settings.isCaseSensitive());
    assertEquals("KP_", settings.getTablePrefix());
    assertEquals(RdbmsSinkSettings.TableNameCase.UPPER, settings.resolvedTableNameCase());
  }
}
