/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.common.fixture.cluster;

import io.stackgres.common.crd.sgcluster.StackGresClusterList;
import io.stackgres.testutil.fixture.Fixture;

public class ClusterListFixture extends Fixture<StackGresClusterList> {

  public ClusterListFixture loadDefault() {
    fixture = readFromJson(STACKGRES_CLUSTER_LIST_JSON);
    return this;
  }

}
