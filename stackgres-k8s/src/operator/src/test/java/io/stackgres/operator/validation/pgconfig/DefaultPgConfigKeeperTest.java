/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.validation.pgconfig;

import io.stackgres.common.crd.sgpgconfig.StackGresPostgresConfig;
import io.stackgres.operator.common.StackGresPostgresConfigReview;
import io.stackgres.operator.common.fixture.AdmissionReviewFixtures;
import io.stackgres.operator.validation.AbstractDefaultConfigKeeper;
import io.stackgres.operator.validation.DefaultKeeperTest;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class DefaultPgConfigKeeperTest
    extends DefaultKeeperTest<StackGresPostgresConfig, StackGresPostgresConfigReview> {

  @Override
  protected AbstractDefaultConfigKeeper<StackGresPostgresConfig, StackGresPostgresConfigReview>
      getValidatorInstance() {
    return new DefaultPgConfigKeeper();
  }

  @Override
  protected StackGresPostgresConfigReview getCreationSample() {
    return AdmissionReviewFixtures.postgresConfig().loadCreate().get();
  }

  @Override
  protected StackGresPostgresConfigReview getDeleteSample() {
    return AdmissionReviewFixtures.postgresConfig().loadDelete().get();
  }

  @Override
  protected StackGresPostgresConfigReview getUpdateSample() {
    return AdmissionReviewFixtures.postgresConfig().loadUpdate().get();
  }

}
