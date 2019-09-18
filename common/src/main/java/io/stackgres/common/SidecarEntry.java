/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.common;

import java.util.Optional;

import io.fabric8.kubernetes.client.CustomResource;

public class SidecarEntry<T extends CustomResource> {

  private final StackGresSidecarTransformer<T> sidecar;
  private final Optional<T> config;

  /**
   * Create a sidecar entry that include sidecar transformer and config.
   */
  public SidecarEntry(StackGresSidecarTransformer<T> sidecar, Optional<T> config) {
    super();
    this.sidecar = sidecar;
    this.config = config;
  }

  public StackGresSidecarTransformer<T> getSidecar() {
    return sidecar;
  }

  public Optional<T> getConfig() {
    return config;
  }

}
