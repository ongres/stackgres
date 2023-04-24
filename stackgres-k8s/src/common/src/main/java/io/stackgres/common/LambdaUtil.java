/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.common;

import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;

public interface LambdaUtil {

  static <T, U> Function<T, U> function(Function<T, U> function) {
    return function;
  }

  static <T, U, V> BiFunction<T, U, V> biFunction(BiFunction<T, U, V> biFunction) {
    return biFunction;
  }

  static <T> Consumer<T> consumer(Consumer<T> consumer) {
    return consumer;
  }

  static <T, U> BiConsumer<T, U> biConsumer(BiConsumer<T, U> biConsumer) {
    return biConsumer;
  }

  static <T> Supplier<T> supplier(Supplier<T> supplier) {
    return supplier;
  }

  static <T> Predicate<T> predicate(Predicate<T> predicate) {
    return predicate;
  }

}
