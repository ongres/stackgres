/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.validation.restoreconfig;

import javax.enterprise.context.ApplicationScoped;

import io.stackgres.operator.common.RestoreConfigReview;
import io.stackgres.operator.customresource.sgrestoreconfig.StackgresRestoreConfig;
import io.stackgres.operator.validation.AbstractDefaultConfigKeeper;

@ApplicationScoped
public class DefaultRestoreConfigKeeper extends AbstractDefaultConfigKeeper<StackgresRestoreConfig,
    RestoreConfigReview> implements RestoreConfigValidator {

}
