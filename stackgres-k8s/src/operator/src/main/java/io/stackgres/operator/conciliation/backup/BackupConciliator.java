/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.conciliation.backup;

import io.stackgres.common.crd.sgbackup.StackGresBackup;
import io.stackgres.operator.conciliation.Conciliator;
import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class BackupConciliator extends Conciliator<StackGresBackup> {

}
