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

import java.util.Base64;
import java.util.Properties;
import org.agrona.concurrent.UnsafeBuffer;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * {@link JobSubscriptionInfo} 实体属性载体往返测试。
 *
 * @author zxuanhong
 * @since 2026.9.0
 */
class JobSubscriptionInfoTest {

  @Test
  void shouldRoundTripThroughProperties() {
    final JobSubscriptionInfo info = new JobSubscriptionInfo().setGeneration(3);
    final JobSubscriptionInfo.Aggregate orders = new JobSubscriptionInfo.Aggregate();
    orders.jobType = "order-process";
    orders.sessions.add(new JobSubscriptionInfo.Session(11, "sdk-worker-1"));
    orders.sessions.add(new JobSubscriptionInfo.Session(12, "other-worker"));
    info.addAggregate(orders);
    final JobSubscriptionInfo.Aggregate payment = new JobSubscriptionInfo.Aggregate();
    payment.jobType = "payment";
    info.addAggregate(payment);

    final Properties properties = new Properties();
    info.writeIntoProperties(properties);
    final String encoded = properties.getProperty(JobSubscriptionInfo.PROPERTY_NAME);
    try {
      java.nio.file.Files.writeString(
          java.nio.file.Path.of("/tmp/js-frame.hex"),
          java.util.HexFormat.of().formatHex(Base64.getDecoder().decode(encoded)));
    } catch (final Exception e) {
      throw new RuntimeException(e);
    }
    assertNotNull(encoded);
    // 直接走 wrap 暴露编解码异常（不经过 fromProperties 的静默兜底）
    final byte[] bytes = Base64.getDecoder().decode(encoded);
    final JobSubscriptionInfo decoded = new JobSubscriptionInfo();
    decoded.wrap(new UnsafeBuffer(bytes), 0, bytes.length);

    assertEquals(3, decoded.getGeneration());
    assertEquals(2, decoded.getAggregates().size());
    assertEquals("order-process", decoded.getAggregates().get(0).jobType);
    assertEquals(2, decoded.getAggregates().get(0).sessions.size());
    assertEquals(11, decoded.getAggregates().get(0).sessions.get(0).sessionId);
    assertEquals("sdk-worker-1", decoded.getAggregates().get(0).sessions.get(0).worker);
    assertEquals("payment", decoded.getAggregates().get(1).jobType);
    assertEquals(0, decoded.getAggregates().get(1).sessions.size());
  }
}
