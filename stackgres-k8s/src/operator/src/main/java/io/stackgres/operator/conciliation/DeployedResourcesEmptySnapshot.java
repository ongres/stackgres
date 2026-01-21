/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.conciliation;

import java.util.List;
import java.util.stream.Stream;

import io.fabric8.kubernetes.api.model.HasMetadata;

public class DeployedResourcesEmptySnapshot implements DeployedResourcesSnapshot {

  @Override
  public Stream<HasMetadata> streamDeployed() {
    return Stream.of();
  }

  @Override
  public List<HasMetadata> ownedDeployedResources() {
    return List.of();
  }

  @Override
  public boolean isChanged(HasMetadata requiredResource, DeployedResource deployedResourceValue) {
    return false;
  }

  @Override
  public boolean isDeployed(HasMetadata requiredResource) {
    return false;
  }

  @Override
  public DeployedResource get(HasMetadata requiredResource) {
    return null;
  }

  @Override
  public List<HasMetadata> deployedResources() {
    return List.of();
  }
}
