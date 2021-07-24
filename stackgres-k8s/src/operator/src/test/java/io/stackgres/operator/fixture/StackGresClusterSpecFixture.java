/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.fixture;

import io.stackgres.common.crd.sgcluster.StackGresClusterPod;
import io.stackgres.common.crd.sgcluster.StackGresClusterSpec;

public class StackGresClusterSpecFixture {

  private StackGresClusterPod pod;

  public StackGresClusterSpec build() {
    StackGresClusterSpec spec = new StackGresClusterSpec();
    spec.setPod(this.pod);
    return spec;
  }

  public StackGresClusterSpecFixture withPod(StackGresClusterPod pod) {
    this.pod = pod;
    return this;
  }

}
