/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.validation.pgconfig;

import io.stackgres.operator.common.StackGresPostgresConfigReview;
import io.stackgres.operator.common.fixture.AdmissionReviewFixtures;

abstract class AbstractPgConfigReview {

  protected StackGresPostgresConfigReview validConfigReview() {
    return AdmissionReviewFixtures.postgresConfig().loadCreate().get();
  }

  protected StackGresPostgresConfigReview validConfigUpdate() {
    return AdmissionReviewFixtures.postgresConfig().loadUpdate().get();
  }

  protected StackGresPostgresConfigReview validConfigDelete() {
    return AdmissionReviewFixtures.postgresConfig().loadDelete().get();
  }

}
