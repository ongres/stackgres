/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.validation.pgconfig;

import io.stackgres.operator.common.PgConfigReview;
import io.stackgres.operator.common.fixture.AdmissionReviewFixtures;

abstract class AbstractPgConfigReview {

  protected PgConfigReview validConfigReview() {
    return AdmissionReviewFixtures.postgresConfig().loadCreate().get();
  }

  protected PgConfigReview validConfigUpdate() {
    return AdmissionReviewFixtures.postgresConfig().loadUpdate().get();
  }

  protected PgConfigReview validConfigDelete() {
    return AdmissionReviewFixtures.postgresConfig().loadDelete().get();
  }

}
