/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.validation.pgbouncer;

import javax.enterprise.context.ApplicationScoped;

import io.stackgres.operator.common.ErrorType;
import io.stackgres.operator.common.PgBouncerReview;
import io.stackgres.operator.sidecars.pgbouncer.customresources.StackGresPgbouncerConfig;
import io.stackgres.operator.validation.AbstractDefaultConfigKeeper;
import io.stackgres.operator.validation.ValidationType;

@ApplicationScoped
@ValidationType(ErrorType.DEFAULT_CONFIGURATION)
public class DefaultPgBouncerConfigKeeper
    extends AbstractDefaultConfigKeeper<StackGresPgbouncerConfig, PgBouncerReview>
    implements PgBouncerValidator {

}
