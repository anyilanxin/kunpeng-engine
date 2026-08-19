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

import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collector;

/** 三态结果：Left（错误）/Right（成功） */
public sealed interface Either<L, R> {

  static <L, R> Either<L, R> right(final R right) {
    return new Right<>(right);
  }

  static <L, R> Either<L, R> left(final L left) {
    return new Left<>(left);
  }

  static <R> EitherOptional<R> ofOptional(final Optional<R> right) {
    return new EitherOptional<>(right);
  }

  /** 收集为 Either<List<L>, List<R>>：任一 Left 则整体 Left */
  static <L, R>
      Collector<Either<L, R>, Tuple<List<L>, List<R>>, Either<List<L>, List<R>>> collector() {
    return Collector.of(
        () -> new Tuple<>(new ArrayList<>(), new ArrayList<>()),
        (acc, next) ->
            next.ifRightOrLeft(right -> acc.getRight().add(right), left -> acc.getLeft().add(left)),
        (a, b) -> {
          a.getLeft().addAll(b.getLeft());
          a.getRight().addAll(b.getRight());
          return a;
        },
        acc -> !acc.getLeft().isEmpty() ? left(acc.getLeft()) : right(acc.getRight()));
  }

  /** 收集时折叠首个 Left */
  static <L, R>
      Collector<Either<L, R>, Tuple<Optional<L>, List<R>>, Either<L, List<R>>>
          collectorFoldingLeft() {
    return Collector.of(
        () -> new Tuple<>(Optional.empty(), new ArrayList<>()),
        (acc, next) ->
            next.ifRightOrLeft(
                right -> acc.getRight().add(right),
                left -> acc.setLeft(acc.getLeft().or(() -> Optional.of(left)))),
        (a, b) -> {
          if (a.getLeft().isEmpty() && b.getLeft().isPresent()) {
            a.setLeft(b.getLeft());
          }
          a.getRight().addAll(b.getRight());
          return a;
        },
        acc ->
            acc.getLeft()
                .<Either<L, List<R>>>map(Either::left)
                .orElse(Either.right(acc.getRight())));
  }

  boolean isRight();

  boolean isLeft();

  R get();

  R getOrElse(R defaultValue);

  L getLeft();

  <T> Either<L, T> map(Function<? super R, ? extends T> right);

  <T> Either<T, R> mapLeft(Function<? super L, ? extends T> left);

  <T> Either<L, T> flatMap(Function<? super R, ? extends Either<L, T>> right);

  Either<L, R> thenDo(Consumer<R> action);

  void ifRight(Consumer<R> action);

  void ifLeft(Consumer<L> action);

  void ifRightOrLeft(Consumer<R> rightAction, Consumer<L> leftAction);

  <T> T fold(Function<? super L, ? extends T> leftFn, Function<? super R, ? extends T> rightFn);

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
      // right 无 left 分支
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
      // left 无 right 分支
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

  record EitherOptional<R>(Optional<R> right) {
    public <L> Either<L, R> orElse(final L left) {
      return right.<Either<L, R>>map(Either::right).orElse(Either.left(left));
    }
  }
}
