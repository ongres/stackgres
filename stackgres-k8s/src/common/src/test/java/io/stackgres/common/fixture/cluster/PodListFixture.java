/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.common.fixture.cluster;

import io.fabric8.kubernetes.api.model.PodList;
import io.fabric8.kubernetes.api.model.PodListBuilder;
import io.stackgres.testutil.fixture.Fixture;

public class PodListFixture extends Fixture<PodList> {

  public PodListFixture loadDefault() {
    fixture = readFromJson(STACKGRES_CLUSTER_PODS_JSON);
    return this;
  }

  public PodListBuilder getBuilder() {
    return new PodListBuilder(fixture);
  }

}
