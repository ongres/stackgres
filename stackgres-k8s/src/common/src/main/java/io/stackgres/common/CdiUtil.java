/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.common;

public final class CdiUtil {

  private CdiUtil() {}

  public static void checkPublicNoArgsConstructorIsCalledToCreateProxy(Class<?> callerClass) {
    StackWalker instance = StackWalker.getInstance(StackWalker.Option.RETAIN_CLASS_REFERENCE);
    String callerClassName = callerClass.getName().replaceAll("_[^_]+$", "");
    String callerClassAltName = instance.getCallerClass().getName();
    if (instance.walk(s -> s
        .noneMatch(p -> (p.getClassName().equals(callerClassName + "_Bean")
            || p.getClassName().equals(callerClassAltName + "_Bean"))
            && p.getMethodName().equals("proxy")))
        .booleanValue()) {
      throw new IllegalStateException("Public no-args constructor can only be used"
          + " to create a dummy proxy instance."
          + " See https://quarkus.io/guides/cdi-reference#simplified-constructor-injection");
    }
  }

}
