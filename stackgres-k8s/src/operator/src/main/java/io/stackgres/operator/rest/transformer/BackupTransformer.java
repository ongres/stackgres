/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.rest.transformer;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import io.stackgres.operator.customresource.sgbackup.StackGresBackup;
import io.stackgres.operator.customresource.sgbackup.StackGresBackupInformation;
import io.stackgres.operator.customresource.sgbackup.StackGresBackupProcess;
import io.stackgres.operator.customresource.sgbackup.StackGresBackupSpec;
import io.stackgres.operator.customresource.sgbackup.StackGresBackupStatus;
import io.stackgres.operator.customresource.sgbackup.StackgresBackupLsn;
import io.stackgres.operator.customresource.sgbackup.StackgresBackupSize;
import io.stackgres.operator.customresource.sgbackup.StackgresBackupTiming;
import io.stackgres.operator.rest.dto.backup.BackupDto;
import io.stackgres.operator.rest.dto.backup.BackupInformation;
import io.stackgres.operator.rest.dto.backup.BackupLsn;
import io.stackgres.operator.rest.dto.backup.BackupProcess;
import io.stackgres.operator.rest.dto.backup.BackupSize;
import io.stackgres.operator.rest.dto.backup.BackupSpec;
import io.stackgres.operator.rest.dto.backup.BackupStatus;
import io.stackgres.operator.rest.dto.backup.BackupTiming;

@ApplicationScoped
public class BackupTransformer extends AbstractResourceTransformer<BackupDto, StackGresBackup> {

  private BackupConfigTransformer backupConfigTransformer;

  @Inject
  public void setBackupConfigTransformer(BackupConfigTransformer backupConfigTransformer) {
    this.backupConfigTransformer = backupConfigTransformer;
  }

  @Override
  public StackGresBackup toCustomResource(BackupDto source) {
    StackGresBackup transformation = new StackGresBackup();
    transformation.setMetadata(getCustomResourceMetadata(source));
    transformation.setSpec(getCustomResourceSpec(source.getSpec()));
    transformation.setStatus(getCustomResourceStatus(source.getStatus()));
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
    StackGresBackupSpec transformation = new StackGresBackupSpec();
    transformation.setSgCluster(source.getCluster());
    transformation.setSubjectToRetentionPolicy(source.getIsPermanent());
    return transformation;
  }

  private StackGresBackupStatus getCustomResourceStatus(BackupStatus source) {
    if (source == null) {
      return null;
    }
    StackGresBackupStatus transformation = new StackGresBackupStatus();
    transformation.setInternalName(source.getInternalName());
    transformation.setTested(source.getTested());

    transformation.setBackupConfig(
        backupConfigTransformer.getCustomResourceSpec(source.getBackupConfig()));
    final BackupInformation sourceBackupInformation = source.getBackupInformation();
    if (sourceBackupInformation != null) {
      final StackGresBackupInformation backupInformation = new StackGresBackupInformation();
      transformation.setBackupInformation(backupInformation);
      backupInformation.setControlData(sourceBackupInformation.getControlData());
      backupInformation.setPgData(sourceBackupInformation.getPgData());
      backupInformation.setHostname(sourceBackupInformation.getHostname());
      backupInformation.setPostgresVersion(sourceBackupInformation.getPostgresVersion());
      backupInformation.setSystemIdentifier(sourceBackupInformation.getSystemIdentifier());
      backupInformation.setStartWalFile(sourceBackupInformation.getStartWalFile());

      BackupLsn sourceLsn = sourceBackupInformation.getLsn();

      if (sourceLsn != null) {
        final StackgresBackupLsn lsn = new StackgresBackupLsn();
        backupInformation.setLsn(lsn);
        lsn.setEnd(sourceLsn.getEnd());
        lsn.setStart(sourceLsn.getStart());
      }

      BackupSize sourceSize = sourceBackupInformation.getSize();

      if (sourceSize != null) {
        StackgresBackupSize size = new StackgresBackupSize();
        backupInformation.setSize(size);
        size.setCompressed(sourceSize.getCompressed());
        size.setUncompressed(sourceSize.getUncompressed());
      }

    }

    BackupProcess sourceProcess = source.getProcess();
    if (sourceProcess != null) {

      final StackGresBackupProcess process = new StackGresBackupProcess();
      transformation.setProcess(process);

      process.setFailure(sourceProcess.getFailure());
      process.setSubjectToRetentionPolicy(sourceProcess.getSubjectToRetentionPolicy());
      process.setStatus(sourceProcess.getStatus());
      process.setJobPod(sourceProcess.getJobPod());

      BackupTiming sourceTiming = sourceProcess.getTiming();
      if (sourceTiming != null) {
        StackgresBackupTiming timing = new StackgresBackupTiming();
        process.setTiming(timing);
        timing.setEnd(sourceTiming.getEnd());
        timing.setStart(sourceTiming.getStart());
        timing.setStored(sourceTiming.getStored());

      }

    }

    return transformation;
  }

  private BackupSpec getResourceSpec(StackGresBackupSpec source) {
    BackupSpec transformation = new BackupSpec();
    transformation.setCluster(source.getSgCluster());
    transformation.setIsPermanent(source.getSubjectToRetentionPolicy());
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
      process.setSubjectToRetentionPolicy(sourceProcess.getSubjectToRetentionPolicy());
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
