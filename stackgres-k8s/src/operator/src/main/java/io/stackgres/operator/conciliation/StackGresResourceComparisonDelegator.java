/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.conciliation;

import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.kubernetes.client.CustomResource;
import io.stackgres.operator.conciliation.comparator.ResourceComparator;

public abstract class StackGresResourceComparisonDelegator<T extends CustomResource>
    implements ComparisonDelegator<T> {

  protected abstract ResourceComparator getComparator(HasMetadata r1);

  @Override
  public boolean isTheSameResource(HasMetadata r1, HasMetadata r2) {
    ResourceComparator comparator = getComparator(r1);

    return comparator
        .isTheSameResource(r1, r2);
  }

  @Override
  public boolean isResourceContentEqual(HasMetadata r1, HasMetadata r2) {
    ResourceComparator comparator = getComparator(r1);

    return comparator
        .isResourceContentEqual(r1, r2);
  }
}
