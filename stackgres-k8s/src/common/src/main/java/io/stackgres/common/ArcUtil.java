/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.common;

public final class ArcUtil {

  private ArcUtil() {}

  public static void checkPublicNoArgsConstructorIsCalledFromArc() {
    StackWalker instance = StackWalker.getInstance(StackWalker.Option.RETAIN_CLASS_REFERENCE);
    String callerClass = instance.getCallerClass().getName();
    if (instance.walk(s -> s.noneMatch(p -> p.getClassName()
        .equals(callerClass + "_Bean"))).booleanValue()) {
      throw new IllegalStateException("Public no-args constructor can only be used "
          + "from ArC context");
    }
  }

}
