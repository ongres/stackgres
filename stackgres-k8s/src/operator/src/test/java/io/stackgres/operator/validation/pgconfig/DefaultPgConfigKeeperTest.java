/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.validation.pgconfig;

import io.stackgres.common.crd.sgpgconfig.StackGresPostgresConfig;
import io.stackgres.operator.common.PgConfigReview;
import io.stackgres.operator.validation.AbstractDefaultConfigKeeper;
import io.stackgres.operator.validation.DefaultKeeperTest;
import io.stackgres.testutil.JsonUtil;
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
    return JsonUtil.readFromJson("pgconfig_allow_request/valid_pgconfig.json",
        PgConfigReview.class);
  }

  @Override
  protected PgConfigReview getDeleteSample() {
    return JsonUtil.readFromJson("pgconfig_allow_request/pgconfig_delete.json",
        PgConfigReview.class);
  }

  @Override
  protected PgConfigReview getUpdateSample() {
    return JsonUtil.readFromJson("pgconfig_allow_request/valid_pgconfig_update.json",
        PgConfigReview.class);
  }

}
