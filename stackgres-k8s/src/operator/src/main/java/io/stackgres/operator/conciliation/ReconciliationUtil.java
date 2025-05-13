/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.conciliation;

import java.util.Map;

import io.stackgres.common.StackGresContext;

public class ReconciliationUtil {

  public static boolean isResourceReconciliationNotPaused(DeployedResource deployedResource) {
    return !deployedResource.hasDeployedLabels(
        Map.of(StackGresContext.RECONCILIATION_PAUSE_KEY, StackGresContext.RIGHT_VALUE));
  }
}
