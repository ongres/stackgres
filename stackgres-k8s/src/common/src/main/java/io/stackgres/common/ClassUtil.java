/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.common;

import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.Optional;

import org.jooq.lambda.Seq;

public interface ClassUtil {

  static Optional<Field> getDeclaredFieldFromClassHierarchy(
      final String fieldName, final Class<?> clazz) {
    return getDeclaredFieldsFromClassHierarchy(clazz)
        .filter(f -> f.getName().equals(fieldName))
        .findFirst();
  }

  static Seq<Field> getDeclaredFieldsFromClassHierarchy(final Class<?> clazz) {
    return Seq
        .<Class<?>>iterateWhilePresent(
            clazz,
            c -> Optional.ofNullable(c.getSuperclass()).filter(s -> s != Object.class))
        .flatMap(c -> Arrays.asList(c.getDeclaredFields()).stream());
  }

}
