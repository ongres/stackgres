/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.conciliation.comparator;

import io.fabric8.kubernetes.api.model.HasMetadata;

public interface ResourceComparator {

  default boolean isTheSameResource(HasMetadata required, HasMetadata deployed) {
    return required.getKind().equals(deployed.getKind())
        && required.getMetadata().getNamespace().equals(deployed.getMetadata().getNamespace())
        && required.getMetadata().getName().equals(deployed.getMetadata().getName());
  }

  boolean isResourceContentEqual(HasMetadata required, HasMetadata deployed);

}
