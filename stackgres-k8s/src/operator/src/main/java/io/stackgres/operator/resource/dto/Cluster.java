/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.resource.dto;

import io.fabric8.kubernetes.client.CustomResource;
import io.quarkus.runtime.annotations.RegisterForReflection;

@RegisterForReflection
public class Cluster extends CustomResource {

  private static final long serialVersionUID = 1L;

  private ClusterSpec spec;

  private ClusterStatus status;

  public ClusterSpec getSpec() {
    return spec;
  }

  public void setSpec(ClusterSpec spec) {
    this.spec = spec;
  }

  public ClusterStatus getStatus() {
    return status;
  }

  public void setStatus(ClusterStatus status) {
    this.status = status;
  }
}
