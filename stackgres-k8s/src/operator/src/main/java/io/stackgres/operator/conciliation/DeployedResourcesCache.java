/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.conciliation;

import java.util.List;
import java.util.Map;

import io.fabric8.kubernetes.api.model.HasMetadata;

public interface DeployedResourcesCache {

  void put(
      HasMetadata generator,
      HasMetadata requiredResource,
      HasMetadata deployedResource);

  void remove(
      HasMetadata generator,
      HasMetadata deletedResource);

  void removeAll(
      HasMetadata generator);

  DeployedResource get(
      HasMetadata generator,
      HasMetadata requiredResource);

  DeployedResourcesSnapshot createDeployedResourcesSnapshot(
      HasMetadata generator,
      List<HasMetadata> ownedDeployedResources,
      List<HasMetadata> deployedResources);

  void removeWithLabelsNotIn(
      HasMetadata generator,
      Map<String, String> genericLabels,
      List<HasMetadata> deployedResources);

}
