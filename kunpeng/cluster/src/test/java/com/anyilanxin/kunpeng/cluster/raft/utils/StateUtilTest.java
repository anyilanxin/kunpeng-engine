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
package com.anyilanxin.kunpeng.cluster.raft.utils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatException;
import static org.assertj.core.api.Assertions.assertThatNoException;

import java.time.Duration;
import java.util.concurrent.CompletableFuture;
import java.util.function.LongConsumer;
import java.util.stream.Stream;
import org.junit.jupiter.api.Named;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 启动一致性检查：节点启动时校验快照索引与日志首索引之间的间隙。无法恢复一致的间隙应抛异常；
 * 日志为空但有间隙的状态应通过“重置日志到快照索引 + 1”恢复一致；其余状态直接放行。
 */
class StateUtilTest {

  private static final Logger LOG = LoggerFactory.getLogger("CONSISTENCY-CHECK-TEST");
  private static final long COMMIT_INDEX = 1L;

  @ParameterizedTest(name = "[不可恢复] {0}")
  @MethodSource("irrecoverableStates")
  void verifyThrowsForIrrecoverableGap(
      final Named<Long> snapshotIndex, final long firstLogIndex, final boolean emptyLog) {
    assertThatException()
        .isThrownBy(
            () -> check(snapshotIndex.getPayload(), firstLogIndex, emptyLog, i -> {}));
  }

  @ParameterizedTest(name = "[无需重置] {0}")
  @MethodSource("consistentStatesWithoutReset")
  void verifyAcceptsConsistentStateWithoutResettingLog(
      final Named<Long> snapshotIndex, final long firstLogIndex, final boolean emptyLog) {
    assertThatNoException()
        .isThrownBy(
            () ->
                check(snapshotIndex.getPayload(), firstLogIndex, emptyLog, i -> {
                  // 一致状态下绝不触发日志重置
                  throw new AssertionError("不应触发日志重置");
                }));
  }

  @ParameterizedTest(name = "[重置后一致] {0}")
  @MethodSource("statesConsistentAfterReset")
  void verifyResetsEmptyLogToSnapshotIndexPlusOne(
      final Named<Long> snapshotIndex, final long firstLogIndex) {
    final var resetTo = new CompletableFuture<Long>();

    assertThatNoException()
        .isThrownBy(
            () -> check(snapshotIndex.getPayload(), firstLogIndex, true, resetTo::complete));

    assertThat(resetTo).succeedsWithin(Duration.ofMillis(100)).isEqualTo(snapshotIndex.getPayload() + 1);
  }

  private static void check(
      final long snapshotIndex, final long firstLogIndex, final boolean emptyLog,
      final LongConsumer resetAction) {
    StateUtil.verifySnapshotLogConsistent(
        COMMIT_INDEX, snapshotIndex, firstLogIndex, emptyLog, resetAction, LOG);
  }

  /** 快照与日志之间存在无法恢复的间隙：日志非空且首索引大于快照索引 + 1。 */
  private static Stream<Arguments> irrecoverableStates() {
    return Stream.of(
        Arguments.of(Named.of("没有快照但日志首索引大于 1", 0L), 5L, false),
        Arguments.of(Named.of("快照与日志之间缺一条条目", 1L), 5L, false),
        Arguments.of(Named.of("快照与日志之间缺多条条目", 3L), 6L, false));
  }

  /** 快照与日志之间没有间隙、无需重置日志即一致的状态。 */
  private static Stream<Arguments> consistentStatesWithoutReset() {
    return Stream.of(
        Arguments.of(Named.of("刚收到快照，重启时尚未收到任何日志条目", 4L), 5L, true),
        Arguments.of(Named.of("收到快照后又收到了日志条目", 4L), 5L, false),
        Arguments.of(Named.of("快照后已压缩日志，首索引等于快照索引", 5L), 5L, false),
        Arguments.of(Named.of("快照后已压缩日志，首索引小于快照索引", 6L), 5L, false),
        Arguments.of(Named.of("初始状态：无快照、空日志", 0L), 1L, true),
        Arguments.of(Named.of("只追加过日志、尚无快照", 0L), 1L, false));
  }

  /** 日志为空但与快照之间有间隙：重置到快照索引 + 1 后即可恢复一致。 */
  private static Stream<Arguments> statesConsistentAfterReset() {
    return Stream.of(
        Arguments.of(Named.of("首个快照后日志已重置、快照提交前宕机", 0L), 5L),
        Arguments.of(Named.of("更新快照后日志已重置、快照提交前宕机", 3L), 5L));
  }
}
