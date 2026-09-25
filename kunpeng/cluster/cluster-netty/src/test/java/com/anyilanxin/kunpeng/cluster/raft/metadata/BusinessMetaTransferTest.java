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
package com.anyilanxin.kunpeng.cluster.raft.metadata;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Map;
import org.junit.jupiter.api.Test;

/** {@link BusinessMetaTransfer} 请求/响应编解码测试。 */
class BusinessMetaTransferTest {

  @Test
  void shouldRoundTripRequest() {
    final byte[] encoded = BusinessMetaTransfer.encodeRequest(Map.of("sourceId", "3"), true);

    final BusinessMetaTransfer.Request decoded = BusinessMetaTransfer.decodeRequest(encoded);
    assertTrue(decoded.forwarded());
    assertEquals(Map.of("sourceId", "3"), decoded.entries());
  }

  @Test
  void shouldRoundTripEmptyRequest() {
    final byte[] encoded = BusinessMetaTransfer.encodeRequest(Map.of(), false);

    final BusinessMetaTransfer.Request decoded = BusinessMetaTransfer.decodeRequest(encoded);
    assertFalse(decoded.forwarded());
    assertTrue(decoded.entries().isEmpty());
  }

  @Test
  void shouldRoundTripResponses() {
    assertResponse(
        BusinessMetaUpdateResponse.ok(15),
        BusinessMetaTransfer.decodeResponse(
            BusinessMetaTransfer.encodeResponse(BusinessMetaUpdateResponse.ok(15))));
    assertResponse(
        BusinessMetaUpdateResponse.noLeader(),
        BusinessMetaTransfer.decodeResponse(
            BusinessMetaTransfer.encodeResponse(BusinessMetaUpdateResponse.noLeader())));
    assertResponse(
        BusinessMetaUpdateResponse.error("boom"),
        BusinessMetaTransfer.decodeResponse(
            BusinessMetaTransfer.encodeResponse(BusinessMetaUpdateResponse.error("boom"))));
  }

  private void assertResponse(
      final BusinessMetaUpdateResponse expected, final BusinessMetaUpdateResponse actual) {
    assertEquals(expected.success(), actual.success());
    assertEquals(expected.index(), actual.index());
    assertEquals(expected.error(), actual.error());
    assertFalse(actual.success() && actual.error() != null);
  }
}
