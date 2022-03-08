/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.common.fixture;

import io.fabric8.kubernetes.api.model.Endpoints;
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

  public EndpointsFixture loadDeployed() {
    fixture = readFromJson(ENDPOINTS_DEPLOYED_JSON);
    return this;
  }

  public EndpointsFixture loadRequired() {
    fixture = readFromJson(ENDPOINTS_REQUIRED_JSON);
    return this;
  }

}
