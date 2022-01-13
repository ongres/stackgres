/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.conciliation.comparator;

import com.fasterxml.jackson.databind.node.ArrayNode;
import io.fabric8.kubernetes.api.model.HasMetadata;

public interface ResourceComparator {

  static String escapePatchPath(String patchPath) {
    return patchPath.replace("~", "~0").replace("/", "~1");
  }

  default boolean isTheSameResource(HasMetadata required, HasMetadata deployed) {
    return required.getKind().equals(deployed.getKind())
        && required.getMetadata().getNamespace().equals(deployed.getMetadata().getNamespace())
        && required.getMetadata().getName().equals(deployed.getMetadata().getName());
  }

  boolean isResourceContentEqual(HasMetadata required, HasMetadata deployed);

  ArrayNode getJsonDiff(HasMetadata required, HasMetadata deployed);

}
