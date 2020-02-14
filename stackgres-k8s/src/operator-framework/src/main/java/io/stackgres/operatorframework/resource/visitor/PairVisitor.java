/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operatorframework.resource.visitor;

import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Supplier;

public abstract class PairVisitor<T, R> {
  protected final T left;
  protected final T right;

  PairVisitor(T left, T right) {
    super();
    this.left = left;
    this.right = right;
  }

  public T getLeft() {
    return left;
  }

  public T getRight() {
    return right;
  }

  public boolean bothInstanceOf(Class<?> resourceClass) {
    return resourceClass.isAssignableFrom(left.getClass())
        && resourceClass.isAssignableFrom(right.getClass());
  }

  @SuppressWarnings("unchecked")
  public <O extends T, S> PairVisitor<O, S> as() {
    return (PairVisitor<O, S>) this;
  }

  public <O extends T, S> PairVisitor<T, R> lastVisit(
      Function<PairVisitor<O, S>, PairVisitor<O, S>> subVisitor) {
    return lastResult(subVisitor.apply(as()).resultAs());
  }

  /**
   * Complete visit if visited pairs are of the same specified class.
   */
  public <O extends T, S> PairVisitor<T, R> lastVisitIfBothInstanceOf(
      Class<O> resourceClass,
      Function<PairVisitor<O, S>, PairVisitor<O, S>> subVisitor) {
    if (!bothInstanceOf(resourceClass)) {
      return this;
    }
    return lastResult(subVisitor.apply(as()).resultAs());
  }

  public abstract R result();

  @SuppressWarnings("unchecked")
  public <S> S resultAs() {
    return (S) result();
  }

  public abstract PairVisitor<T, R> transformRight(Function<T, T> rightTransformer);

  public abstract PairVisitor<T, R> transformLeft(Function<T, T> leftTransformer);

  public abstract PairVisitor<T, R> visit();

  public abstract <O> PairVisitor<T, R> visit(Function<T, O> getter);

  public abstract <O> PairVisitor<T, R> visit(Function<T, O> getter,
      BiConsumer<T, O> setter);

  public abstract <O> PairVisitor<T, R> visit(Function<T, O> getter,
      BiConsumer<T, O> setter, O defaultValue);

  public abstract <O> PairVisitor<T, R> visitUsingDefaultFrom(Function<T, O> getter,
      BiConsumer<T, O> setter, Function<T, O> defaultGetter);

  public abstract <O> PairVisitor<T, R> visitTransformed(
      Function<T, O> getter, BiConsumer<T, O> setter,
      BiFunction<O, O, O> leftTransformer, BiFunction<O, O, O> rightTransformer);

  public abstract <O, S> PairVisitor<T, R> visitWith(Function<T, O> getter,
      BiConsumer<T, O> setter,
      Function<PairVisitor<O, S>, PairVisitor<O, S>> subVisitor);

  public abstract <O, S> PairVisitor<T, R> visitWithUsingDefaultFrom(Function<T, O> getter,
      BiConsumer<T, O> setter,
      Function<PairVisitor<O, S>, PairVisitor<O, S>> subVisitor,
      Supplier<O> defaultValue);

  public abstract <E, O extends List<E>> PairVisitor<T, R> visitList(
      Function<T, O> getter, BiConsumer<T, O> setter);

  public abstract <E, O extends List<E>, S> PairVisitor<T, R> visitListWith(
      Function<T, O> getter, BiConsumer<T, O> setter,
      Function<PairVisitor<E, S>, PairVisitor<E, S>> tester);

  public abstract <K, V, O extends Map<K, V>> PairVisitor<T, R> visitMap(
      Function<T, O> getter);

  public abstract <K, V, O extends Map<K, V>> PairVisitor<T, R> visitMap(
      Function<T, O> getter, BiConsumer<T, O> setter);

  public abstract <K, V, O extends Map<K, V>> PairVisitor<T, R> visitMapKeys(
      Function<T, O> getter);

  public abstract <K, V, O extends Map<K, V>> PairVisitor<T, R> visitMapKeys(
      Function<T, O> getter, BiConsumer<T, O> setter);

  public abstract <K, V, O extends Map<K, V>>
      PairVisitor<T, R> visitMapTransformed(
          Function<T, O> getter, BiConsumer<T, O> setter,
          BiFunction<Entry<K, V>, Entry<K, V>, Entry<K, V>> leftTransformer,
          BiFunction<Entry<K, V>, Entry<K, V>, Entry<K, V>> rightTransformer);

