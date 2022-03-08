/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.common.fixture;

import io.fabric8.kubernetes.api.model.Status;
import io.stackgres.testutil.fixture.Fixture;

public class KubeStatusFixture extends Fixture<Status> {

  public KubeStatusFixture load1_16_4() {
    fixture = readFromJson(KUBE_STATUS_STATUS_1_16_4_JSON);
    return this;
  }

  public KubeStatusFixture load1_13_12() {
    fixture = readFromJson(KUBE_STATUS_STATUS_1_13_12_JSON);
    return this;
  }

  public KubeStatusFixture loadInvalidClusterName() {
    fixture = readFromJson(KUBE_STATUS_INVALID_CLUSTER_NAME_JSON);
    return this;
  }

  public KubeStatusFixture loadInvalidDnsName() {
    fixture = readFromJson(KUBE_STATUS_INVALID_DNS_NAME_JSON);
    return this;
  }

  public KubeStatusFixture loadAlreadyExists() {
    fixture = readFromJson(KUBE_STATUS_ALREADY_EXISTS_JSON);
    return this;
  }

}
