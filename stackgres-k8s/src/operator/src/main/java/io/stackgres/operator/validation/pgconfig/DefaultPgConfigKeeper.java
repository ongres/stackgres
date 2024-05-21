/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.validation.pgconfig;

import io.stackgres.common.ErrorType;
import io.stackgres.common.crd.sgpgconfig.StackGresPostgresConfig;
import io.stackgres.operator.common.StackGresPostgresConfigReview;
import io.stackgres.operator.validation.AbstractDefaultConfigKeeper;
import io.stackgres.operator.validation.ValidationType;
import jakarta.inject.Singleton;

@Singleton
@ValidationType(ErrorType.DEFAULT_CONFIGURATION)
public class DefaultPgConfigKeeper
    extends AbstractDefaultConfigKeeper<StackGresPostgresConfig, StackGresPostgresConfigReview>
    implements PgConfigValidator {

}
