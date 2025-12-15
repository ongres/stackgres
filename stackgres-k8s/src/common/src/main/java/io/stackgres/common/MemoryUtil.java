/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.common;

import org.github.jamm.MemoryMeter;

public final class MemoryUtil {

  private MemoryUtil() {
    throw new AssertionError("Utility class");
  }

  public static long measureDeep(Object object) {
    try {
      return MemoryMeter.builder()
          .printVisitedTree()
          .build()
          .measureDeep(object);
    } catch (RuntimeException ex) {
      ex.printStackTrace();
      throw ex;
    }
  }

}
