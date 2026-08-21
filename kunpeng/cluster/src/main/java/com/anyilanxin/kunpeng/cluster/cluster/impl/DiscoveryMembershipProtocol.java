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
package com.anyilanxin.kunpeng.cluster.cluster.impl;

import com.anyilanxin.kunpeng.cluster.cluster.BootstrapService;
import com.anyilanxin.kunpeng.cluster.cluster.Member;
import com.anyilanxin.kunpeng.cluster.cluster.MemberId;
import com.anyilanxin.kunpeng.cluster.cluster.discovery.NodeDiscoveryEvent;
import com.anyilanxin.kunpeng.cluster.cluster.discovery.NodeDiscoveryEventListener;
import com.anyilanxin.kunpeng.cluster.cluster.discovery.NodeDiscoveryService;
import com.anyilanxin.kunpeng.cluster.cluster.protocol.GroupMembershipEvent;
import com.anyilanxin.kunpeng.cluster.cluster.protocol.GroupMembershipEventListener;
import com.anyilanxin.kunpeng.cluster.cluster.protocol.GroupMembershipProtocol;
import com.anyilanxin.kunpeng.cluster.cluster.protocol.GroupMembershipProtocolConfig;
import com.anyilanxin.kunpeng.cluster.utils.event.AbstractListenerManager;
import io.micrometer.core.instrument.MeterRegistry;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicBoolean;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 以节点发现服务为成员来源的组成员协议实现。
 *
 * <p>成员表完全镜像 {@link NodeDiscoveryService} 的节点视图：节点加入即成员加入，节点离开即
 * 成员离开，本地成员在 {@link #join} 时单独登记。构造时可预置一批初始成员，用于测试或静态
 * 组播场景。
 */
public final class DiscoveryMembershipProtocol
    extends AbstractListenerManager<GroupMembershipEvent, GroupMembershipEventListener>
    implements GroupMembershipProtocol, NodeDiscoveryEventListener {
  private static final Logger LOGGER = LoggerFactory.getLogger(DiscoveryMembershipProtocol.class);

  private final ConcurrentMap<MemberId, Member> memberTable = new ConcurrentHashMap<>();
  private final AtomicBoolean running = new AtomicBoolean();

  private NodeDiscoveryService discoverySource;

  /** 创建一个成员表为空的协议实例。 */
  public DiscoveryMembershipProtocol() {}

  /** 以配置对象中预置的成员创建协议实例。 */
  public DiscoveryMembershipProtocol(final Config config) {
    this(config.members);
  }

  /** 以显式成员表创建协议实例。 */
  public DiscoveryMembershipProtocol(final Map<MemberId, Member> members) {
    memberTable.putAll(members);
  }

  @Override
  public Set<Member> getMembers() {
    return new HashSet<>(memberTable.values());
  }

  @Override
  public Member getMember(final MemberId memberId) {
    return memberTable.get(memberId);
  }

  @Override
  public CompletableFuture<Void> join(
      final BootstrapService bootstrap,
      final NodeDiscoveryService discovery,
      final Member localMember) {
    if (running.compareAndSet(false, true)) {
      // 先把发现服务当前已知的节点全部登记为成员
      discovery.getNodes().forEach(node -> registerMember(toMember(node), false));
      registerMember(localMember, true);
      discoverySource = discovery;
      discoverySource.addListener(this);
      LOGGER.info("Started discovery membership protocol with members [{}]", memberTable);
    }
    return CompletableFuture.completedFuture(null);
  }

  @Override
  public CompletableFuture<Void> leave(final Member localMember) {
    if (running.compareAndSet(true, false)) {
      LOGGER.info("Stopped discovery membership protocol");
      discoverySource.removeListener(this);
      memberTable.clear();
    }
    return CompletableFuture.completedFuture(null);
  }

  /** 节点发现事件回调：JOIN 对应成员加入，LEAVE 对应成员移除。 */
  @Override
  public void event(final NodeDiscoveryEvent event) {
    final var affected = toMember(event.node());
    switch (event.type()) {
      case JOIN -> registerMember(affected, false);
      case LEAVE -> unregisterMember(affected);
      default -> {
        // 其余事件类型与成员表无关，忽略
      }
    }
  }

  @Override
  public GroupMembershipProtocolConfig config() {
    return new Config(new HashMap<>(memberTable));
  }

  /** 把节点视图映射为成员视图。 */
  private static Member toMember(final com.anyilanxin.kunpeng.cluster.cluster.Node node) {
    final var memberId = MemberId.from(node.id().id());
    return Member.member(memberId, node.address());
  }

  /** 登记成员；首次出现的成员触发 MEMBER_ADDED 事件，{@code announce} 为 true 时无条件触发。 */
  private void registerMember(final Member member, final boolean announce) {
    final var added = memberTable.put(member.id(), member) == null;
    if (added || announce) {
      post(new GroupMembershipEvent(GroupMembershipEvent.Type.MEMBER_ADDED, member));
    }
  }

  /** 移除成员；确实存在时触发 MEMBER_REMOVED 事件。 */
  private void unregisterMember(final Member member) {
    if (memberTable.remove(member.id()) != null) {
      post(new GroupMembershipEvent(GroupMembershipEvent.Type.MEMBER_REMOVED, member));
    }
  }

  /** 预置成员表配置。 */
  public static final class Config extends GroupMembershipProtocolConfig {
    private final Map<MemberId, Member> members;

    public Config(final Map<MemberId, Member> members) {
      this.members = members;
    }

    /** 配置中预置的成员表。 */
    public Map<MemberId, Member> members() {
      return members;
    }

    @Override
    public GroupMembershipProtocol.Type<Config> getType() {
      return new Type();
    }
  }

  private static final class Type implements GroupMembershipProtocol.Type<Config> {
    @Override
    public String name() {
      return "memory";
    }

    @Override
    public GroupMembershipProtocol newProtocol(
        final Config config, final String actorSchedulerName, final MeterRegistry registry) {
      return new DiscoveryMembershipProtocol(config);
    }
  }
}
