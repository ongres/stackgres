/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.common.fixture;

import io.fabric8.kubernetes.api.model.Endpoints;
import io.fabric8.kubernetes.api.model.EndpointsBuilder;
import io.stackgres.testutil.fixture.Fixture;

public class EndpointsFixture extends Fixture<Endpoints> {

  public EndpointsFixture loadPatroni() {
    fixture = readFromJson(ENDPOINTS_PATRONI_JSON);
    return this;
  }

  public EndpointsFixture loadPatroniConfig() {
    fixture = readFromJson(ENDPOINTS_PATRONI_CONFIG_JSON);
    return this;
  }

  public EndpointsFixture loadPatroniConfigWithStandbyCluster() {
    fixture = readFromJson(ENDPOINTS_PATRONI_CONFIG_WITH_STANDBY_CLUSTER_JSON);
    return this;
  }

  public EndpointsFixture loadPatroniDeployed() {
    fixture = readFromJson(ENDPOINTS_PATRONI_DEPLOYED_JSON);
    return this;
  }

  public EndpointsFixture loadPatroniRequired() {
    fixture = readFromJson(ENDPOINTS_PATRONI_REQUIRED_JSON);
    return this;
  }

  public EndpointsFixture loadDeployed() {
    fixture = readFromJson(ENDPOINTS_DEPLOYED_JSON);
    return this;
  }

  public EndpointsFixture loadRequired() {
    fixture = readFromJson(ENDPOINTS_REQUIRED_JSON);
    return this;
  }

  public EndpointsBuilder getBuilder() {
    return new EndpointsBuilder(fixture);
  }

}
