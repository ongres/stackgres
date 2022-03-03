/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.validation.backupconfig;

import javax.enterprise.context.ApplicationScoped;

import io.stackgres.common.crd.sgbackupconfig.StackGresBackupConfig;
import io.stackgres.operator.validation.AbstractDefaultCustomResourceHolder;

@ApplicationScoped
public class BackupConfigDefaultCustomResourceHolder
    extends AbstractDefaultCustomResourceHolder<StackGresBackupConfig> {
}
