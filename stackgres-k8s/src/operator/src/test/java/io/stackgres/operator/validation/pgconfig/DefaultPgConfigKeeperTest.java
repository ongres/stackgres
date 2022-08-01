/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.validation.pgconfig;

import io.stackgres.common.crd.sgpgconfig.StackGresPostgresConfig;
import io.stackgres.operator.common.PgConfigReview;
import io.stackgres.operator.common.fixture.AdmissionReviewFixtures;
import io.stackgres.operator.validation.AbstractDefaultConfigKeeper;
import io.stackgres.operator.validation.DefaultKeeperTest;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class DefaultPgConfigKeeperTest
    extends DefaultKeeperTest<StackGresPostgresConfig, PgConfigReview> {

  @Override
  protected AbstractDefaultConfigKeeper<StackGresPostgresConfig, PgConfigReview>
      getValidatorInstance() {
    return new DefaultPgConfigKeeper();
  }

  @Override
  protected PgConfigReview getCreationSample() {
    return AdmissionReviewFixtures.postgresConfig().loadCreate().get();
  }

  @Override
  protected PgConfigReview getDeleteSample() {
    return AdmissionReviewFixtures.postgresConfig().loadDelete().get();
  }

  @Override
  protected PgConfigReview getUpdateSample() {
    return AdmissionReviewFixtures.postgresConfig().loadUpdate().get();
  }

}
