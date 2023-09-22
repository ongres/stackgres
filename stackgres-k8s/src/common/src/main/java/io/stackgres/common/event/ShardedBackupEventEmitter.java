/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.common.event;

import javax.enterprise.context.ApplicationScoped;

import io.stackgres.common.crd.sgshardedbackup.StackGresShardedBackup;

@ApplicationScoped
public class ShardedBackupEventEmitter extends AbstractEventEmitter<StackGresShardedBackup> {

}
