/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.common;

import java.security.SecureRandom;

public final class StringUtil {

  private StringUtil() {
    throw new AssertionError("Utility class");
  }

  /**
   * Random string with alphanumeric characters between 8 and 64.
   */
  public static String generateRandom() {
    final SecureRandom srand = new SecureRandom();
    return StringUtil.generateRandom(srand.nextInt(56) + 8);
  }

  /**
   * Random string with alphanumeric characters with specified lenght.
   */
  public static String generateRandom(int length) {
    final SecureRandom srand = new SecureRandom();
    return srand.ints('0', 'z' + 1)
        .filter(i -> (i <= '9' || i >= 'A') && (i <= 'Z' || i >= 'a'))
        .limit(length)
        .collect(StringBuilder::new, StringBuilder::appendCodePoint, StringBuilder::append)
        .toString();
  }

}
