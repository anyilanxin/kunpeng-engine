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
package com.anyilanxin.kunpeng.scheduler.core;

import com.anyilanxin.kunpeng.scheduler.future.ActorFuture;
import java.util.concurrent.Callable;

/** 一次调用绑定的一次性/复用信封（fast-lane 与订阅轮询走 owner 池化复用） */
public final class ActorEnvelope {

  public enum Kind {
    RUN,
    CALL,
    REPEAT,
    SUBSCRIPTION,
    CLOSE_REQUEST,
    LIFECYCLE
  }

  private Kind kind;
  private Runnable action;
  private Callable<?> callable;
  private ActorFuture<?> future;
  private SubscriptionSlot slot;
  private Phases runInPhase;

  public ActorEnvelope wrap(final Kind kind, final Runnable action, final ActorFuture<?> future) {
    this.kind = kind;
    this.action = action;
    this.callable = null;
    this.future = future;
    this.slot = null;
    this.runInPhase = null;
    return this;
  }

  public ActorEnvelope wrapCall(final Callable<?> callable, final ActorFuture<?> future) {
    this.kind = Kind.CALL;
    this.action = null;
    this.callable = callable;
    this.future = future;
    this.slot = null;
    return this;
  }

  public ActorEnvelope wrapSubscription(final SubscriptionSlot slot) {
    this.kind = Kind.SUBSCRIPTION;
    this.action = null;
    this.callable = null;
    this.future = null;
    this.slot = slot;
    return this;
  }

  public Kind getKind() {
    return kind;
  }

  public Runnable getAction() {
    return action;
  }

  public Callable<?> getCallable() {
    return callable;
  }

  @SuppressWarnings("unchecked")
  public <T> ActorFuture<T> getFuture() {
    return (ActorFuture<T>) future;
  }

  public SubscriptionSlot getSlot() {
    return slot;
  }

  public Phases getRunInPhase() {
    return runInPhase;
  }

  public void setRunInPhase(final Phases phase) {
    this.runInPhase = phase;
  }

  public void reset() {
    kind = null;
    action = null;
    callable = null;
    future = null;
    slot = null;
    runInPhase = null;
  }
}
