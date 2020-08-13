/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operatorframework.resource.visitor;

import java.util.HashMap;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.function.BiConsumer;
import java.util.function.BinaryOperator;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.function.UnaryOperator;

class PairUpdater<T> extends PairVisitor<T, T> {

  PairUpdater(T toUpdate, T withUpdates) {
    super(toUpdate, withUpdates);
  }

  @Override
  public T result() {
    return left;
  }

  @Override
  public PairVisitor<T, T> transformRight(UnaryOperator<T> rightTransformer) {
    return new PairUpdater<>(left, rightTransformer.apply(right));
  }

  @Override
  public PairVisitor<T, T> transformLeft(UnaryOperator<T> leftTransformer) {
    return new PairUpdater<>(leftTransformer.apply(left), right);
  }

  public PairVisitor<T, T> visit() {
    return returnRightIfNull();
  }

  @Override
  public <O> PairVisitor<T, T> visit(Function<T, O> getter) {
    return this;
  }

  @Override
  public <O> PairVisitor<T, T> visit(Function<T, O> getter, BiConsumer<T, O> setter) {
    return updateAny(getter, setter);
  }

  @Override
  public <O> PairVisitor<T, T> visit(Function<T, O> getter, BiConsumer<T, O> setter,
      O defaultValue) {
    return updateAnyUsingDefaultFrom(getter, setter, t -> defaultValue);
  }

  @Override
  public <O> PairVisitor<T, T> visitUsingDefaultFrom(Function<T, O> getter,
      BiConsumer<T, O> setter, Function<T, O> defaultGetter) {
    return updateAnyUsingDefaultFrom(getter, setter, defaultGetter);
  }

  @Override
  public <O> PairVisitor<T, T> visitTransformed(Function<T, O> getter, BiConsumer<T, O> setter,
      BinaryOperator<O> leftTransformer, BinaryOperator<O> rightTransformer) {
    return updateAnyTransformed(getter, setter, rightTransformer);
  }

  @Override
  public <O, S> PairVisitor<T, T> visitWith(Function<T, O> getter,
      BiConsumer<T, O> setter,
      UnaryOperator<PairVisitor<O, S>> subVisitor) {
    return updateAnyWith(getter, setter, subVisitor);
  }

  @Override
  public <O, S> PairVisitor<T, T> visitWithUsingDefaultFrom(Function<T, O> getter,
      BiConsumer<T, O> setter,
      UnaryOperator<PairVisitor<O, S>> subVisitor,
      Supplier<O> defaultValue) {
    return updateAnyWithUsingDefaultFrom(getter, setter, subVisitor,
        defaultValue);
  }

  PairVisitor<T, T> returnRightIfNull() {
    if (left == null || right == null) {
      return lastResult(right);
    }
    return this;
  }

  <O> PairUpdater<T> updateAny(Function<T, O> getter, BiConsumer<T, O> setter) {
    setter.accept(left, getter.apply(right));
    return this;
  }

  <O> PairUpdater<T> updateAnyUsingDefaultFrom(Function<T, O> getter, BiConsumer<T, O> setter,
      Function<T, O> defaultGetter) {
    setter.accept(left, Optional.ofNullable(getter.apply(right))
        .orElseGet(() -> defaultGetter.apply(right)));
    return this;
  }

  <O> PairUpdater<T> updateAnyTransformed(Function<T, O> getter, BiConsumer<T, O> setter,
      BinaryOperator<O> rightTransformer) {
    setter.accept(left, rightTransformer.apply(getter.apply(left),
        getter.apply(right)));
    return this;
  }

  <O, S> PairUpdater<T> updateAnyWith(Function<T, O> getter, BiConsumer<T, O> setter,
      UnaryOperator<PairVisitor<O, S>> subVisitor) {
    setter.accept(left, subVisitor.apply(get(getter).as()).resultAs());
    return this;
  }

  <O, S> PairUpdater<T> updateAnyWithUsingDefaultFrom(Function<T, O> getter,
      BiConsumer<T, O> setter, UnaryOperator<PairVisitor<O, S>> subVisitor,
      Supplier<O> defaultValue) {
    setter.accept(left, subVisitor.apply(getOrDefault(getter, defaultValue).as()).resultAs());
    return this;
  }

