/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.common;

import java.util.Arrays;

import io.quarkus.arc.Arc;

public class ArcUtil {

  public static void checkPublicNoArgsConstructorIsCalledFromArc() {
    if (Arrays.asList(new Exception().fillInStackTrace()
        .getStackTrace())
        .stream()
        .noneMatch(stackTraceElement -> stackTraceElement.getClassName()
            .equals(Arc.class.getName()))) {
      throw new IllegalStateException("Public no-args constructor can only be used from "
          + Arc.class.getName() + " class");
    }
  }

}
