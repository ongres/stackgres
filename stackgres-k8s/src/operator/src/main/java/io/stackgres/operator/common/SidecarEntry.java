/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.common;

import java.util.Optional;

public class SidecarEntry<T> {

  private final StackGresClusterSidecarResourceFactory<T> sidecar;
  private final Optional<T> config;

  /**
   * Create a sidecar entry that include sidecar transformer and config.
   */
  public SidecarEntry(StackGresClusterSidecarResourceFactory<T> sidecar, Optional<T> config) {
    super();
    this.sidecar = sidecar;
    this.config = config;
  }

  public StackGresClusterSidecarResourceFactory<T> getSidecar() {
    return sidecar;
  }

  public Optional<T> getConfig() {
    return config;
  }

}
