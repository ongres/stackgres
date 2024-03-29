/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.conciliation.factory.cluster.sidecars.pooling.parameters;

import java.util.Set;

import com.google.common.collect.ImmutableSet;
import io.stackgres.common.StackGresUtil;

public class PgBouncerBlocklist {

  private static final String FILE_PATH = "/pgbouncer-blocklist.properties";

  private static final Set<String> BLOCKED_LIST = StackGresUtil.loadProperties(FILE_PATH)
      .keySet().stream()
      .map(Object::toString)
      .filter(e -> !e.isBlank())
      .collect(ImmutableSet.toImmutableSet());

  private PgBouncerBlocklist() {}

  public static Set<String> getBlocklistParameters() {
    return BLOCKED_LIST;
  }

}
