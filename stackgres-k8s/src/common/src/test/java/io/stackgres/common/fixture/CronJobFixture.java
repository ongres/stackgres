/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.common.fixture;

import io.fabric8.kubernetes.api.model.batch.v1beta1.CronJob;
import io.fabric8.kubernetes.api.model.batch.v1beta1.CronJobBuilder;
import io.stackgres.testutil.fixture.Fixture;

public class CronJobFixture extends Fixture<CronJob> {

  public CronJobFixture loadDeployed() {
    fixture = readFromJson(CRONJOB_DEPLOYED_JSON);
    return this;
  }

  public CronJobFixture loadRequired() {
    fixture = readFromJson(CRONJOB_REQUIRED_JSON);
    return this;
  }

  public CronJobBuilder getBuilder() {
    return new CronJobBuilder(fixture);
  }

}
