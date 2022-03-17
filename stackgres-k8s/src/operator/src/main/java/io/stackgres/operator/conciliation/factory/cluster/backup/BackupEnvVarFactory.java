/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.conciliation.factory.cluster.backup;

import java.util.Map;

import io.stackgres.common.crd.sgbackupconfig.StackGresBackupConfig;
import io.stackgres.common.crd.sgbackupconfig.StackGresBackupConfigSpec;
import io.stackgres.common.crd.storages.BackupStorage;

public interface BackupEnvVarFactory {

  Map<String, String> getSecretEnvVar(String namespace, BackupStorage storage);

  Map<String, String> getSecretEnvVar(StackGresBackupConfig backupConfig);

  Map<String, String> getSecretEnvVar(String namespace, StackGresBackupConfigSpec backupConfig);
}
