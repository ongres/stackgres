/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.mutation.backupconfig;

import javax.enterprise.context.ApplicationScoped;

import io.stackgres.common.crd.sgbackupconfig.StackGresBackupConfig;
import io.stackgres.operator.common.BackupConfigReview;
import io.stackgres.operator.mutation.AbstractAnnotationMutator;

@ApplicationScoped
public class BackupAnnotationMutator
    extends AbstractAnnotationMutator<StackGresBackupConfig, BackupConfigReview>
    implements BackupConfigMutator {
}