  <O> PairUpdater<O> get(Function<T, O> getter) {
    return new PairUpdater<>(getter.apply(left), getter.apply(right));
  }

  <O> PairUpdater<O> getOrDefault(Function<T, O> getter,
      Supplier<O> defaultValue) {
    return new PairUpdater<>(
        getter.andThen(Optional::ofNullable)
            .andThen(o -> o.orElseGet(defaultValue)).apply(left),
        getter.andThen(Optional::ofNullable)
            .andThen(o -> o.orElseGet(defaultValue)).apply(right));
  }

  @Override
  public <E, O extends List<E>> PairVisitor<T, T> visitList(
      Function<T, O> getter, BiConsumer<T, O> setter) {
    return updateList(getter, setter);
  }

  @Override
  public <E, O extends List<E>, S> PairVisitor<T, T> visitListWith(
      Function<T, O> getter, BiConsumer<T, O> setter,
      UnaryOperator<PairVisitor<E, S>> subVisitor) {
    return updateList(getter, setter, subVisitor);
  }

  <E, O extends List<E>> PairUpdater<T> updateList(
      Function<T, O> getter, BiConsumer<T, O> setter) {
    O leftList = getter.apply(left);
    O rightList = getter.apply(right);
    if (leftList == null || leftList.isEmpty()
        || rightList == null || rightList.isEmpty()) {
      setter.accept(left, rightList);
      return this;
    }
    ListIterator<E> leftListIterator = leftList.listIterator();
    ListIterator<E> rightListIterator = rightList.listIterator();
    while (leftListIterator.hasNext() && rightListIterator.hasNext()) {
      leftListIterator.next();
      E rightElement = rightListIterator.next();
      leftListIterator.set(rightElement);
    }
    while (leftListIterator.hasNext()) {
      leftListIterator.next();
      leftListIterator.remove();
    }
    while (rightListIterator.hasNext()) {
      leftList.add(rightListIterator.next());
    }
    return this;
  }

  <E, O extends List<E>, S> PairUpdater<T> updateList(
      Function<T, O> getter, BiConsumer<T, O> setter,
      UnaryOperator<PairVisitor<E, S>> subVisitor) {
    O leftList = getter.apply(left);
    O rightList = getter.apply(right);
    if (leftList == null || leftList.isEmpty()
        || rightList == null || rightList.isEmpty()) {
      setter.accept(left, rightList);
      return this;
    }
    ListIterator<E> leftListIterator = leftList.listIterator();
    ListIterator<E> rightListIterator = rightList.listIterator();
    while (leftListIterator.hasNext() && rightListIterator.hasNext()) {
      E leftElement = leftListIterator.next();
      E rightElement = rightListIterator.next();
      leftListIterator.set(subVisitor.apply(
          new PairUpdater<E>(leftElement, rightElement).as()).resultAs());
    }
    while (leftListIterator.hasNext()) {
      leftListIterator.next();
      leftListIterator.remove();
    }
    while (rightListIterator.hasNext()) {
      leftList.add(rightListIterator.next());
    }
    return this;
  }

  @Override
  public <K, V, O extends Map<K, V>> PairVisitor<T, T> visitMap(
      Function<T, O> getter) {
    return updateMap(getter, null);
  }

  @Override
  public <K, V, O extends Map<K, V>> PairVisitor<T, T> visitMap(
      Function<T, O> getter, BiConsumer<T, O> setter) {
    return updateMap(getter, setter);
  }

  @Override
  public <K, V, O extends Map<K, V>> PairVisitor<T, T> visitMapKeys(
      Function<T, O> getter) {
    return updateMapKeys(getter, null);
  }

  @Override
  public <K, V, O extends Map<K, V>> PairVisitor<T, T> visitMapKeys(
      Function<T, O> getter, BiConsumer<T, O> setter) {
    return updateMapKeys(getter, setter);
  }

  @Override
  public <K, V, O extends Map<K, V>> PairVisitor<T, T> visitMapTransformed(
      Function<T, O> getter, BiConsumer<T, O> setter,
      BinaryOperator<Entry<K, V>> leftTransformer,
      BinaryOperator<Entry<K, V>> rightTransformer) {
    return updateMapTransformed(getter, setter, leftTransformer, rightTransformer);
  }

