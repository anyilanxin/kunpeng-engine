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
package com.anyilanxin.kunpeng.cluster.raft.orchestrator;

import com.anyilanxin.kunpeng.scheduler.future.ActorFuture;
import com.anyilanxin.kunpeng.scheduler.startup.StartupStep;

/**
 * 分区组启动步骤。
 *
 * <p>实现 scheduler 的 {@link StartupStep}，由 {@code StartupProcess} 调度：
 * 启动链按注册<b>顺序</b>执行 {@link #startup}，关闭链按<b>倒序</b>执行 {@link #shutdown}。
 *
 * <p>在启动步骤列表中，位于 {@code RaftPartitionGroupStartupStep} 之前的为"前置启动"
 * （此时 {@link RaftGroupContext#partitionGroup()} 尚不可用），之后的为"后置启动"。
 *
 * @param <T> 上下文类型，必须继承 {@link RaftGroupContext}
 */
public interface PartitionStartup<T extends RaftGroupContext> extends StartupStep<T> {

  @Override
  String getName();

  /** 启动（StartupProcess 按列表顺序调用） */
  @Override
  ActorFuture<T> startup(T context);

  /** 关闭（StartupProcess 按列表倒序调用） */
  @Override
  ActorFuture<T> shutdown(T context);
}
