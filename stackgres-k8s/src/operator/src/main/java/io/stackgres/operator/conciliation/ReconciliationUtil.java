/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.conciliation;

import java.util.Optional;

import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.kubernetes.api.model.ObjectMeta;
import io.stackgres.common.StackGresContext;

public class ReconciliationUtil {

  public static boolean isResourceReconciliationNotPaused(HasMetadata resource) {
    return Optional
        .of(resource)
        .map(HasMetadata::getMetadata)
        .map(ObjectMeta::getAnnotations)
        .map(annotations -> annotations
            .get(StackGresContext.RECONCILIATION_PAUSE_KEY))
        .map(Boolean::parseBoolean)
        .filter(pause -> pause)
        .isEmpty();
  }
}
