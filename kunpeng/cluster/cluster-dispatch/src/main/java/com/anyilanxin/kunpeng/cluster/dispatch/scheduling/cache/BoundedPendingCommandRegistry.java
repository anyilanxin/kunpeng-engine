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
package com.anyilanxin.kunpeng.cluster.dispatch.scheduling.cache;

import com.anyilanxin.kunpeng.cluster.dispatch.scheduling.PendingCommandRegistry;
import com.anyilanxin.kunpeng.protocol.admin.AdminValueLifeCycle;
import java.util.HashMap;
import java.util.Map;
import org.agrona.collections.LongHashSet;

/**
 * 按 {@link AdminValueLifeCycle lifeCycle} 分键、容量有界的线程安全 {@link PendingCommandRegistry}。
 *
 * <p>只跟踪底层映射中存在的 intent，其余 lifeCycle 一律静默忽略。每个被跟踪的 lifeCycle 拥有 独立的 {@link BoundedKeySet}，总上界即各
 * intent 上界之和。
 */
public final class BoundedPendingCommandRegistry implements PendingCommandRegistry.Stageable {

  private final Map<AdminValueLifeCycle, BoundedKeySet> sets;

  /**
   * 基于预构建的 lifeCycle 到集合映射创建注册表。
   *
   * @param sets 被跟踪 lifeCycle 到其有界底层集合的映射；将以不可变方式持有
   */
  public BoundedPendingCommandRegistry(final Map<AdminValueLifeCycle, BoundedKeySet> sets) {
    this.sets = Map.copyOf(sets);
  }

  /** 创建恰好跟踪给定 lifeCycle 的注册表，每个 lifeCycle 由默认容量的集合承载，其大小经 {@code metrics} 上报。 */
  public static BoundedPendingCommandRegistry forIntents(
      final RegistryMetrics metrics, final AdminValueLifeCycle... lifeCycles) {
    final Map<AdminValueLifeCycle, BoundedKeySet> built = new HashMap<>();
    for (final AdminValueLifeCycle lifeCycle : lifeCycles) {
      built.put(lifeCycle, new BoundedKeySet(metrics.forLifeCycle(lifeCycle)));
    }
    return new BoundedPendingCommandRegistry(built);
  }

  @Override
  public void add(final AdminValueLifeCycle lifeCycle, final long key) {
    final var set = sets.get(lifeCycle);
    if (set != null) {
      set.add(key);
    }
  }

  @Override
  public boolean contains(final AdminValueLifeCycle lifeCycle, final long key) {
    final var set = sets.get(lifeCycle);
    return set != null && set.contains(key);
  }

  @Override
  public void remove(final AdminValueLifeCycle lifeCycle, final long key) {
    final var set = sets.get(lifeCycle);
    if (set != null) {
      set.remove(key);
    }
  }

  @Override
  public void clear() {
    sets.values().forEach(BoundedKeySet::clear);
  }

  @Override
  public Staging stage() {
    return new StagedRegistry();
  }

  /**
   * 本注册表上的单个、非线程安全暂存会话。
   *
   * <p>在 {@link #commit()} 或 {@link #rollback()} 被调用前，变更只触碰本地缓冲；读取先查 缓冲、再回落到底层的 per-lifeCycle 集合。
   */
  private final class StagedRegistry implements Staging {

    private final Map<AdminValueLifeCycle, LongHashSet> buffered = new HashMap<>();

    @Override
    public void add(final AdminValueLifeCycle lifeCycle, final long key) {
      if (!sets.containsKey(lifeCycle)) {
        return;
      }
      buffer(lifeCycle).add(key);
    }

    @Override
    public boolean contains(final AdminValueLifeCycle lifeCycle, final long key) {
      final var set = sets.get(lifeCycle);
      if (set == null) {
        return false;
      }
      final var bufferedSet = buffered.get(lifeCycle);
      if (bufferedSet != null && bufferedSet.contains(key)) {
        return true;
      }
      return set.contains(key);
    }

    @Override
    public void remove(final AdminValueLifeCycle lifeCycle, final long key) {
      if (!sets.containsKey(lifeCycle)) {
        return;
      }
      buffer(lifeCycle).remove(key);
    }

    @Override
    public void clear() {
      buffered.values().forEach(LongHashSet::clear);
    }

    @Override
    public void commit() {
      for (final var entry : buffered.entrySet()) {
        final var set = sets.get(entry.getKey());
        if (set != null) {
          set.addAll(entry.getValue());
        }
      }
    }

    @Override
    public void rollback() {
      for (final var entry : buffered.entrySet()) {
        final var set = sets.get(entry.getKey());
        if (set != null) {
          set.removeAll(entry.getValue());
        }
      }
    }

    private LongHashSet buffer(final AdminValueLifeCycle lifeCycle) {
      return buffered.computeIfAbsent(lifeCycle, ignored -> new LongHashSet());
    }
  }
}
