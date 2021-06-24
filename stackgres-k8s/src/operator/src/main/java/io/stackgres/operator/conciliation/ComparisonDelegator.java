/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.conciliation;

import com.fasterxml.jackson.databind.node.ArrayNode;
import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.kubernetes.client.CustomResource;

public interface ComparisonDelegator<T extends CustomResource<?, ?>> {

  boolean isTheSameResource(HasMetadata required, HasMetadata deployed);

  boolean isResourceContentEqual(HasMetadata required, HasMetadata deployed);

  ArrayNode getJsonDiff(HasMetadata required, HasMetadata deployed);
}
