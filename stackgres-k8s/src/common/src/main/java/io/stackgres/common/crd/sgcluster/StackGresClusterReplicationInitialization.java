/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.common.crd.sgcluster;

import java.time.Duration;
import java.time.format.DateTimeParseException;
import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import io.quarkus.runtime.annotations.RegisterForReflection;
import io.stackgres.common.StackGresUtil;
import io.stackgres.common.crd.sgbackup.StackGresBaseBackupPerformance;
import io.stackgres.common.validation.FieldReference;
import io.stackgres.common.validation.FieldReference.ReferencedField;
import io.stackgres.common.validation.ValidEnum;
import io.sundr.builder.annotations.Buildable;
import jakarta.validation.Valid;
import jakarta.validation.constraints.AssertTrue;

@RegisterForReflection
@JsonInclude(JsonInclude.Include.NON_DEFAULT)
@JsonIgnoreProperties(ignoreUnknown = true)
@Buildable(editableEnabled = false, validationEnabled = false, generateBuilderPackage = false,
    lazyCollectionInitEnabled = false, lazyMapInitEnabled = false,
    builderPackage = "io.fabric8.kubernetes.api.builder")
public class StackGresClusterReplicationInitialization {

  @ValidEnum(enumClass = StackGresReplicationInitializationMode.class, allowNulls = false,
      message = "mode must be BackupFromPrimary, BackupFromReplica,"
          + " FromExistingBackup or FromNewlyCreatedBackup")
  private String mode;

  private String backupNewerThan;

  @Valid
  private StackGresBaseBackupPerformance backupRestorePerformance;

  @ReferencedField("backupNewerThan")
  interface BackupNewerThan extends FieldReference { }

  @JsonIgnore
  @AssertTrue(message = "backupNewerThan must be positive and in ISO 8601 duration format:"
      + " `PnDTnHnMn.nS`.",
      payload = BackupNewerThan.class)
  public boolean isBackupNewerThanValid() {
    try {
      if (backupNewerThan != null) {
        return !Duration.parse(backupNewerThan).isNegative();
      }
      return true;
    } catch (DateTimeParseException ex) {
      return false;
    }
  }

  public String getMode() {
    return mode;
  }

  public void setMode(String mode) {
    this.mode = mode;
  }

  public String getBackupNewerThan() {
    return backupNewerThan;
  }

  public void setBackupNewerThan(String backupNewerThan) {
    this.backupNewerThan = backupNewerThan;
  }

  public StackGresBaseBackupPerformance getBackupRestorePerformance() {
    return backupRestorePerformance;
  }

  public void setBackupRestorePerformance(StackGresBaseBackupPerformance backupRestorePerformance) {
    this.backupRestorePerformance = backupRestorePerformance;
  }

  @Override
  public int hashCode() {
    return Objects.hash(backupNewerThan, backupRestorePerformance, mode);
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (!(obj instanceof StackGresClusterReplicationInitialization)) {
      return false;
    }
    StackGresClusterReplicationInitialization other = (StackGresClusterReplicationInitialization) obj;
    return Objects.equals(backupNewerThan, other.backupNewerThan)
        && Objects.equals(backupRestorePerformance, other.backupRestorePerformance) && Objects.equals(mode, other.mode);
  }

  public String toString() {
    return StackGresUtil.toPrettyYaml(this);
  }
}
