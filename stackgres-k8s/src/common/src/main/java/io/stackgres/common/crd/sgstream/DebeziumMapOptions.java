/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.common.crd.sgstream;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
public @interface DebeziumMapOptions {

  public static final String DEFAULT_MAP_SEPARATOR = ".";
  public static final int DEFAULT_VALUE_FROM_LEVEL = 2;
  public static final int DEFAULT_PREFIX_FROM_LEVEL = 0;
  public static final boolean DEFAULT_GENERATE_SUMMARY = false;

  boolean generateSummary() default DEFAULT_GENERATE_SUMMARY;

  String separatorLevel0() default DEFAULT_MAP_SEPARATOR;

  String separatorLevel1() default DEFAULT_MAP_SEPARATOR;

  int valueFromLevel() default DEFAULT_VALUE_FROM_LEVEL;

  int prefixFromLevel() default DEFAULT_PREFIX_FROM_LEVEL;

}
