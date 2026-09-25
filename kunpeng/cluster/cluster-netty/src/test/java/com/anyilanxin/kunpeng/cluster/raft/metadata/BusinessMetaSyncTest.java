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

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import com.anyilanxin.kunpeng.cluster.raft.storage.system.BusinessMetaStore;
import java.util.Map;
import org.junit.jupiter.api.Test;

/** {@link BusinessMetaSync} 同步响应帧编解码测试。 */
class BusinessMetaSyncTest {

  @Test
  void shouldRoundTripAvailableResponse() {
    final byte[] state = BusinessMetaStore.encode(9, 3, Map.of("sourceId", "8"));
    final byte[] encoded = BusinessMetaSync.encodeSyncResponse(true, state);

    assertEquals(1 + state.length, encoded.length);
    assertEquals(BusinessMetaSync.STATUS_AVAILABLE, encoded[0]);
    final byte[] decoded = BusinessMetaSync.decodeSyncResponse(encoded);
    assertArrayEquals(state, decoded);
    // 载荷可被 BusinessMetaStore 正常解码
    assertEquals(9, BusinessMetaStore.decode(decoded).orElseThrow().index());
  }

  @Test
  void shouldEncodeUnavailableResponse() {
    final byte[] encoded = BusinessMetaSync.encodeSyncResponse(false, null);
    assertEquals(1, encoded.length);
    assertEquals(BusinessMetaSync.STATUS_UNAVAILABLE, encoded[0]);
    assertNull(BusinessMetaSync.decodeSyncResponse(encoded));
  }

  @Test
  void shouldTreatGarbagePayloadAsUnavailable() {
    assertNull(BusinessMetaSync.decodeSyncResponse(new byte[0]));
    assertNull(BusinessMetaSync.decodeSyncResponse(null));
    assertNull(
        BusinessMetaSync.decodeSyncResponse(new byte[] {BusinessMetaSync.STATUS_UNAVAILABLE}));
  }
}
