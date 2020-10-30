/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.common;

public final class CdiUtil {

  private CdiUtil() {}

  public static void checkPublicNoArgsConstructorIsCalledToCreateProxy() {
    StackWalker instance = StackWalker.getInstance(StackWalker.Option.RETAIN_CLASS_REFERENCE);
    String callerClass = instance.getCallerClass().getName();
    if (instance.walk(s -> s
        .noneMatch(p -> p.getClassName().equals(callerClass + "_Bean")
            && p.getMethodName().equals("proxy")))
        .booleanValue()) {
      throw new IllegalStateException("Public no-args constructor can only be used"
          + " to create a dummy proxy instance."
          + " See https://quarkus.io/guides/cdi-reference#simplified-constructor-injection");
    }
  }

}
