/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.patroni.factory.parameters;

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
    try {
      properties.load(Blocklist.class.getResourceAsStream(
          "/postgresql-blocklist.properties"));
    } catch (Exception ex) {
      throw new RuntimeException(ex);
    }
    return properties;
  }

  public static List<String> getBlocklistParameters() {
    return BLOCKLIST;
  }

}
