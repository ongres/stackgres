/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.validation.backupconfig;

import javax.inject.Singleton;

import io.stackgres.common.ErrorType;
import io.stackgres.common.crd.sgbackupconfig.StackGresBackupConfig;
import io.stackgres.operator.common.BackupConfigReview;
import io.stackgres.operator.validation.AbstractDefaultConfigKeeper;
import io.stackgres.operator.validation.ValidationType;

@Singleton
@ValidationType(ErrorType.DEFAULT_CONFIGURATION)
public class DefaultBackupConfigKeeper
    extends AbstractDefaultConfigKeeper<StackGresBackupConfig, BackupConfigReview>
    implements BackupConfigValidator {

}
