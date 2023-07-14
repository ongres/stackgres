/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.mutation.pgbouncer;

import io.stackgres.common.crd.sgpooling.StackGresPoolingConfig;
import io.stackgres.operator.common.PoolingReview;
import io.stackgres.operator.mutation.AbstractAnnotationMutator;
import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class PgBouncerAnnotationMutator
    extends AbstractAnnotationMutator<StackGresPoolingConfig, PoolingReview>
    implements PgBouncerMutator {
}
