/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.common;

import java.util.Optional;

import io.fabric8.kubernetes.client.CustomResource;

public class SidecarEntry<T extends CustomResource, C> {

  private final StackGresSidecarTransformer<T, C> sidecar;
  private final Optional<T> config;

  /**
   * Create a sidecar entry that include sidecar transformer and config.
   */
  public SidecarEntry(StackGresSidecarTransformer<T, C> sidecar, Optional<T> config) {
    super();
    this.sidecar = sidecar;
    this.config = config;
  }

  public StackGresSidecarTransformer<T, C> getSidecar() {
    return sidecar;
  }

  public Optional<T> getConfig() {
    return config;
  }

}
