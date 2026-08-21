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
package com.anyilanxin.kunpeng.utils;

import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * 二选一值：{@link Left}（错误）或 {@link Right}（成功）。
 *
 * <pre>{@code
 * Either.right(1).get();              // => 1
 * Either.left("error").getLeft();     // => "error"
 * Either.right(1).getLeft();          // 抛 NoSuchElementException
 * }</pre>
 *
 * @param <L> 左值类型（错误）
 * @param <R> 右值类型（成功）
 */
public sealed interface Either<L, R> {

  /** 包装成功值 */
  static <L, R> Either<L, R> right(final R right) {
    return new Right<>(right);
  }

  /** 包装错误值 */
  static <L, R> Either<L, R> left(final L left) {
    return new Left<>(left);
  }

  /** 是否为成功值 */
  boolean isRight();

  /** 是否为错误值 */
  boolean isLeft();

  /**
   * 取成功值
   *
   * @throws NoSuchElementException 若为 {@link Left}
   */
  R get();

  /** 取成功值；为 {@link Left} 时返回默认值 */
  R getOrElse(R defaultValue);

  /**
   * 取错误值
   *
   * @throws NoSuchElementException 若为 {@link Right}
   */
  L getLeft();

  /** 映射成功值；为 {@link Left} 时原样返回 */
  <T> Either<L, T> map(Function<? super R, ? extends T> right);

  /** 左右互换 */
  default Either<R, L> swap() {
    if (isRight()) {
      return Either.left(get());
    } else {
      return Either.right(getLeft());
    }
  }

  /** 映射错误值；为 {@link Right} 时原样返回 */
  <T> Either<T, R> mapLeft(Function<? super L, ? extends T> left);

  /** 成功值flatMap为新的 Either（可转为 Left）；为 {@link Left} 时原样返回 */
  <T> Either<L, T> flatMap(Function<? super R, ? extends Either<L, T>> right);

  /** 对成功值执行副作用后返回自身（支持链式）；为 {@link Left} 时不执行 */
  Either<L, R> thenDo(Consumer<R> action);

  /** 为 {@link Right} 时消费成功值，否则不执行 */
  void ifRight(Consumer<R> action);

  /** 为 {@link Left} 时消费错误值，否则不执行 */
  void ifLeft(Consumer<L> action);

  /** 按左右分别消费对应值 */
  void ifRightOrLeft(Consumer<R> rightAction, Consumer<L> leftAction);

  /** 按左右分别映射后折叠为同一结果类型 */
  <T> T fold(Function<? super L, ? extends T> leftFn, Function<? super R, ? extends T> rightFn);

  /** 成功值载体 */
  @SuppressWarnings("java:S2972")
  record Right<L, R>(R value) implements Either<L, R> {
    @Override
    public boolean isRight() {
      return true;
    }

    @Override
    public boolean isLeft() {
      return false;
    }

    @Override
    public R get() {
      return value;
    }

    @Override
    public R getOrElse(final R defaultValue) {
      return value;
    }

    @Override
    public L getLeft() {
      throw new NoSuchElementException("Expected a left, but this is right");
    }

    @Override
    public <T> Either<L, T> map(final Function<? super R, ? extends T> right) {
      return Either.right(right.apply(value));
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> Either<T, R> mapLeft(final Function<? super L, ? extends T> left) {
      return (Either<T, R>) this;
    }

    @Override
    public <T> Either<L, T> flatMap(final Function<? super R, ? extends Either<L, T>> right) {
      return right.apply(value);
    }

    @Override
    public Either<L, R> thenDo(final Consumer<R> action) {
      action.accept(value);
      return this;
    }

    @Override
    public void ifRight(final Consumer<R> right) {
      right.accept(value);
    }

    @Override
    public void ifLeft(final Consumer<L> action) {
      // do nothing
    }

    @Override
    public void ifRightOrLeft(final Consumer<R> rightAction, final Consumer<L> leftAction) {
      rightAction.accept(value);
    }

    @Override
    public <T> T fold(
        final Function<? super L, ? extends T> leftFn,
        final Function<? super R, ? extends T> rightFn) {
      return rightFn.apply(value);
    }
  }

  /** 错误值载体 */
  @SuppressWarnings("java:S2972")
  record Left<L, R>(L value) implements Either<L, R> {

    @Override
    public boolean isRight() {
      return false;
    }

    @Override
    public boolean isLeft() {
      return true;
    }

    @Override
    public R get() {
      throw new NoSuchElementException("Expected a right, but this is left");
    }

    @Override
    public R getOrElse(final R defaultValue) {
      return defaultValue;
    }

    @Override
    public L getLeft() {
      return value;
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> Either<L, T> map(final Function<? super R, ? extends T> right) {
      return (Either<L, T>) this;
    }

    @Override
    public <T> Either<T, R> mapLeft(final Function<? super L, ? extends T> left) {
      return Either.left(left.apply(value));
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> Either<L, T> flatMap(final Function<? super R, ? extends Either<L, T>> right) {
      return (Either<L, T>) this;
    }

    @Override
    public Either<L, R> thenDo(final Consumer<R> action) {
      return this;
    }

    @Override
    public void ifRight(final Consumer<R> right) {
      // do nothing
    }

    @Override
    public void ifLeft(final Consumer<L> action) {
      action.accept(value);
    }

    @Override
    public void ifRightOrLeft(final Consumer<R> rightAction, final Consumer<L> leftAction) {
      leftAction.accept(value);
    }

    @Override
    public <T> T fold(
        final Function<? super L, ? extends T> leftFn,
        final Function<? super R, ? extends T> rightFn) {
      return leftFn.apply(value);
    }
  }

  /** Optional 到 Either 的桥接：空时转为指定的 Left */
  record EitherOptional<R>(Optional<R> right) {
    public <L> Either<L, R> orElse(final L left) {
      return right.<Either<L, R>>map(Either::right).orElse(Either.left(left));
    }
  }
}
