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
package com.anyilanxin.kunpeng.cluster.cluster.leaderfound;

import com.anyilanxin.kunpeng.cluster.cluster.ClusterMembershipEvent;
import com.anyilanxin.kunpeng.cluster.cluster.ClusterMembershipService;
import com.anyilanxin.kunpeng.cluster.cluster.Member;
import com.anyilanxin.kunpeng.cluster.cluster.MemberId;
import com.anyilanxin.kunpeng.cluster.utils.concurrent.Scheduled;
import com.anyilanxin.kunpeng.cluster.utils.concurrent.SingleThreadContext;
import com.anyilanxin.kunpeng.cluster.utils.concurrent.ThreadContext;
import com.anyilanxin.kunpeng.cluster.utils.net.Address;
import java.time.Duration;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.atomic.AtomicBoolean;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author zxuanhong
 */
public class DefaultManageClusterLeaderFoundService implements ManageClusterLeaderFoundService {

  private volatile LeaderInfo leader;
  private final ClusterMembershipService membershipService;
  public static final Logger LOGGER =
      LoggerFactory.getLogger("com.anyilanxin.kunpeng.cluster.cluster.leader");
  private final Member localMember;
  private final Set<ClusterLeaderChangeListener> changeListeners = new CopyOnWriteArraySet<>();
  private final AtomicBoolean started = new AtomicBoolean();
  private final ThreadContext threadContext =
      new SingleThreadContext("atomix-cluster-leader-found-%d");

  /** 等待 leader 的在途 future：stop 时 executor 已关闭、重试 timer 失效，需逐个显式失败防调用方悬挂 */
  private final Set<CompletableFuture<LeaderInfo>> pendingLeaderFutures =
      ConcurrentHashMap.newKeySet();

  public DefaultManageClusterLeaderFoundService(final ClusterMembershipService membershipService) {
    this.membershipService = membershipService;
    localMember = membershipService.getLocalMember();
  }

  @Override
  public CompletableFuture<ClusterLeaderFoundService> start() {
    membershipService.addListener(this);
    started.set(true);
    checkForMissingEvents();
    return CompletableFuture.completedFuture(this);
  }

  @Override
  public boolean isRunning() {
    return started.get();
  }

  @Override
  public CompletableFuture<Void> stop() {
    started.set(false);
    membershipService.removeListener(this);
    threadContext.close();
    pendingLeaderFutures.forEach(
        future ->
            future.completeExceptionally(
                new IllegalStateException("Leader found service is stopped")));
    pendingLeaderFutures.clear();
    return CompletableFuture.completedFuture(null);
  }

  @Override
  public CompletableFuture<MemberId> getAsyncLeader() {
    final CompletableFuture<LeaderInfo> future = new CompletableFuture<>();
    final CompletableFuture<MemberId> result = new CompletableFuture<>();
    getLeader(future);
    future.whenComplete(
        (leader, throwable) -> {
          if (throwable != null) {
            result.completeExceptionally(throwable);
          } else {
            result.complete(leader.memberId());
          }
        });
    return result;
  }

  @Override
  public CompletableFuture<Address> getAsyncLeaderAddress() {
    final CompletableFuture<LeaderInfo> future = new CompletableFuture<>();
    final CompletableFuture<Address> result = new CompletableFuture<>();
    getLeader(future);
    future.whenComplete(
        (leader, throwable) -> {
          if (throwable != null) {
            result.completeExceptionally(throwable);
          } else {
            result.complete(leader.address());
          }
        });
    return result;
  }

  @Override
  public MemberId getLeader() {
    if (leader == null) {
      return null;
    }
    return leader.memberId();
  }

  @Override
  public Address getLeaderAddress() {
    if (leader == null) {
      return null;
    }
    return leader.address();
  }

