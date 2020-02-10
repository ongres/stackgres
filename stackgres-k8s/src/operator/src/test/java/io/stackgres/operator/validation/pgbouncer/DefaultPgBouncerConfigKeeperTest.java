/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.validation.pgbouncer;

import io.stackgres.operator.common.PgBouncerReview;
import io.stackgres.operator.initialization.DefaultCustomResourceFactory;
import io.stackgres.operator.sidecars.pgbouncer.customresources.StackGresPgbouncerConfig;
import io.stackgres.operator.utils.JsonUtil;
import io.stackgres.operator.validation.AbstractDefaultConfigKeeper;
import io.stackgres.operator.validation.DefaultKeeperTest;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class DefaultPgBouncerConfigKeeperTest extends DefaultKeeperTest<StackGresPgbouncerConfig, PgBouncerReview> {

  @Override
  protected AbstractDefaultConfigKeeper<StackGresPgbouncerConfig, PgBouncerReview> getValidatorInstance() {
    return new DefaultPgBouncerConfigKeeper();
  }

  @Override
  protected PgBouncerReview getCreationSample() {
    return JsonUtil.readFromJson("pgbouncer_allow_request/create.json",
        PgBouncerReview.class);
  }

  @Override
  protected PgBouncerReview getDeleteSample() {
    return JsonUtil.readFromJson("pgbouncer_allow_request/delete.json",
        PgBouncerReview.class);
  }

  @Override
  protected PgBouncerReview getUpdateSample() {
    return JsonUtil.readFromJson("pgbouncer_allow_request/update.json",
        PgBouncerReview.class);
  }

  @Override
  protected StackGresPgbouncerConfig getDefault() {
    return JsonUtil.readFromJson("pgbouncer_config/default.json",
        StackGresPgbouncerConfig.class);
  }
}