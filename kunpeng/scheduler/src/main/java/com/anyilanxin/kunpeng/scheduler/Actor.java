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
package com.anyilanxin.kunpeng.scheduler;

import com.anyilanxin.kunpeng.scheduler.future.ActorFuture;
import java.time.Duration;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

/** actor 基类：逻辑单线程实体。生命周期钩子由调度器按相位触发；job 失败语义按相位分流。 */
public abstract class Actor implements ConcurrencyControl, AsyncClosable, AutoCloseable {

  protected final ActorControl actor = new ActorControl(this);

  private final String name;
  // 惰性初始化: 子类 createContext 可能读取子类字段, 构造期调用会读到未初始化值
  private volatile Map<String, String> context;

  protected Actor() {
    this.name = getClass().getSimpleName();
  }

  // ===== 生命周期钩子（调度器触发） =====

  protected void onActorStarting() {}

  protected void onActorStarted() {}

  protected void onActorCloseRequested() {}

  protected void onActorClosing() {}

  protected void onActorClosed() {}

  protected void onActorFailed() {}

  /** STARTED 相位 job 失败的默认处理（记录日志） */
  protected void handleFailure(final Throwable failure) {
    // 子类可覆写; 默认仅由调度器记录
  }

  protected Map<String, String> createContext() {
    return new HashMap<>();
  }

  // ===== 调度核跨包触发桥（不进公共契约） =====

  public void internalOnActorStarting() {
    onActorStarting();
  }

  public void internalOnActorStarted() {
    onActorStarted();
  }

  public void internalOnActorCloseRequested() {
    onActorCloseRequested();
  }

  public void internalOnActorClosing() {
    onActorClosing();
  }

  public void internalOnActorClosed() {
    onActorClosed();
  }

  public void internalOnActorFailed() {
    onActorFailed();
  }

  public void internalHandleFailure(final Throwable failure) {
    handleFailure(failure);
  }

  // ===== 基础信息 =====

  public String getName() {
    return name;
  }

  public Map<String, String> getContext() {
    if (context == null) {
      context = java.util.Collections.unmodifiableMap(createContext());
    }
    return context;
  }

  public ActorControl getControl() {
    return actor;
  }

  public static String buildActorName(final String firstName, final int secondName) {
    return firstName + "-" + secondName;
  }

  public static String buildActorName(final String firstName, final String secondName) {
    return firstName + "-" + secondName;
  }

  public static ActorBuilder newActor() {
    return new ActorBuilder();
  }

  /** actor 构建器（name + 启动钩子） */
  public static final class ActorBuilder {
    private String name;
    private Consumer<ActorControl> startedHandler;

    public ActorBuilder name(final String name) {
      this.name = name;
      return this;
    }

    public ActorBuilder actorStartedHandler(final Consumer<ActorControl> handler) {
      this.startedHandler = handler;
      return this;
    }

    public Actor build() {
      return new Actor() {
        @Override
        public String getName() {
          return name != null ? name : "actor";
        }

        @Override
        protected void onActorStarted() {
          if (startedHandler != null) {
            startedHandler.accept(actor);
          }
        }
      };
    }
  }

  public static Actor wrap(final Consumer<ActorControl> r) {
    return new Actor() {
      @Override
      public void onActorStarted() {
        r.accept(actor);
      }

      @Override
      public String getName() {
        return "wrapped-actor";
      }
    };
  }

  // ===== 提交（任意线程） =====

  public void run(final Runnable action) {
    actor.run(action);
  }

  public void submit(final Runnable action) {
    actor.submit(action);
  }

  public <T> ActorFuture<T> call(final Callable<T> callable) {
    return actor.call(callable);
  }

  public <T> void runOnCompletion(
      final ActorFuture<T> future, final BiConsumer<T, Throwable> callback) {
    actor.runOnCompletion(future, callback);
  }

  public <T> void runOnCompletion(
      final Collection<ActorFuture<T>> futures, final Consumer<Throwable> callback) {
    actor.runOnCompletion(futures, callback);
  }

  public ScheduledTimer schedule(final Duration delay, final Runnable runnable) {
    return actor.schedule(delay, runnable);
  }

  // ===== 关闭 =====

  public ActorFuture<Void> closeAsync() {
    return actor.close();
  }

  public boolean isActorClosed() {
    return actor.isClosed();
  }

  /** 同步关闭（最多 300s） */
  @Override
  public void close() {
    try {
      closeAsync().get(300, java.util.concurrent.TimeUnit.SECONDS);
    } catch (final Exception e) {
      final var cause = e.getCause() != null ? e.getCause() : e;
      throw new RuntimeException("Failed to close actor " + name, cause);
    }
  }

  /** 显式失败（任意相位致死） */
  public void onActorFailed(final Throwable error) {
    actor.fail(error);
  }
}
