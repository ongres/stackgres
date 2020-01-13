/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.validation.backupconfig;

import javax.enterprise.context.ApplicationScoped;

import io.stackgres.operator.common.BackupConfigReview;
import io.stackgres.operator.customresource.sgbackupconfig.StackGresBackupConfig;
import io.stackgres.operator.validation.AbstractDefaultConfigKeeper;

@ApplicationScoped
public class DefaultBackupConfigKeeper
    extends AbstractDefaultConfigKeeper<StackGresBackupConfig, BackupConfigReview>
    implements BackupConfigValidator {

}
