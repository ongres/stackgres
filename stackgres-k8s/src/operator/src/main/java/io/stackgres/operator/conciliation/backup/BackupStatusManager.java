/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.conciliation.backup;

import java.util.Optional;

import javax.enterprise.context.ApplicationScoped;

import io.stackgres.common.crd.sgbackup.BackupPhase;
import io.stackgres.common.crd.sgbackup.StackGresBackup;
import io.stackgres.common.crd.sgbackup.StackGresBackupProcess;
import io.stackgres.common.crd.sgbackup.StackGresBackupStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@ApplicationScoped
public class BackupStatusManager {

  private static final Logger LOGGER = LoggerFactory.getLogger(BackupStatusManager.class);

  private static String getBackupId(StackGresBackup backup) {
    return backup.getMetadata().getNamespace() + "/" + backup.getMetadata().getName();
  }

  public StackGresBackup refreshCondition(StackGresBackup source) {
    if (isBackupStatusNotInitialized(source)) {
      LOGGER.debug("Backup {} is not initialized", getBackupId(source));
      if (source.getStatus() == null) {
        source.setStatus(new StackGresBackupStatus());
      }
      if (source.getStatus().getProcess() == null) {
        source.getStatus().setProcess(new StackGresBackupProcess());
      }
      source.getStatus().getProcess().setStatus(BackupPhase.PENDING.label());
    }
    return source;
  }

  private boolean isBackupStatusNotInitialized(StackGresBackup source) {
    return Optional.of(source)
        .map(StackGresBackup::getStatus)
        .map(StackGresBackupStatus::getProcess)
        .map(StackGresBackupProcess::getStatus)
        .isEmpty();
  }

}
