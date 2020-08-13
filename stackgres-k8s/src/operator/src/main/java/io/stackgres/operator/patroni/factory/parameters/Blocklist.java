/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.patroni.factory.parameters;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.util.List;
import java.util.Properties;
import java.util.stream.Collectors;

import com.google.common.collect.ImmutableList;

public class Blocklist {

  private static final List<String> BLOCKLIST;

  static {
    BLOCKLIST = ImmutableList.<String>builder()
        .addAll(readResource().entrySet().stream()
            .filter(e -> !e.getKey().toString().isEmpty())
            .map(e -> e.getKey().toString())
            .collect(Collectors.toList()))
        .build();
  }

  private Blocklist() {}

  private static Properties readResource() {
    Properties properties = new Properties();
    try (InputStream is = Blocklist.class.getResourceAsStream(
        "/postgresql-blocklist.properties")) {
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
