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
package com.anyilanxin.kunpeng.cluster.cluster;

import com.anyilanxin.kunpeng.cluster.cluster.discovery.NodeDiscoveryConfig;
import com.anyilanxin.kunpeng.cluster.cluster.discovery.NodeDiscoveryEvent;
import com.anyilanxin.kunpeng.cluster.cluster.discovery.NodeDiscoveryEventListener;
import com.anyilanxin.kunpeng.cluster.cluster.discovery.NodeDiscoveryProvider;
import com.anyilanxin.kunpeng.cluster.utils.event.AbstractListenerManager;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CopyOnWriteArraySet;

/**
 * 测试辅助：节点集合由用例手动驱动的发现服务提供者。
 *
 * <p>join/leave 只负责把本地节点加入或移出集合，并在集合确实发生变化时派发对应事件。
 */
public final class TestDiscoveryProvider
    extends AbstractListenerManager<NodeDiscoveryEvent, NodeDiscoveryEventListener>
    implements NodeDiscoveryProvider {

  /** 已被发现的节点集合，线程安全以便测试并发驱动。 */
  private final Set<Node> discovered = new CopyOnWriteArraySet<>();

  @Override
  public Set<Node> getNodes() {
    return discovered;
  }

  @Override
  public CompletableFuture<Void> join(final BootstrapService bootstrap, final Node localNode) {
    return track(localNode, NodeDiscoveryEvent.Type.JOIN);
  }

  @Override
  public CompletableFuture<Void> leave(final Node localNode) {
    return track(localNode, NodeDiscoveryEvent.Type.LEAVE);
  }

  /** 无真实配置来源，恒定返回 null。 */
  @Override
  public NodeDiscoveryConfig config() {
    return null;
  }

  /** 若集合状态发生变化则广播对应类型的事件，并立即完成返回的 future。 */
  private CompletableFuture<Void> track(final Node node, final NodeDiscoveryEvent.Type type) {
    final boolean changed =
        type == NodeDiscoveryEvent.Type.JOIN ? discovered.add(node) : discovered.remove(node);
    if (changed) {
      listenerRegistry.process(new NodeDiscoveryEvent(type, node));
    }
    return CompletableFuture.completedFuture(null);
  }
}
