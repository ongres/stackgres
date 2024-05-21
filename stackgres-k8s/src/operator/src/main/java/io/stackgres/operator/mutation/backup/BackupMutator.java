/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.mutation.backup;

import io.stackgres.common.crd.sgbackup.StackGresBackup;
import io.stackgres.operator.common.StackGresBackupReview;
import io.stackgres.operatorframework.admissionwebhook.mutating.Mutator;

public interface BackupMutator extends Mutator<StackGresBackup, StackGresBackupReview> {

}
