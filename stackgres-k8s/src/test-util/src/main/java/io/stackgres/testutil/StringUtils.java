/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.testutil;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.Random;

import javax.annotation.Nonnull;

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

  @Nonnull
  public static String getRandomNamespace() {
    String namespace = sanitize(getRandomString());
    namespace = namespace.replaceAll("^\\d", "a");
    if (namespace.length() >= 63) {
      return namespace.substring(0, 62);
    }
    return namespace;
  }

  @Nonnull
  public static String getRandomResourceName() {
    String clusterName = sanitize(getRandomString());
    clusterName = clusterName.replaceAll("^\\d", "a");
    if (clusterName.length() >= 53) {
      return clusterName.substring(0, 52);
    }
    return clusterName;
  }

  @Nonnull
  public static String getRandomResourceName(int size) {
    String clusterName = sanitize(sanitize(getRandomString(size)));
    clusterName = clusterName.replaceAll("^\\d", "a");
    if (clusterName.length() >= size) {
      return clusterName.substring(0, size - 1);
    }
    return clusterName;
  }

  @Nonnull
  public static String getRandomResourceNameWithExactlySize(int size) {
    String clusterName = sanitize(sanitize(getRandomString(size)));
    clusterName = clusterName.replaceAll("^\\d", "a");
    return clusterName;
  }

  public static String readString(String resource) {
    try (InputStream is = ClassLoader.getSystemResourceAsStream(resource)) {
      if (is == null) {
        throw new IllegalArgumentException("resource " + resource + " not found");
      }

      return new String(is.readAllBytes(), StandardCharsets.UTF_8);
    } catch (IOException ex) {
      throw new IllegalArgumentException("could not open resource " + resource, ex);
    }
  }
}
