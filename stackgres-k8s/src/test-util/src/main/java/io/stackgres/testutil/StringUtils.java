/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.testutil;

import java.util.Locale;
import java.util.Random;

import org.jetbrains.annotations.NotNull;

public class StringUtils {

  public static String getRandomString() {

    return getRandomString(new Random().nextInt(128) + 1);

  }

  public static String getRandomString(int length) {
    int leftLimit = 48; // numeral '0'
    int rightLimit = 122; // letter 'z'

    final Random random = new Random();

    return random.ints(leftLimit, rightLimit + 1)
        .filter(i -> (i <= 57 || i >= 65) && (i <= 90 || i >= 97))
        .limit(length)
        .collect(StringBuilder::new, StringBuilder::appendCodePoint, StringBuilder::append)
        .toString();
  }

  private static String sanitize(String s) {
    return s.toLowerCase(Locale.ENGLISH).replaceAll("^\\d", "a");
  }

  @NotNull
  public static String getRandomNamespace() {
    String namespace = sanitize(getRandomString());
    namespace = namespace.replaceAll("^\\d", "a");
    if (namespace.length() >= 63) {
      return namespace.substring(0, 62);
    }
    return namespace;
  }

  @NotNull
  public static String getRandomClusterName() {
    String clusterName = sanitize(getRandomString());
    clusterName = clusterName.replaceAll("^\\d", "a");
    if (clusterName.length() >= 59) {
      return clusterName.substring(0, 58);
    }
    return clusterName;
  }
}