  public PairVisitor<T, R> lastResult(R result) {
    return new LastResult(left, right, result);
  }

  class LastResult extends PairVisitor<T, R> {
    private final R result;

    LastResult(T left, T right, R result) {
      super(left, right);
      this.result = result;
    }

    @Override
    public <O extends T, S> PairVisitor<T, R> lastVisit(
        Function<PairVisitor<O, S>, PairVisitor<O, S>> subVisitor) {
      return this;
    }

    @Override
    public <O extends T, S> PairVisitor<T, R> lastVisitIfBothInstanceOf(
        Class<O> resourceClass,
        Function<PairVisitor<O, S>, PairVisitor<O, S>> subVisitor) {
      return this;
    }

    @Override
    public R result() {
      return result;
    }

    @Override
    public PairVisitor<T, R> transformRight(Function<T, T> rightTransformer) {
      return this;
    }

    @Override
    public PairVisitor<T, R> transformLeft(Function<T, T> leftTransformer) {
      return this;
    }

    @Override
    public PairVisitor<T, R> visit() {
      return this;
    }

    @Override
    public <O> PairVisitor<T, R> visit(Function<T, O> getter) {
      return this;
    }

    @Override
    public <O> PairVisitor<T, R> visit(Function<T, O> getter, BiConsumer<T, O> setter) {
      return this;
    }

    @Override
    public <O> PairVisitor<T, R> visit(Function<T, O> getter, BiConsumer<T, O> setter,
        O defaultValue) {
      return this;
    }

    @Override
    public <O> PairVisitor<T, R> visitUsingDefaultFrom(Function<T, O> getter,
        BiConsumer<T, O> setter, Function<T, O> defaultGetter) {
      return this;
    }

    @Override
    public <O> PairVisitor<T, R> visitTransformed(Function<T, O> getter, BiConsumer<T, O> setter,
        BiFunction<O, O, O> leftTransformer, BiFunction<O, O, O> rightTransformer) {
      return this;
    }

    @Override
    public <O, S> PairVisitor<T, R> visitWith(Function<T, O> getter,
        BiConsumer<T, O> setter,
        Function<PairVisitor<O, S>, PairVisitor<O, S>> subVisitor) {
      return this;
    }

    @Override
    public <O, S> PairVisitor<T, R> visitWithUsingDefaultFrom(
        Function<T, O> getter, BiConsumer<T, O> setter,
        Function<PairVisitor<O, S>, PairVisitor<O, S>> subVisitor,
        Supplier<O> defaultValue) {
      return this;
    }

    @Override
    public <E, O extends List<E>> PairVisitor<T, R> visitList(
        Function<T, O> getter, BiConsumer<T, O> setter) {
      return this;
    }

    @Override
    public <E, O extends List<E>, S> PairVisitor<T, R> visitListWith(
        Function<T, O> getter, BiConsumer<T, O> setter,
        Function<PairVisitor<E, S>, PairVisitor<E, S>> tester) {
      return this;
    }

    @Override
    public <K, V, O extends Map<K, V>> PairVisitor<T, R> visitMap(
        Function<T, O> getter) {
      return this;
    }

    @Override
    public <K, V, O extends Map<K, V>> PairVisitor<T, R> visitMap(
        Function<T, O> getter, BiConsumer<T, O> setter) {
      return this;
    }

    @Override
    public <K, V, O extends Map<K, V>> PairVisitor<T, R> visitMapKeys(
        Function<T, O> getter, BiConsumer<T, O> setter) {
      return this;
    }

    @Override
    public <K, V, O extends Map<K, V>> PairVisitor<T, R> visitMapKeys(
        Function<T, O> getter) {
      return this;
    }

    @Override
    public <K, V, O extends Map<K, V>>
        PairVisitor<T, R> visitMapTransformed(
            Function<T, O> getter, BiConsumer<T, O> setter,
            BiFunction<Entry<K, V>, Entry<K, V>, Entry<K, V>> leftTransformer,
            BiFunction<Entry<K, V>, Entry<K, V>, Entry<K, V>> rightTransformer) {
      return this;
    }

    @Override
    public PairVisitor<T, R> lastResult(R result) {
      return this;
    }

  }

}
