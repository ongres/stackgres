/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.conciliation;

import java.util.Optional;

import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.kubernetes.client.VersionInfo;
import io.stackgres.common.StackGresVersion;

public interface GenerationContext<T extends HasMetadata> {

  T getSource();

  StackGresVersion getVersion();

  /**
   * Whether the images of the generated resources are retrieved from the StackGres images
   * registry (see {@code SGCluster.spec.configurations.registry.enabled}).
   */
  default boolean isRegistryEnabled() {
    return false;
  }

  Optional<VersionInfo> getKubernetesVersion();

}
