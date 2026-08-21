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
package com.anyilanxin.kunpeng.cluster.utils.concurrent;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicInteger;
import org.assertj.core.api.Assertions;
import org.junit.Test;

/** Ordered completable future test. */
public class OrderedFutureTest {

  /** Tests ordered completion of future callbacks. */
  @Test
  public void testOrderedCompletion() throws Throwable {
    final CompletableFuture<String> future = new OrderedFuture<>();
    final AtomicInteger order = new AtomicInteger();
    future.whenComplete((r, e) -> assertThat(order.incrementAndGet()).isEqualTo(1));
    future.whenComplete((r, e) -> assertThat(order.incrementAndGet()).isEqualTo(2));
    future.handle(
        (r, e) -> {
          assertThat(order.incrementAndGet()).isEqualTo(3);
          assertThat(r).isEqualTo("foo");
          return "bar";
        });
    future.thenRun(() -> assertThat(order.incrementAndGet()).isEqualTo(3));
    future.thenAccept(
        r -> {
          assertThat(order.incrementAndGet()).isEqualTo(5);
          assertThat(r).isEqualTo("foo");
        });
    future.thenApply(
        r -> {
          assertThat(order.incrementAndGet()).isEqualTo(6);
          assertThat(r).isEqualTo("foo");
          return "bar";
        });
    future.whenComplete(
        (r, e) -> {
          assertThat(order.incrementAndGet()).isEqualTo(7);
          assertThat(r).isEqualTo("foo");
        });
    future.complete("foo");
  }

  /** Tests ordered failure of future callbacks. */
  public void testOrderedFailure() throws Throwable {
    final CompletableFuture<String> future = new OrderedFuture<>();
    final AtomicInteger order = new AtomicInteger();
    future.whenComplete((r, e) -> assertThat(order.incrementAndGet()).isEqualTo(1));
    future.whenComplete((r, e) -> assertThat(order.incrementAndGet()).isEqualTo(2));
    future.handle(
        (r, e) -> {
          assertThat(order.incrementAndGet()).isEqualTo(3);
          return "bar";
        });
    future.thenRun(() -> Assertions.fail());
    future.thenAccept(r -> Assertions.fail());
    future.exceptionally(
        e -> {
          assertThat(order.incrementAndGet()).isEqualTo(3);
          return "bar";
        });
    future.completeExceptionally(new RuntimeException("foo"));
  }

  /** Tests calling callbacks that are added after completion. */
  public void testAfterComplete() throws Throwable {
    final CompletableFuture<String> future = new OrderedFuture<>();
    future.whenComplete((result, error) -> assertThat(result).isEqualTo("foo"));
    future.complete("foo");
    final AtomicInteger count = new AtomicInteger();
    future.whenComplete(
        (result, error) -> {
          assertThat(result).isEqualTo("foo");
          assertThat(count.incrementAndGet()).isEqualTo(1);
        });
    future.thenAccept(
        result -> {
          assertThat(result).isEqualTo("foo");
          assertThat(count.incrementAndGet()).isEqualTo(2);
        });
    assertThat(count.get()).isEqualTo(2);
  }
}
