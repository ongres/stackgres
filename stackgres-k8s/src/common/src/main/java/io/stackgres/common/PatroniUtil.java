/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.common;

import io.stackgres.common.resource.ResourceUtil;
import org.jetbrains.annotations.NotNull;

public final class PatroniUtil {

  private PatroniUtil() {
    throw new AssertionError("Utility class");
  }

  public static final String READ_WRITE_SERVICE = "-primary";
  public static final String READ_ONLY_SERVICE = "-replicas";
  public static final String FAILOVER_SERVICE = "-failover";
  public static final String CONFIG_SERVICE = "-config";
  public static final int POSTGRES_SERVICE_PORT = 5432;
  public static final int REPLICATION_SERVICE_PORT = 5433;

  public static String name(@NotNull String clusterName) {
    return ResourceUtil.resourceName(clusterName);
  }

  public static String readWriteName(@NotNull String clusterName) {
    return ResourceUtil.resourceName(clusterName + READ_WRITE_SERVICE);
  }

  public static String readOnlyName(@NotNull String clusterName) {
    return ResourceUtil.resourceName(clusterName + READ_ONLY_SERVICE);
  }

}
