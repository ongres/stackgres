/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.conciliation;

import java.util.List;
import java.util.stream.Stream;

import io.fabric8.kubernetes.api.model.HasMetadata;

public interface DeployedResourcesSnapshot {

  List<HasMetadata> ownedDeployedResources();

  List<HasMetadata> deployedResources();

  DeployedResource get(HasMetadata requiredResource);

  Stream<HasMetadata> streamDeployed();

  boolean isDeployed(HasMetadata requiredResource);

  boolean isChanged(HasMetadata requiredResource, DeployedResource deployedResourceValue);

  static DeployedResourcesSnapshot emptySnapshot() {
    return new DeployedResourcesEmptySnapshot();
  }

}
