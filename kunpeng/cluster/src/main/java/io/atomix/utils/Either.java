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
package io.atomix.utils;

import java.util.function.Consumer;
import java.util.function.Function;

/**
 * A value of either a left or a right type, representing an error or a success respectively.
 * Delegates to the shared {@link com.anyilanxin.kunpeng.utils.Either} implementation.
 *
 * @param <L> the left (error) type
 * @param <R> the right (success) type
 */
public final class Either<L, R> {

  private final com.anyilanxin.kunpeng.utils.Either<L, R> delegate;

  private Either(final com.anyilanxin.kunpeng.utils.Either<L, R> delegate) {
    this.delegate = delegate;
  }

  /** @return an Either holding the given left (error) value */
  public static <L, R> Either<L, R> left(final L left) {
    return new Either<>(com.anyilanxin.kunpeng.utils.Either.left(left));
  }

  /** @return an Either holding the given right (success) value */
  public static <L, R> Either<L, R> right(final R right) {
    return new Either<>(com.anyilanxin.kunpeng.utils.Either.right(right));
  }

  /** @return whether this Either holds a right (success) value */
  public boolean isRight() {
    return delegate.isRight();
  }

  /** @return whether this Either holds a left (error) value */
  public boolean isLeft() {
    return delegate.isLeft();
  }

  /** @return the right value; throws if this is a left */
  public R get() {
    return delegate.get();
  }

  /** @return the right value, or the given default when this is a left */
  public R getOrElse(final R defaultValue) {
    return delegate.getOrElse(defaultValue);
  }

  /** @return the left value; throws if this is a right */
  public L getLeft() {
    return delegate.getLeft();
  }

  /** Maps the left value when present; otherwise returns this Either unchanged. */
  public <T> Either<T, R> mapLeft(final Function<? super L, ? extends T> mapper) {
    if (isRight()) {
      return right(get());
    }
    return left(mapper.apply(getLeft()));
  }

  /** Maps the right value when present; otherwise returns this Either unchanged. */
  public <T> Either<L, T> map(final Function<? super R, ? extends T> mapper) {
    if (isLeft()) {
      return left(getLeft());
    }
    return right(mapper.apply(get()));
  }

  /** Runs one of the given consumers depending on the contained value. */
  public void ifRightOrLeft(final Consumer<R> rightConsumer, final Consumer<L> leftConsumer) {
    if (isRight()) {
      rightConsumer.accept(get());
    } else {
      leftConsumer.accept(getLeft());
    }
  }

  @Override
  public boolean equals(final Object o) {
    if (this == o) {
      return true;
    }
    if (!(o instanceof final Either<?, ?> other)) {
      return false;
    }
    return delegate.equals(other.delegate);
  }

  @Override
  public int hashCode() {
    return delegate.hashCode();
  }

  @Override
  public String toString() {
    return isLeft() ? "Left[%s]".formatted(getLeft()) : "Right[%s]".formatted(get());
  }
}
