/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.conciliation.comparator;

import io.fabric8.kubernetes.api.model.HasMetadata;

public interface ResourceComparator {

  default boolean isTheSameResource(HasMetadata r1, HasMetadata r2) {
    return r1.getKind().equals(r2.getKind())
        && r1.getMetadata().getNamespace().equals(r2.getMetadata().getNamespace())
        && r1.getMetadata().getName().equals(r2.getMetadata().getName());
  }

  boolean isResourceContentEqual(HasMetadata r1, HasMetadata r2);

}
