/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.mutation.backup;

import javax.enterprise.context.ApplicationScoped;

import io.stackgres.operator.common.BackupReview;
import io.stackgres.operator.customresource.sgbackup.StackGresBackup;
import io.stackgres.operator.mutation.AbstractAnnotationMutator;

@ApplicationScoped
public class BackupAnnotationMutator
    extends AbstractAnnotationMutator<StackGresBackup, BackupReview>
    implements BackupMutator {
}
