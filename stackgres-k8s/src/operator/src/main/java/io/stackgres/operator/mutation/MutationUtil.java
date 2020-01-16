/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.mutation;

public class MutationUtil {

  public static final String MUTATION_PATH = "/stackgres/mutation";
  public static final String CLUSTER_MUTATION_PATH = MUTATION_PATH + "/sgcluster";
  public static final String PGCONFIG_MUTATION_PATH = MUTATION_PATH + "/sgpgconfig";
  public static final String CONNPOOLCONFIG_MUTATION_PATH =
      MUTATION_PATH + "/sgconnectionpoolingconfig";
  public static final String BACKUPCONFIG_MUTATION_PATH = MUTATION_PATH + "/sgbackupconfig";
  public static final String PROFILE_MUTATION_PATH = MUTATION_PATH + "/sgprofile";
  public static final String RESTORECONFIG_MUTATION_PATH = MUTATION_PATH + "/sgrestoreconfig";

  private MutationUtil() {
  }

}
