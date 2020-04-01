/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.mutation.pgbouncer;

import javax.enterprise.context.ApplicationScoped;

import io.stackgres.operator.common.PoolingReview;
import io.stackgres.operator.mutation.AbstractAnnotationMutator;
import io.stackgres.operator.sidecars.pooling.customresources.StackGresPoolingConfig;

@ApplicationScoped
public class PgBouncerAnnotationMutator
    extends AbstractAnnotationMutator<StackGresPoolingConfig, PoolingReview>
    implements PgBouncerMutator {
}
