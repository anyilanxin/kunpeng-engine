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
package com.anyilanxin.kunpeng.cluster.raft;

import static org.assertj.core.api.Assertions.assertThat;

import com.anyilanxin.kunpeng.utils.FileUtil;
import com.anyilanxin.kunpeng.cluster.cluster.MemberId;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import net.jqwik.api.Arbitraries;
import net.jqwik.api.Arbitrary;
import net.jqwik.api.EdgeCasesMode;
import net.jqwik.api.ForAll;
import net.jqwik.api.Property;
import net.jqwik.api.PropertyDefaults;
import net.jqwik.api.Provide;
import net.jqwik.api.ShrinkingMode;
import net.jqwik.api.lifecycle.AfterTry;
import net.jqwik.api.lifecycle.BeforeProperty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 基于属性的随机化测试：在随机注入消息丢失/重启故障的过程中不断发起 join，
 * 故障停止后 join 必须最终完成，且每轮都满足“每个成员每个任期至多投一票”的安全性不变量。
 */
@PropertyDefaults(tries = 10, shrinking = ShrinkingMode.OFF, edgeCases = EdgeCasesMode.NONE)
public class RandomizedRaftJoinTest {

  private static final Logger LOG = LoggerFactory.getLogger(RandomizedRaftJoinTest.class);
  private static final int FAULT_SEQUENCE_LENGTH = 1000;
  /** 故障停止后等待 join 完成的最大推进步数。 */
  private static final int SETTLE_BUDGET = 10100;

  private ControllableRaftContexts raftContexts;
  private Path raftDataDir;
  private MemberId bootstrapMember;
  private MemberId joiningMember;
  private List<RaftOperation> faultOperations;

  @BeforeProperty
  public void createTwoNodeClusterFixture() {
    bootstrapMember = MemberId.from("0");
    joiningMember = MemberId.from("1");
    // 缩短 quorum 响应超时，使确定式调度器下也能触发基于挂钟时间的领导者退位：
    // 领导者连续 minStepDownFailureCount 次联系不上加入节点后退位，与生产行为一致
    raftContexts =
        new ControllableRaftContexts(
            2, config -> config.setMaxQuorumResponseTimeout(Duration.ofMillis(1)));
    faultOperations = RaftOperation.getRaftOperationsWithRestarts();
  }

  @AfterTry
  public void teardownCluster() throws IOException {
    if (raftContexts != null) {
      raftContexts.shutdown();
    }
    if (raftDataDir != null) {
      FileUtil.deleteTree(raftDataDir);
      raftDataDir = null;
    }
  }

  @Property
  void joinEventuallyCompletesUnderRandomFaults(
      @ForAll("faultScript") final List<RaftOperation> faultScript,
      @ForAll("faultTargets") final List<MemberId> faultTargets,
      @ForAll("randomSeeds") final long seed)
      throws Exception {
    // 单节点起簇：先只用 bootstrapMember 引导
    raftDataDir = Files.createTempDirectory(null);
    raftContexts.setup(raftDataDir, new Random(seed), Set.of(0));
    LOG.info("两节点集群就绪，初始仅包含成员 0");

    CompletableFuture<?> joinFuture =
        raftContexts.join(joiningMember, Set.of(bootstrapMember, joiningMember));

    // 执行：逐步注入故障；每一步后都采样安全性不变量（成员在本任期内的投票记录随时可能被覆盖，
    // 因此必须在每步之间立即检查）
    final var targetIterator = faultTargets.iterator();
    for (final RaftOperation fault : faultScript) {
      final MemberId target = targetIterator.next();
      LOG.info("在 {} 上执行 {}", target, fault);
      fault.run(raftContexts, target);
      raftContexts.assertAtMostOneVotePerMemberAndTerm();

      joinFuture = retryJoinIfFailed(joinFuture);
    }

    // 故障停止，等待 join 收敛
    LOG.info("停止注入故障，等待 join 完成");
    int budget = SETTLE_BUDGET;
    while (!(joinFuture.isDone()
            && !joinFuture.isCompletedExceptionally()
            && raftContexts.allMembersAreReady()
            && raftContexts.hasLeaderAtTheLatestTerm())
        && budget-- > 0) {
      joinFuture = retryJoinIfFailed(joinFuture);
      raftContexts.runUntilDone();
      raftContexts.processAllMessage();
      raftContexts.tickHeartbeatTimeout();
    }

    // 验证：join 完成、有最新任期的领导者、全部成员 READY，且安全性不变量仍成立
    assertThat(joinFuture).as("成员 1 的 join 应最终完成").isCompleted();
    assertThat(raftContexts.hasLeaderAtTheLatestTerm()).as("集群应存在领导者").isTrue();
    raftContexts.assertAllMembersAreReady();
    raftContexts.assertAtMostOneVotePerMemberAndTerm();
  }

  private CompletableFuture<?> retryJoinIfFailed(final CompletableFuture<?> current) {
    if (current.isCompletedExceptionally()) {
      LOG.info("join 失败，重试");
      return raftContexts.join(joiningMember, Set.of(bootstrapMember, joiningMember));
    }
    return current;
  }

  @Provide
  Arbitrary<List<RaftOperation>> faultScript() {
    return Arbitraries.of(faultOperations).list().ofSize(FAULT_SEQUENCE_LENGTH);
  }

  @Provide
  Arbitrary<List<MemberId>> faultTargets() {
    return Arbitraries.of(bootstrapMember, joiningMember).list().ofSize(FAULT_SEQUENCE_LENGTH);
  }

  @Provide
  Arbitrary<Long> randomSeeds() {
    return Arbitraries.longs();
  }
}
