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
package com.anyilanxin.kunpeng.cluster.raft.snapshotv2;

import com.anyilanxin.kunpeng.cluster.raft.snapshot.SnapshotId;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

final class SnapshotIdTest {

  @Test
  void shouldEncodeNodeIdFirstInStringForm() {
    final var id = new SnapshotId("node-1", 5, 3);

    assertThat(id.asString()).isEqualTo("6e6f64652d31-5-3");
  }

  @Test
  void shouldRoundTripStringForm() {
    final var id = new SnapshotId("node", 123, 456);

    assertThat(SnapshotId.fromString(id.asString())).isEqualTo(id);
  }

  @Test
  void shouldRejectMalformedId() {
    assertThatThrownBy(() -> SnapshotId.fromString("not-a-valid-id"))
      .isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  void shouldCompareByTermWhenIndexEqual() {
    final var a = new SnapshotId("node-1", 10, 1);
    final var b = new SnapshotId("node-1", 10, 2);

    assertThat(a.compareTo(b)).isNegative();
    assertThat(b.compareTo(a)).isPositive();
  }

  @Test
  void shouldCompareByIndexIgnoringNodeId() {
    // 拍摄（自身 nodeId）与接收（leader nodeId）的镜像共用同一 store，排序不得掺入 nodeId
    final var selfOld = new SnapshotId("node-1", 10, 1);
    final var newerFromOtherLeader = new SnapshotId("leader-2", 11, 2);
    final var olderFromOtherLeader = new SnapshotId("leader-2", 5, 1);

    assertThat(newerFromOtherLeader.compareTo(selfOld)).isPositive();
    assertThat(olderFromOtherLeader.compareTo(selfOld)).isNegative();
    assertThat(new SnapshotId("a", 10, 1).compareTo(new SnapshotId("b", 10, 1))).isZero();
  }

  @Test
  void shouldPreferIndexOverTerm() {
    // index 是最高位：index 更高的镜像即便 term 更低也更新
    final var higherIndexLowerTerm = new SnapshotId("node-1", 11, 1);
    final var lowerIndexHigherTerm = new SnapshotId("node-1", 10, 5);

    assertThat(higherIndexLowerTerm.compareTo(lowerIndexHigherTerm)).isPositive();
    assertThat(lowerIndexHigherTerm.compareTo(higherIndexLowerTerm)).isNegative();
  }

  @Test
  void compareToEqualShouldNotImplyEquals() {
    // compareTo 只看 (index, term)，nodeId 不同则 equals 为 false
    final var a = new SnapshotId("node-1", 10, 1);
    final var b = new SnapshotId("node-2", 10, 1);

    assertThat(a.compareTo(b)).isZero();
    assertThat(a).isNotEqualTo(b);
  }
}
