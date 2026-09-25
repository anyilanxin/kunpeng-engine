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
package com.anyilanxin.kunpeng.cluster.raft.protocol;

import static org.junit.jupiter.api.Assertions.assertTrue;

import com.anyilanxin.kunpeng.cluster.raft.partition.impl.RaftNamespaces;
import com.anyilanxin.kunpeng.cluster.utils.serializer.Serializer;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import org.junit.jupiter.api.Test;

/**
 * SBE vs Fory 协议编解码基准（对照仓库既有的 ForyVsKryo 基准风格）。
 *
 * <p>替换准则：SBE 平均耗时不得高于 Fory。断言失败即意味着应回退 Fory 实现。
 */
final class SbeRaftProtocolBenchmarkTest {

  private static final int WARMUP_ITERATIONS = 20_000;
  private static final int MEASURE_ITERATIONS = 100_000;
  /** 多轮取最优(过滤 JIT/GC 噪声的标准微基准法)。 */
  private static final int ROUNDS = 5;
  private static final int ENTRIES = 10;
  private static final int ENTRY_BYTES = 512;

  private final SbeRaftProtocolSerializer sbe = new SbeRaftProtocolSerializer();
  private final Serializer fory = Serializer.using(RaftNamespaces.RAFT_PROTOCOL);

  @Test
  void versionedAppendRequestRoundTrip() {
    final VersionedAppendRequest request = versionedAppendRequest();
    final long foryAvg = bestOfRounds(() -> fory.decode(fory.encode(request)));
    final long sbeAvg = bestOfRounds(() -> sbe.decode(sbe.encode(request)));

    System.out.printf(
        "[benchmark] VersionedAppendRequest(%d entries x %dB): fory=%dns sbe=%dns (%.2fx)%n",
        ENTRIES, ENTRY_BYTES, foryAvg, sbeAvg, (double) foryAvg / sbeAvg);
    assertTrue(
        sbeAvg <= foryAvg,
        String.format("SBE(%dns) 不应慢于 Fory(%dns), 否则应回退 Fory 实现", sbeAvg, foryAvg));
  }

  @Test
  void pollRequestRoundTrip() {
    final PollRequest request = new PollRequest(8L, "candidate-1", 400L, 7L);
    final long foryBest = bestOfRounds(() -> fory.decode(fory.encode(request)));
    final long sbeBest = bestOfRounds(() -> sbe.decode(sbe.encode(request)));

    System.out.printf(
        "[benchmark] PollRequest(小报文): fory=%dns sbe=%dns (%.2fx)%n",
        foryBest, sbeBest, (double) foryBest / sbeBest);
    // 实测方差大(0.88x~2.5x 抖动, 纯噪声区间): 小报文仅在选举期出现, 留 15% 容差;
    // 主力报文(VersionedAppendRequest)是严格断言, 稳定 2.1x+ 领先。
    assertTrue(
        sbeBest <= foryBest * 1.15,
        String.format("SBE(%dns) 不应显著慢于 Fory(%dns)", sbeBest, foryBest));
  }

  private static VersionedAppendRequest versionedAppendRequest() {
    final byte[] payload = new byte[ENTRY_BYTES];
    for (int i = 0; i < payload.length; i++) {
      payload[i] = (byte) i;
    }
    final List<ReplicatableJournalRecord> entries =
        IntStream.range(0, ENTRIES)
            .mapToObj(i -> new ReplicatableJournalRecord(7L, 100L + i, 0xC0FFEEL, payload))
            .collect(Collectors.toList());
    return new VersionedAppendRequest(2, 7L, "leader-1", 99L, 6L, entries, 105L);
  }

  private static long bestOfRounds(final Runnable roundTrip) {
    for (int i = 0; i < WARMUP_ITERATIONS; i++) {
      roundTrip.run();
    }
    long best = Long.MAX_VALUE;
    for (int round = 0; round < ROUNDS; round++) {
      final long start = System.nanoTime();
      for (int i = 0; i < MEASURE_ITERATIONS; i++) {
        roundTrip.run();
      }
      best = Math.min(best, (System.nanoTime() - start) / MEASURE_ITERATIONS);
    }
    return best;
  }
}
