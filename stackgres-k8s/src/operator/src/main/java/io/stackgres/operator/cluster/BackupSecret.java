/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.cluster;

import java.util.List;

import com.google.common.collect.ImmutableList;

import io.fabric8.kubernetes.api.model.HasMetadata;
import io.stackgres.operator.common.StackGresClusterContext;
import io.stackgres.operator.controller.ResourceGeneratorContext;

public class BackupSecret {

  /**
   * Create required Secrets based on the StackGresBackupConfig definition.
   */
  public static List<HasMetadata> create(
      ResourceGeneratorContext<StackGresClusterContext> context) {
    StackGresClusterContext clusterContext = context.getContext();
    return ImmutableList.of();
  }

}