  /** 启动时补拉当前已在线成员的拓扑广播，避免依赖后续 gossip 事件 */
  private void checkForMissingEvents() {
    final Set<Member> members = membershipService.getMembers();
    if (members == null || members.isEmpty()) {
      return;
    }
    for (final Member member : members) {
      final String memberType =
          member
              .properties()
              .getProperty(
                  MemberLeaderType.MEMBER_LEADER_PROPERTY_KEY, MemberLeaderType.GENERAL.getType());
      if (MemberLeaderType.LEADER.getType().equals(memberType)) {
        final MemberId id = member.id();
        final Address address = member.address();
        leader = new LeaderInfo(member.id(), address);
        notices(leader, false);
        LOGGER.debug("[{}]发现集群领导:{}", localMember.id().id(), id.id());
      }
    }
  }

  private void getLeader(final CompletableFuture<LeaderInfo> future) {
    LOGGER.debug("[{}]-------正在获取领导节点", localMember.id());
    if (leader != null) {
      LOGGER.debug("[{}]-------获取领导节点成功-----当前领导节点:{}", localMember.id(), leader.memberId().id());
      future.complete(leader);
      return;
    }
    if (!started.get()) {
      future.completeExceptionally(new IllegalStateException("Leader found service is stopped"));
      return;
    }
    pendingLeaderFutures.add(future);
    future.whenComplete((result, error) -> pendingLeaderFutures.remove(future));
    try {
      final Scheduled timer =
          threadContext.schedule(Duration.ofMillis(400), () -> getLeader(future));
      future.whenComplete((result, error) -> timer.cancel());
    } catch (final RejectedExecutionException e) {
      // 与 stop() 竞态：executor 已关闭，重试无法继续，直接以异常完成
      future.completeExceptionally(e);
    }
  }

  @Override
  public void event(final ClusterMembershipEvent event) {
    final Member subject = event.subject();
    LOGGER.debug("[{}]节点元数据发生变化:{}", subject.id(), subject.properties());
    switch (event.type()) {
      case MEMBER_REMOVED -> {
        if (leader != null && subject.id().equals(leader.memberId())) {
          LOGGER.debug("[{}]集群领导下线:{}", localMember.id().id(), leader.memberId().id());
          leader = null;
          notices(leader, true);
        }
      }
      case MEMBER_ADDED, METADATA_CHANGED -> {
        final String memberType =
            subject
                .properties()
                .getProperty(
                    MemberLeaderType.MEMBER_LEADER_PROPERTY_KEY,
                    MemberLeaderType.GENERAL.getType());
        if (MemberLeaderType.LEADER.getType().equals(memberType)) {
          if (leader == null || !leader.memberId().equals(subject.id())) {
            final MemberId id = subject.id();
            final Member member = membershipService.getMember(id);
            final Address address = member.address();
            leader = new LeaderInfo(member.id(), address);
            notices(leader, false);
            LOGGER.debug("[{}]发现集群领导:{}", localMember.id().id(), id.id());
          }
        }
      }
      default -> {
        // ignore
      }
    }
  }

  void notices(final LeaderInfo leader, final boolean lose) {
    if (leader != null) {
      LOGGER.info(
          "[{}]集群领导{}[{}]", localMember.id().id(), lose ? "丢失" : "发现", leader.memberId().id());
    } else {
      LOGGER.info("[{}]集群领导{}", localMember.id().id(), lose ? "丢失" : "发现");
    }
    threadContext.execute(
        () ->
            changeListeners.forEach(
                v -> {
                  if (!lose) {
                    v.foundLeader(leader);
                  } else {
                    v.loseLeader(leader);
                  }
                }));
  }

  @Override
  public void addLeaderChangeListener(final ClusterLeaderChangeListener listener) {
    threadContext.execute(
        () -> {
          changeListeners.add(listener);
          if (leader != null) {
            listener.foundLeader(leader);
          }
        });
  }

  @Override
  public void removeLeaderChangeListener(final ClusterLeaderChangeListener listener) {
    changeListeners.remove(listener);
  }
}
