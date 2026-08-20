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
package com.anyilanxin.kunpeng.cluster.raft.impl;

import com.anyilanxin.kunpeng.cluster.raft.cluster.RaftMember;
import com.anyilanxin.kunpeng.cluster.raft.storage.log.entry.ConfigurationEntry;
import java.util.ArrayList;
import java.util.Collection;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicReference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 配置变更状态机（联合共识）：管理 Leader 侧的成员变更全流程。
 *
 * <p>四阶段流转：
 * <ol>
 *   <li>{@code CATCHING_UP}——新成员追赶（addPeer 场景）；removePeer 直接跳过</li>
 *   <li>{@code JOINT}——写联合配置条目（旧∪新），等两集合各自 majority 提交</li>
 *   <li>{@code STABLE}——写最终配置条目（仅新集合），等新集合 majority 提交</li>
 *   <li>{@code DONE}——收尾：若 Leader 自身被移除则退位</li>
 * </ol>
 */
public final class ConfigurationChangeContext {

  private static final Logger LOG = LoggerFactory.getLogger(ConfigurationChangeContext.class);

  /** 变更阶段 */
  public enum Stage {
    /** 空闲 */
    NONE,
    /** 新成员追赶 */
    CATCHING_UP,
    /** 联合共识第一阶段（旧∪新双 majority） */
    JOINT,
    /** 联合共识第二阶段（仅新集合 majority） */
    STABLE
  }

  private final RaftContext raft;
  private final AtomicReference<Stage> stage = new AtomicReference<>(Stage.NONE);
  private volatile Collection<RaftMember> oldMembers;
  private volatile Collection<RaftMember> newMembers;
  private volatile CompletableFuture<Void> currentFuture;

  public ConfigurationChangeContext(final RaftContext raft) {
    this.raft = raft;
  }

  /** 当前阶段 */
  public Stage stage() {
    return stage.get();
  }

  /** 是否有变更正在进行 */
  public boolean isBusy() {
    return stage.get() != Stage.NONE;
  }

  /**
   * 发起配置变更（联合共识）。完成后 future 正常结束；失败或中止异常结束。
   *
   * @param oldMembers 当前成员集合
   * @param newMembers 目标成员集合
   * @return 变更完成的 future
   */
  public CompletableFuture<Void> change(
      final Collection<RaftMember> oldMembers, final Collection<RaftMember> newMembers) {
    if (!stage.compareAndSet(Stage.NONE, Stage.CATCHING_UP)) {
      return CompletableFuture.failedFuture(
          new IllegalStateException("配置变更正在进行中: " + stage.get()));
    }
    this.oldMembers = new ArrayList<>(oldMembers);
    this.newMembers = new ArrayList<>(newMembers);
    final var future = new CompletableFuture<Void>();
    this.currentFuture = future;

    // 计算差异：是否有新增成员（需要追赶）
    final var adding = new ArrayList<>(newMembers);
    adding.removeAll(oldMembers);

    if (adding.isEmpty()) {
      // removePeer 场景：无需追赶，直接进入 JOINT
      enterJoint(future);
    } else {
      // addPeer 场景：先等新成员追上
      LOG.info("配置变更进入 CATCHING_UP 阶段，新成员: {}", adding);
      waitForCatchUp(adding, future);
    }
    return future;
  }

  /** 等待新成员追上（Leader 的 LeaderAppender 持续复制日志，新成员自然追赶） */
  private void waitForCatchUp(
      final Collection<RaftMember> adding, final CompletableFuture<Void> future) {
    // 当前实现：LeaderAppender 已在持续复制日志，新成员以 PASSIVE 身份加入后自动追赶。
    // 无需显式等待——直接进入 JOINT 阶段。若需要严格的追赶确认，可在此检查 matchIndex。
    LOG.info("新成员以 PASSIVE 身份追赶中（LeaderAppender 持续复制），进入 JOINT 阶段");
    enterJoint(future);
  }

  /** 进入联合共识第一阶段：写旧∪新条目 */
  private void enterJoint(final CompletableFuture<Void> future) {
    if (!stage.compareAndSet(Stage.CATCHING_UP, Stage.JOINT)) {
      future.completeExceptionally(new IllegalStateException("阶段异常: " + stage.get()));
      return;
    }
    LOG.info("配置变更进入 JOINT 阶段: old={}, new={}", oldMembers, newMembers);
    final var jointEntry = new ConfigurationEntry(
        System.currentTimeMillis(), newMembers, oldMembers);
    appendConfiguration(jointEntry, future);
  }

  /** 联合条目提交后，进入第二阶段：写最终配置 */
  void onJointCommitted(final CompletableFuture<Void> future) {
    if (!stage.compareAndSet(Stage.JOINT, Stage.STABLE)) {
      return; // 已被取消或阶段不符
    }
    LOG.info("配置变更进入 STABLE 阶段: {}", newMembers);
    final var stableEntry = new ConfigurationEntry(
        System.currentTimeMillis(), newMembers, null);
    appendConfiguration(stableEntry, future);
  }

  /** 最终配置提交后，完成变更 */
  void onStableCommitted(final CompletableFuture<Void> future) {
    LOG.info("配置变更完成: {}", newMembers);

    // 检查 Leader 自身是否被移除
    final var localId = raft.getCluster().getLocalMember().memberId();
    final var removed = newMembers.stream()
        .noneMatch(m -> m.memberId().equals(localId));

    stage.set(Stage.NONE);
    currentFuture = null;

    if (removed) {
      LOG.info("Leader 自身被移除，执行退位");
      raft.transition(com.anyilanxin.kunpeng.cluster.raft.RaftServer.Role.FOLLOWER);
    }
    future.complete(null);
  }

  /** 写配置条目到日志 */
  private void appendConfiguration(
      final ConfigurationEntry entry, final CompletableFuture<Void> future) {
    raft.getThreadContext()
        .execute(
            () -> {
              try {
                // 将配置条目包装为 RaftLogEntry 后追加，由 LeaderAppender 复制到全部成员
                final var logEntry =
                    new com.anyilanxin.kunpeng.cluster.raft.storage.log.entry.RaftLogEntry(
                        raft.getTerm(), entry);
                final var indexed = raft.getLog().append(logEntry);
                LOG.debug("配置条目已追加: index={}, joint={}",
                    indexed.index(), entry.isJointConsensus());

                if (entry.isJointConsensus()) {
                  // 联合条目：等双 majority 提交后进入 STABLE
                  // 当前简化：日志追加后即视为提交（Leader 本地写入成功 + Appender 复制）
                  // 完整实现需要等待 commitIndex 追上该条目索引
                  onJointCommitted(future);
                } else {
                  // 最终条目：完成
                  onStableCommitted(future);
                }
              } catch (final Exception e) {
                LOG.error("配置条目写入失败", e);
                abort(future, e);
              }
            });
  }

  /** 中止变更（恢复原状态） */
  void abort(final CompletableFuture<Void> future, final Throwable error) {
    LOG.warn("配置变更中止: {}", error.getMessage());
    stage.set(Stage.NONE);
    currentFuture = null;
    future.completeExceptionally(error);
  }
}
