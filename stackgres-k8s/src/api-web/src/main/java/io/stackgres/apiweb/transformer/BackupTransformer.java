/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.apiweb.transformer;

import java.util.Optional;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import io.stackgres.apiweb.dto.backup.BackupDto;
import io.stackgres.apiweb.dto.backup.BackupInformation;
import io.stackgres.apiweb.dto.backup.BackupLsn;
import io.stackgres.apiweb.dto.backup.BackupProcess;
import io.stackgres.apiweb.dto.backup.BackupSize;
import io.stackgres.apiweb.dto.backup.BackupSpec;
import io.stackgres.apiweb.dto.backup.BackupStatus;
import io.stackgres.apiweb.dto.backup.BackupTiming;
import io.stackgres.common.crd.sgbackup.StackGresBackup;
import io.stackgres.common.crd.sgbackup.StackGresBackupInformation;
import io.stackgres.common.crd.sgbackup.StackGresBackupProcess;
import io.stackgres.common.crd.sgbackup.StackGresBackupSpec;
import io.stackgres.common.crd.sgbackup.StackGresBackupStatus;
import io.stackgres.common.crd.sgbackup.StackgresBackupLsn;
import io.stackgres.common.crd.sgbackup.StackgresBackupSize;
import io.stackgres.common.crd.sgbackup.StackgresBackupTiming;

@ApplicationScoped
public class BackupTransformer extends AbstractResourceTransformer<BackupDto, StackGresBackup> {

  private BackupConfigTransformer backupConfigTransformer;

  @Inject
  public void setBackupConfigTransformer(BackupConfigTransformer backupConfigTransformer) {
    this.backupConfigTransformer = backupConfigTransformer;
  }

  @Override
  public StackGresBackup toCustomResource(BackupDto source, StackGresBackup original) {
    StackGresBackup transformation = Optional.ofNullable(original)
        .orElseGet(StackGresBackup::new);
    transformation.setMetadata(getCustomResourceMetadata(source, original));
    transformation.setSpec(getCustomResourceSpec(source.getSpec()));
    return transformation;
  }

  @Override
  public BackupDto toResource(StackGresBackup source) {
    BackupDto transformation = new BackupDto();
    transformation.setMetadata(getResourceMetadata(source));
    transformation.setSpec(getResourceSpec(source.getSpec()));
    transformation.setStatus(getResourceStatus(source.getStatus()));
    return transformation;
  }

  private StackGresBackupSpec getCustomResourceSpec(BackupSpec source) {
    if (source == null) {
      return null;
    }
    StackGresBackupSpec transformation = new StackGresBackupSpec();
    transformation.setSgCluster(source.getCluster());
    transformation.setManagedLifecycle(source.getManagedLifecycle());
    return transformation;
  }

  private BackupSpec getResourceSpec(StackGresBackupSpec source) {
    if (source == null) {
      return null;
    }
    BackupSpec transformation = new BackupSpec();
    transformation.setCluster(source.getSgCluster());
    transformation.setManagedLifecycle(source.getManagedLifecycle());
    return transformation;
  }

  private BackupStatus getResourceStatus(StackGresBackupStatus source) {
    if (source == null) {
      return null;
    }
    BackupStatus transformation = new BackupStatus();
    transformation.setBackupConfig(
        backupConfigTransformer.getResourceSpec(source.getBackupConfig()));

    final StackGresBackupInformation sourceBackupInformation = source.getBackupInformation();
    transformation.setInternalName(source.getInternalName());
    transformation.setTested(source.getTested());

    if (sourceBackupInformation != null) {
      final BackupInformation backupInformation = new BackupInformation();
      transformation.setBackupInformation(backupInformation);

      backupInformation.setControlData(sourceBackupInformation.getControlData());
      backupInformation.setPgData(sourceBackupInformation.getPgData());
      backupInformation.setHostname(sourceBackupInformation.getHostname());
      backupInformation.setPostgresVersion(sourceBackupInformation.getPostgresVersion());
      backupInformation.setSystemIdentifier(sourceBackupInformation.getSystemIdentifier());
      backupInformation.setStartWalFile(sourceBackupInformation.getStartWalFile());
      backupInformation.setTimeline(sourceBackupInformation.getTimeline());

      final StackgresBackupSize sourceSize = sourceBackupInformation.getSize();
      if (sourceSize != null) {
        final BackupSize size = new BackupSize();
        backupInformation.setSize(size);
        size.setCompressed(sourceSize.getCompressed());
        size.setUncompressed(sourceSize.getUncompressed());
      }

      final StackgresBackupLsn sourceLsn = sourceBackupInformation.getLsn();
      if (sourceLsn != null) {
        final BackupLsn lsn = new BackupLsn();
        backupInformation.setLsn(lsn);
        lsn.setEnd(sourceLsn.getEnd());
        lsn.setStart(sourceLsn.getStart());
      }
    }

    final StackGresBackupProcess sourceProcess = source.getProcess();
    if (sourceProcess != null) {

      final BackupProcess process = new BackupProcess();
      transformation.setProcess(process);
      process.setManagedLifecycle(sourceProcess.getManagedLifecycle());
      process.setJobPod(sourceProcess.getJobPod());
      process.setStatus(sourceProcess.getStatus());
      process.setFailure(sourceProcess.getFailure());

      final StackgresBackupTiming sourceTiming = sourceProcess.getTiming();
      if (sourceTiming != null) {
        final BackupTiming timing = new BackupTiming();
        process.setTiming(timing);

        timing.setEnd(sourceTiming.getEnd());
        timing.setStart(sourceTiming.getStart());
        timing.setStored(sourceTiming.getStored());
      }
    }
    return transformation;
  }

}
