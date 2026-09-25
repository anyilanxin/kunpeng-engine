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
package com.anyilanxin.kunpeng.utils.micrometer;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import io.micrometer.core.instrument.Tags;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.Test;

/**
 * Micrometer 工具测试。
 *
 * @author zxuanhong
 * @since 2026.9.0
 */
final class MicrometersTest {

  @Test
  void wrapForwardsMetersWithCommonTagsToBackingRegistry() {
    final var backing = new SimpleMeterRegistry();
    final var wrapped = Micrometers.wrap(backing, Tags.of("partition", "3"));

    wrapped.counter("test.counter").increment();

    assertNotNull(backing.find("test.counter").tags("partition", "3").counter());
  }

  @Test
  void closeRemovesMetersFromBackingRegistryWithoutClosingIt() {
    final var backing = new SimpleMeterRegistry();
    final var wrapped = Micrometers.wrap(backing, Tags.of("sinkId", "test"));
    wrapped.counter("test.counter").increment();

    Micrometers.close(wrapped);

    assertNull(backing.find("test.counter").counter());
    backing.counter("test.after-close").increment();
    assertEquals(1.0, backing.get("test.after-close").counter().count());
  }

  @Test
  void closeToleratesNullRegistry() {
    assertDoesNotThrow(() -> Micrometers.close(null));
  }

  @Test
  void partitionKeyNamesTagsContainPartitionId() {
    final var tags = PartitionKeyNames.tags(42);
    final var partitionTag =
        tags.stream()
            .filter(tag -> tag.getKey().equals("partition"))
            .findFirst()
            .orElseThrow();
    assertEquals("42", partitionTag.getValue());
  }
}
