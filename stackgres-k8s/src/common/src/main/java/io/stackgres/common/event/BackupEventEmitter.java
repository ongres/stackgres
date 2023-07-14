/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.common.event;

import io.stackgres.common.crd.sgbackup.StackGresBackup;
import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class BackupEventEmitter extends AbstractEventEmitter<StackGresBackup> {

}
