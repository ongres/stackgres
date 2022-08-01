/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.common.fixture;

import io.fabric8.kubernetes.api.model.Service;
import io.fabric8.kubernetes.api.model.ServiceBuilder;
import io.stackgres.testutil.fixture.Fixture;

public class ServiceFixture extends Fixture<Service> {

  public ServiceFixture loadDeployed() {
    fixture = readFromJson(SERVICE_DEPLOYED_JSON);
    return this;
  }

  public ServiceFixture loadRequired() {
    fixture = readFromJson(SERVICE_REQUIRED_JSON);
    return this;
  }

  public ServiceFixture loadPatroniRest() {
    fixture = readFromJson(SERVICE_PATRONI_REST_JSON);
    return this;
  }

  public ServiceBuilder getBuilder() {
    return new ServiceBuilder(fixture);
  }

}
