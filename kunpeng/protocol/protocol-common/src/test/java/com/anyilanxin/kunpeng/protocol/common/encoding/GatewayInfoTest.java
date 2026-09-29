/*
 * Copyright © 2026 anyilanxin zxh(anyilanxin.com)
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
package com.anyilanxin.kunpeng.protocol.common.encoding;

import java.util.Properties;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * {@link GatewayInfo} 实体属性载体往返测试。
 *
 * @author zxuanhong
 * @since 2026.9.0
 */
class GatewayInfoTest {

  @Test
  void shouldRoundTripThroughProperties() {
    final Properties properties = new Properties();
    GatewayInfo.advertise(properties, "10.0.0.8", 2024);

    final GatewayInfo decoded = GatewayInfo.fromProperties(properties);
    assertNotNull(decoded);
    assertEquals(2024, decoded.getClientPort());
    assertEquals("10.0.0.8", decoded.getClientHost());
  }

  @Test
  void shouldFallBackHostWhenBlank() {
    final Properties properties = new Properties();
    GatewayInfo.advertise(properties, null, 2024);
    final GatewayInfo decoded = GatewayInfo.fromProperties(properties);
    assertNotNull(decoded);
    assertEquals("", decoded.getClientHost());
  }

  @Test
  void shouldReturnNullForMissingProperty() {
    assertNull(GatewayInfo.fromProperties(new Properties()));
  }
}
