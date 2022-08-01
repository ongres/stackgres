/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.fixture.processor;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Target;

/**
 * Generates an enum with all the paths of the files contained in the path tree under the root path.
 */
@Documented
@Target(ElementType.TYPE)
public @interface PathsAsConstants {

  /**
   * The path root.
   */
  String value() default "";

  String regExp() default "^.*$";

}
