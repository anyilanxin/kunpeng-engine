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
package com.anyilanxin.kunpeng.sink.protocol;

import static org.assertj.core.api.Assertions.assertThat;

import org.agrona.concurrent.UnsafeBuffer;
import org.junit.jupiter.api.Test;

/**
 * sink 位置消息测试。
 *
 * @author zxuanhong
 * @since 2026.9.0
 */
final class SinkPositionsMessageTest {

  @Test
  void survivesARoundTrip() {
    // 假设
    final var original = new SinkPositionsMessage();
    original.put("elasticsearch", 815L, new UnsafeBuffer(new byte[] {1, 2, 3}));
    original.put("rdbms", -1L, null);

    // 当
    final var encoded = original.toByteBuffer();
    final var decoded = new SinkPositionsMessage();
    decoded.wrap(new UnsafeBuffer(encoded), 0, encoded.remaining());

    // 则
    assertThat(decoded.getPositions()).containsOnlyKeys("elasticsearch", "rdbms");
    assertThat(decoded.getPositions().get("elasticsearch").position()).isEqualTo(815L);
    assertThat(decoded.getPositions().get("elasticsearch").metadata())
        .isEqualTo(new UnsafeBuffer(new byte[] {1, 2, 3}));
    assertThat(decoded.getPositions().get("rdbms").position()).isEqualTo(-1L);
    assertThat(decoded.getPositions().get("rdbms").metadata().capacity()).isZero();
  }

  @Test
  void emptyMessageSurvivesARoundTrip() {
    // 假设
    final var original = new SinkPositionsMessage();

    // 当
    final var encoded = original.toByteBuffer();
    final var decoded = new SinkPositionsMessage();
    final var matched =
        decoded.tryWrap(new UnsafeBuffer(encoded), 0, encoded.remaining());

    // 则
    assertThat(matched).isTrue();
    assertThat(decoded.getPositions()).isEmpty();
  }

  @Test
  void tryWrapRejectsForeignMessages() {
    // 假设：一个长度合适但内容全零（schema/模板 id 为 0）的缓冲区
    final var foreign = new UnsafeBuffer(new byte[16]);

    // 当/则
    assertThat(new SinkPositionsMessage().tryWrap(foreign, 0, foreign.capacity()))
        .isFalse();
  }
}
