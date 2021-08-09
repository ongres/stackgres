/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.conciliation.factory.cluster.sidecars.pooling.parameters;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.util.Properties;
import java.util.Set;

import com.google.common.collect.ImmutableSet;

public class PgBouncerBlocklist {

  private static final Set<String> BLOCKLIST;

  static {
    BLOCKLIST = readResource().entrySet().stream()
        .filter(e -> !e.getKey().toString().isEmpty())
        .map(e -> e.getKey().toString())
        .collect(ImmutableSet.toImmutableSet());
  }

  private PgBouncerBlocklist() {}

  private static Properties readResource() {
    Properties properties = new Properties();
    try (InputStream is = PgBouncerBlocklist.class.getResourceAsStream(
        "/pgbouncer-blocklist.properties")) {
      if (is == null) {
        throw new IOException("Couldn't read pgbouncer-blocklist.properties");
      }
      properties.load(is);
    } catch (IOException ex) {
      throw new UncheckedIOException(ex);
    }
    return properties;
  }

  public static Set<String> getBlocklistParameters() {
    return BLOCKLIST;
  }

}
