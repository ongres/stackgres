/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.conciliation;

import com.fasterxml.jackson.databind.node.ArrayNode;
import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.kubernetes.client.CustomResource;
import io.stackgres.operator.conciliation.comparator.ResourceComparator;

public abstract class CustomResourceComparisonDelegator<T extends CustomResource<?, ?>>
    implements ComparisonDelegator<T> {

  protected abstract ResourceComparator getComparator(HasMetadata r1);

  @Override
  public boolean isTheSameResource(HasMetadata required, HasMetadata deployed) {
    ResourceComparator comparator = getComparator(required);

    return comparator
        .isTheSameResource(required, deployed);
  }

  @Override
  public boolean isResourceContentEqual(HasMetadata required, HasMetadata deployed) {
    ResourceComparator comparator = getComparator(required);

    return comparator
        .isResourceContentEqual(required, deployed);
  }

  @Override
  public ArrayNode getJsonDiff(HasMetadata required, HasMetadata deployed) {
    ResourceComparator comparator = getComparator(required);

    return comparator
        .getJsonDiff(required, deployed);
  }
}
