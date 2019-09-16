/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.common.sgcluster;

import com.google.common.base.MoreObjects;

import io.fabric8.kubernetes.client.CustomResource;

public class StackGresCluster extends CustomResource {

  private static final long serialVersionUID = -5276087851826599719L;

  private StackGresClusterSpec spec;
  private StackGresClusterStatus status;

  public StackGresClusterSpec getSpec() {
    return spec;
  }

  public void setSpec(StackGresClusterSpec spec) {
    this.spec = spec;
  }

  public StackGresClusterStatus getStatus() {
    return status;
  }

  public void setStatus(StackGresClusterStatus status) {
    this.status = status;
  }

  @Override
  public String toString() {
    return MoreObjects.toStringHelper(this)
        .omitNullValues()
        .add("apiVersion", getApiVersion())
        .add("metadata", getMetadata())
        .add("spec", spec)
        .add("status", status)
        .toString();
  }

}
