/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.validation.backupconfig;

import io.stackgres.common.crd.sgbackupconfig.StackGresBackupConfig;
import io.stackgres.operator.validation.AbstractDefaultCustomResourceHolder;
import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class BackupConfigDefaultCustomResourceHolder
    extends AbstractDefaultCustomResourceHolder<StackGresBackupConfig> {
}
