/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.mutation.restore;

import javax.enterprise.context.ApplicationScoped;

import io.stackgres.operator.common.RestoreConfigReview;
import io.stackgres.operator.customresource.sgrestoreconfig.StackgresRestoreConfig;
import io.stackgres.operator.mutation.AbstractAnnotationMutator;

@ApplicationScoped
public class RestoreAnnotationsMutator extends
    AbstractAnnotationMutator<StackgresRestoreConfig, RestoreConfigReview>
    implements RestoreMutator {

}
