/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.conciliation.factory.cluster.patroni.parameters;

import java.util.Set;

import com.google.common.collect.ImmutableSet;
import io.stackgres.common.StackGresUtil;

public class PostgresBlocklist {

  private static final String FILE_PATH = "/postgresql-blocklist.properties";

  private static final Set<String> BLOCKED_LIST = StackGresUtil.loadProperties(FILE_PATH)
      .keySet().stream()
      .map(Object::toString)
      .filter(e -> !e.isBlank())
      .collect(ImmutableSet.toImmutableSet());

  private PostgresBlocklist() {}

  public static Set<String> getBlocklistParameters() {
    return BLOCKED_LIST;
  }

}
