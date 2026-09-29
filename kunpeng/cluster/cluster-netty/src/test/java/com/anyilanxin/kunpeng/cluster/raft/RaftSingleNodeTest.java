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

import com.anyilanxin.kunpeng.cluster.raft.impl.RaftContext.State;
import java.util.concurrent.TimeUnit;
import org.awaitility.Awaitility;
import org.junit.Rule;
import org.junit.Test;

/** 单节点集群在重启后应能重新进入 READY 状态。 */
public class RaftSingleNodeTest {
  @Rule public final RaftRule rule = RaftRule.withBootstrappedNodes(1);

  private static void awaitReady(final RaftServer server) {
    Awaitility.await()
        .atMost(30, TimeUnit.SECONDS)
        .until(() -> server.getContext().getState() == State.READY);
  }

  @Test
  public void restartSingleNodeCluster() throws Exception {
    // 准备：单节点集群先达到 READY
    final var soleServer = rule.getServers().iterator().next();
    final var serverName = soleServer.name();
    awaitReady(soleServer);

    // 执行：关闭后以同名节点重新加入
    rule.shutdownLeader();
    rule.joinCluster(serverName);

    // 验证：重新加入的单节点再次达到 READY
    awaitReady(rule.getServers().iterator().next());
  }
}
