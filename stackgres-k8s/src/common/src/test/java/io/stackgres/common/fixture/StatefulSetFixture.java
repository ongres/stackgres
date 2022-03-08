/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.common.fixture;

import io.fabric8.kubernetes.api.model.apps.StatefulSet;
import io.stackgres.testutil.fixture.Fixture;

public class StatefulSetFixture extends Fixture<StatefulSet> {

  public StatefulSetFixture loadDeployed() {
    fixture = readFromJson(STATEFULSET_DEPLOYED_JSON);
    return this;
  }

  public StatefulSetFixture loadRequiredDistributedLogs() {
    fixture = readFromJson(STATEFULSET_REQUIRED_DISTRIBUTEDLOGS_JSON);
    return this;
  }

  public StatefulSetFixture loadDeployedDistributedLogs() {
    fixture = readFromJson(STATEFULSET_DEPLOYED_DISTRIBUTEDLOGS_JSON);
    return this;
  }

  public StatefulSetFixture loadRequired() {
    fixture = readFromJson(STATEFULSET_REQUIRED_JSON);
    return this;
  }

  public StatefulSetFixture load0_9_5() {
    fixture = readFromJson(STATEFULSET_0_9_5_JSON);
    return this;
  }

  public StatefulSetFixture loadWithoutManagedFields() {
    fixture = readFromJson(STATEFULSET_STATEFULSET_WITHOUT_MANAGED_FIELDS_JSON);
    return this;
  }

}
