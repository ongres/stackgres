/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.common.fixture;

import io.fabric8.kubernetes.api.model.batch.v1.Job;
import io.fabric8.kubernetes.api.model.batch.v1.JobBuilder;
import io.stackgres.testutil.fixture.Fixture;

public class JobFixture extends Fixture<Job> {

  public JobFixture loadDeployed() {
    fixture = readFromJson(JOB_DEPLOYED_JSON);
    return this;
  }

  public JobFixture loadRequired() {
    fixture = readFromJson(JOB_REQUIRED_JSON);
    return this;
  }

  public JobBuilder getBuilder() {
    return new JobBuilder(fixture);
  }

}
