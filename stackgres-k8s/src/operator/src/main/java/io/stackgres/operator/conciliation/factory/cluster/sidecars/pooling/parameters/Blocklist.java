/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.conciliation.factory.cluster.sidecars.pooling.parameters;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.util.List;
import java.util.Properties;

import com.google.common.collect.ImmutableList;

public class Blocklist {

  private static final List<String> BLOCKLIST;

  static {
    BLOCKLIST = readResource().entrySet().stream()
        .filter(e -> !e.getKey().toString().isEmpty())
        .map(e -> e.getKey().toString())
        .collect(ImmutableList.toImmutableList());
  }

  private Blocklist() {}

  private static Properties readResource() {
    Properties properties = new Properties();
    try (InputStream is = Blocklist.class.getResourceAsStream(
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

  public static List<String> getBlocklistParameters() {
    return BLOCKLIST;
  }

}
