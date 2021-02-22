/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.conciliation;

import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.kubernetes.client.CustomResource;

public interface ComparisonDelegator<T extends CustomResource<?, ?>> {

  boolean isTheSameResource(HasMetadata r1, HasMetadata r2);

  boolean isResourceContentEqual(HasMetadata r1, HasMetadata r2);
}
