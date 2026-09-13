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
package com.anyilanxin.kunpeng.cluster.dispatch.scheduling;

import java.time.*;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicReference;

/**
 * 调度用的时间源，可按需调整以支持测试或重放。
 *
 * <p>普通 {@link #passthrough} 时钟只是透传其时间源。{@link Adjustable} 时钟在时间源之上 叠加单个 {@link
 * Adjustable.Adjustment}：{@link Adjustable.Adjustment.Shift} 相对时间源
 * 持续偏移（因此持续追踪时间源，而非冻结应用时刻的值），{@link Adjustable.Adjustment.Pin} 则将时间彻底固定。应用新调整会替换旧调整而非叠加；{@link
 * Adjustable#stackOffset} 是 显式扩展现有偏移的途径。
 */
public interface TimerClock extends InstantSource {

  /** 返回透明转发到 {@code source} 的时钟。 */
  static TimerClock passthrough(final InstantSource source) {
    return new PassthroughClock(source);
  }

  /** 基于系统时钟返回透传时钟。 */
  static TimerClock systemClock() {
    return passthrough(InstantSource.system());
  }

  /** 基于 {@code source} 返回可调时钟。 */
  static Adjustable adjustable(final InstantSource source) {
    return new AdjustableClock(source);
  }

  /** 返回当前调整；未调整时返回 {@link Adjustable.Adjustment.None}。 */
  Adjustable.Adjustment adjustment();

  /** 时间可被调整的时钟。 */
  interface Adjustable extends TimerClock {

    /**
     * 应用给定调整，替换任何已有调整。
     *
     * @param adjustment 要应用的调整；传 {@link Adjustment#none()} 表示复位
     */
    void apply(Adjustment adjustment);

    /** 将时钟固定到给定时刻。 */
    default void pin(final Instant at) {
      apply(Adjustment.pin(at));
    }

    /** 将时钟偏移给定时长。 */
    default void offset(final Duration by) {
      apply(Adjustment.shift(by));
    }

    /** 在已有偏移上扩展 {@code additional}；若无偏移则直接偏移 {@code additional}。 */
    default void stackOffset(final Duration additional) {
      if (adjustment() instanceof final Adjustment.Shift s) {
        offset(s.by().plus(additional));
      } else {
        offset(additional);
      }
    }

    /** 将时钟复位到其时间源。 */
    default void reset() {
      apply(Adjustment.none());
    }

    /** 返回时钟当前是否已相对时间源被调整。 */
    default boolean isAdjusted() {
      return !(adjustment() instanceof Adjustment.None);
    }

    /** 应用到 {@link Adjustable} 时钟的单次调整。 */
    sealed interface Adjustment {
      static None none() {
        return new None();
      }

      static Pin pin(final Instant at) {
        return new Pin(at);
      }

      static Shift shift(final Duration by) {
        return new Shift(by);
      }

      record None() implements Adjustment {}

      record Pin(Instant at) implements Adjustment {}

      record Shift(Duration by) implements Adjustment {}
    }
  }

  /** 将每次读取都转发给时间源的时钟。 */
  final class PassthroughClock implements TimerClock {
    private final InstantSource source;

    PassthroughClock(final InstantSource source) {
      this.source = Objects.requireNonNull(source);
    }

    @Override
    public Adjustable.Adjustment adjustment() {
      return Adjustable.Adjustment.none();
    }

    @Override
    public Instant instant() {
      return source.instant();
    }

    @Override
    public long millis() {
      return source.millis();
    }

    @Override
    public Clock withZone(final ZoneId zone) {
      return source.withZone(zone);
    }

    @Override
    public int hashCode() {
      return Objects.hashCode(source);
    }

    @Override
    public boolean equals(final Object o) {
      if (this == o) {
        return true;
      }
      if (!(o instanceof final PassthroughClock that)) {
        return false;
      }
      return Objects.equals(source, that.source);
    }

    @Override
    public String toString() {
      return "PassthroughClock{source=" + source + '}';
    }
  }

  /** 在时间源之上以原子替换单个调整的时钟。 */
  final class AdjustableClock implements Adjustable {
    private final InstantSource source;
    private final AtomicReference<Adjustment> adjustment = new AtomicReference<>();

    AdjustableClock(final InstantSource source) {
      this.source = Objects.requireNonNull(source);
      adjustment.set(Adjustment.none());
    }

    @Override
    public void apply(final Adjustment adjustment) {
      this.adjustment.set(Objects.requireNonNull(adjustment));
    }

    @Override
    public Adjustment adjustment() {
      return adjustment.get();
    }

    @Override
    public Instant instant() {
      final Adjustment current = adjustment.get();
      return switch (current) {
        case Adjustment.None() -> source.instant();
        case Adjustment.Pin(final var at) -> at;
        case Adjustment.Shift(final var by) -> source.instant().plus(by);
      };
    }

    @Override
    public long millis() {
      final Adjustment current = adjustment.get();
      return switch (current) {
        case Adjustment.None() -> source.millis();
        case Adjustment.Pin(final var at) -> at.toEpochMilli();
        case Adjustment.Shift(final var by) -> source.millis() + by.toMillis();
      };
    }

    @Override
    public int hashCode() {
      return Objects.hash(source, adjustment.get());
    }

    @Override
    public boolean equals(final Object o) {
      if (this == o) {
        return true;
      }
      if (!(o instanceof final AdjustableClock that)) {
        return false;
      }
      return Objects.equals(source, that.source)
          && Objects.equals(adjustment.get(), that.adjustment.get());
    }

    @Override
    public String toString() {
      return "AdjustableClock{source=" + source + ", adjustment=" + adjustment.get() + '}';
    }
  }
}