  <K, V, O extends Map<K, V>> PairUpdater<T> updateMap(
      Function<T, O> getter, BiConsumer<T, O> setter) {
    O leftMap = getter.apply(left);
    O rightMap = getter.apply(right);
    if (leftMap == null || leftMap.isEmpty()
        || rightMap == null || rightMap.isEmpty()) {
      if (leftMap == null && rightMap != null && setter == null) {
        throw new IllegalStateException();
      }
      if (setter != null) {
        setter.accept(left, rightMap);
        return this;
      }
      if (leftMap != null && leftMap.isEmpty() && rightMap != null) {
        leftMap.putAll(rightMap);
        return this;
      }
      if (leftMap != null) {
        leftMap.clear();
      }
      return this;
    }
    Map<K, V> leftMapCopy = new HashMap<>(leftMap);
    rightMap.entrySet().stream()
        .forEach(rightEntry -> leftMap.put(rightEntry.getKey(), rightEntry.getValue()));
    leftMapCopy.entrySet().stream()
        .filter(leftEntry -> !rightMap.containsKey(leftEntry.getKey()))
        .forEach(leftEntry -> leftMap.remove(leftEntry.getKey()));
    return this;
  }

  <K, V, O extends Map<K, V>> PairUpdater<T> updateMapKeys(
      Function<T, O> getter, BiConsumer<T, O> setter) {
    O leftMap = getter.apply(left);
    O rightMap = getter.apply(right);
    if (leftMap == null || leftMap.isEmpty() || rightMap == null || rightMap.isEmpty()) {
      if (leftMap == null && rightMap != null && setter == null) {
        throw new IllegalStateException();
      }
      if (setter != null) {
        setter.accept(left, rightMap);
        return this;
      }
      if (leftMap != null && leftMap.isEmpty() && rightMap != null) {
        leftMap.putAll(rightMap);
        return this;
      }
      if (leftMap != null) {
        leftMap.clear();
      }
      return this;
    }
    Map<K, V> leftMapCopy = new HashMap<>(leftMap);
    rightMap.entrySet().stream()
        .filter(rightEntry -> !leftMap.containsKey(rightEntry.getKey()))
        .forEach(rightEntry -> leftMap.put(rightEntry.getKey(), rightEntry.getValue()));
    leftMapCopy.entrySet().stream()
        .filter(leftEntry -> !rightMap.containsKey(leftEntry.getKey()))
        .forEach(leftEntry -> leftMap.remove(leftEntry.getKey()));
    return this;
  }

  <K, V, O extends Map<K, V>> PairUpdater<T> updateMapTransformed(
      Function<T, O> getter, BiConsumer<T, O> setter,
      BinaryOperator<Entry<K, V>> leftTransformer,
      BinaryOperator<Entry<K, V>> rightTransformer) {
    O leftMap = getter.apply(left);
    O rightMap = getter.apply(right);
    if (leftMap == null || leftMap.isEmpty() || rightMap == null || rightMap.isEmpty()) {
      if (leftMap == null && rightMap != null && setter == null) {
        throw new IllegalStateException();
      }
      if (setter != null) {
        setter.accept(left, rightMap);
        return this;
      }
      if (leftMap != null && leftMap.isEmpty() && rightMap != null) {
        leftMap.putAll(rightMap);
        return this;
      }
      if (leftMap != null) {
        leftMap.clear();
      }
      return this;
    }
    Map<K, V> leftMapCopy = new HashMap<>(leftMap);
    rightMap.entrySet().stream()
        .map(rightEntry -> rightTransformer.apply(
            leftMap.entrySet()
                .stream()
                .filter(leftEntry -> leftEntry.getKey().equals(rightEntry.getKey()))
                .findAny()
                .map(leftEntry -> leftTransformer.apply(leftEntry, rightEntry))
                .orElse(null),
            rightEntry))
        .forEach(rightEntry -> leftMap.put(rightEntry.getKey(), rightEntry.getValue()));
    leftMapCopy.entrySet().stream()
        .filter(leftEntry -> !rightMap.containsKey(leftEntry.getKey()))
        .forEach(leftEntry -> leftMap.remove(leftEntry.getKey()));
    return this;
  }

}
