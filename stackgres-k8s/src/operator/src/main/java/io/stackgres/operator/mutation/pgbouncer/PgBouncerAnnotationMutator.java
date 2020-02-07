/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.mutation.pgbouncer;

import javax.enterprise.context.ApplicationScoped;

import io.stackgres.operator.cluster.sidecars.pgbouncer.customresources.StackGresPgbouncerConfig;
import io.stackgres.operator.common.PgBouncerReview;
import io.stackgres.operator.mutation.AbstractAnnotationMutator;

@ApplicationScoped
public class PgBouncerAnnotationMutator
    extends AbstractAnnotationMutator<StackGresPgbouncerConfig, PgBouncerReview>
    implements PgBouncerMutator {
}
