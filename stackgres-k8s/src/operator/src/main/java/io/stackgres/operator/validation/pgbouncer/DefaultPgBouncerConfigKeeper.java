/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.validation.pgbouncer;

import javax.enterprise.context.ApplicationScoped;

import io.stackgres.operator.cluster.sidecars.pgbouncer.customresources.StackGresPgbouncerConfig;
import io.stackgres.operator.common.PgBouncerReview;
import io.stackgres.operator.validation.AbstractDefaultConfigKeeper;

@ApplicationScoped
public class DefaultPgBouncerConfigKeeper
    extends AbstractDefaultConfigKeeper<StackGresPgbouncerConfig, PgBouncerReview>
    implements PgBouncerValidator {

}
