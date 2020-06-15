/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.common;

import io.stackgres.common.resource.ResourceUtil;

public class PatroniUtil {
  public static final String READ_WRITE_SERVICE = "-primary";
  public static final String READ_ONLY_SERVICE = "-replicas";
  public static final String FAILOVER_SERVICE = "-failover";
  public static final String CONFIG_SERVICE = "-config";

  public static String readWriteName(String clusterName) {
    return ResourceUtil.resourceName(clusterName + READ_WRITE_SERVICE);
  }
}
