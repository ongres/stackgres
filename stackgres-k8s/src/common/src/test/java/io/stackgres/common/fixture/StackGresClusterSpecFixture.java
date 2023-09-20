/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.common.fixture;

import io.stackgres.common.crd.sgcluster.StackGresClusterPods;
import io.stackgres.common.crd.sgcluster.StackGresClusterSpec;
import io.stackgres.common.crd.sgcluster.StackGresClusterSpecBuilder;

public class StackGresClusterSpecFixture {

  private StackGresClusterPods pod;

  public StackGresClusterSpec build() {
    StackGresClusterSpec spec = new StackGresClusterSpec();
    spec.setPods(this.pod);
    return spec;
  }

  public StackGresClusterSpecFixture withPod(StackGresClusterPods pod) {
    this.pod = pod;
    return this;
  }

  public StackGresClusterSpecBuilder getBuilder() {
    return new StackGresClusterSpecBuilder(build());
  }

}
