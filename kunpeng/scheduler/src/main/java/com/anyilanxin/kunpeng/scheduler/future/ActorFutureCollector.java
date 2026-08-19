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
package com.anyilanxin.kunpeng.scheduler.future;

import com.anyilanxin.kunpeng.scheduler.ConcurrencyControl;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.BinaryOperator;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collector;

/** 收集 ActorFuture 流为聚合 future：全成功 → 有序值列表; 任一失败 → 聚合异常 */
public final class ActorFutureCollector<V>
    implements Collector<ActorFuture<V>, List<ActorFuture<V>>, ActorFuture<List<V>>> {

  private final ConcurrencyControl concurrencyControl;

  public ActorFutureCollector(final ConcurrencyControl concurrencyControl) {
    this.concurrencyControl = Objects.requireNonNull(concurrencyControl);
  }

  @Override
  public Supplier<List<ActorFuture<V>>> supplier() {
    return ArrayList::new;
  }

  @Override
  public BiConsumer<List<ActorFuture<V>>, ActorFuture<V>> accumulator() {
    return List::add;
  }

  @Override
  public BinaryOperator<List<ActorFuture<V>>> combiner() {
    return (listA, listB) -> {
      listA.addAll(listB);
      return listA;
    };
  }

  @Override
  public Function<List<ActorFuture<V>>, ActorFuture<List<V>>> finisher() {
    return futures -> {
      final CompletableActorFuture<List<V>> aggregated = new CompletableActorFuture<>();
      if (futures.isEmpty()) {
        aggregated.complete(List.of());
        return aggregated;
      }
      final Object[] results = new Object[futures.size()];
      final int[] remaining = {futures.size()};
      final List<Throwable> failures = new ArrayList<>();
      for (int i = 0; i < futures.size(); i++) {
        final int index = i;
        concurrencyControl.runOnCompletion(
            futures.get(index),
            (value, error) -> {
              if (error != null) {
                synchronized (failures) {
                  failures.add(error);
                }
              } else {
                results[index] = value;
              }
              if (remaining[0] == 0 || --remaining[0] == 0) {
                if (failures.isEmpty()) {
                  final List<V> values = new ArrayList<>(results.length);
                  for (final Object result : results) {
                    values.add((V) result);
                  }
                  aggregated.complete(values);
                } else {
                  final var aggregate = new RuntimeException("Aggregated futures failed");
                  synchronized (failures) {
                    failures.forEach(aggregate::addSuppressed);
                  }
                  aggregated.completeExceptionally(aggregate);
                }
              }
            });
      }
      return aggregated;
    };
  }

  @Override
  public Set<Characteristics> characteristics() {
    return Set.of();
  }
}
