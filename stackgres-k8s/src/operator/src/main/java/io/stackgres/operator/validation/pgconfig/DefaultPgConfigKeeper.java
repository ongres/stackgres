/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.validation.pgconfig;

import javax.enterprise.context.ApplicationScoped;

import io.stackgres.operator.common.PgConfigReview;
import io.stackgres.operator.customresource.sgpgconfig.StackGresPostgresConfig;
import io.stackgres.operator.validation.AbstractDefaultConfigKeeper;

@ApplicationScoped
public class DefaultPgConfigKeeper
    extends AbstractDefaultConfigKeeper<StackGresPostgresConfig, PgConfigReview>
    implements PgConfigValidator {

}
