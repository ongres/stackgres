/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.common.crd.sgdbops;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import io.quarkus.runtime.annotations.RegisterForReflection;
import io.stackgres.common.StackGresUtil;
import io.stackgres.common.crd.sgcluster.StackGresClusterExtension;
import io.stackgres.common.crd.sgcluster.StackGresClusterInstalledExtension;
import io.stackgres.common.validation.FieldReference;
import io.stackgres.common.validation.FieldReference.ReferencedField;
import io.sundr.builder.annotations.Buildable;
import jakarta.validation.Valid;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotEmpty;

@RegisterForReflection
@JsonInclude(JsonInclude.Include.NON_DEFAULT)
@JsonIgnoreProperties(ignoreUnknown = true)
@Buildable(editableEnabled = false, validationEnabled = false, generateBuilderPackage = false,
    lazyCollectionInitEnabled = false, lazyMapInitEnabled = false,
    builderPackage = "io.fabric8.kubernetes.api.builder")
public class StackGresDbOpsMajorVersionUpgrade {

  @NotEmpty(message = "postgresVersion must not be empty")
  private String postgresVersion;

  @Valid
  private List<StackGresClusterExtension> postgresExtensions;

  @NotEmpty(message = "sgPostgresConfig must not be empty")
  private String sgPostgresConfig;

  private String backupPath;

  private Boolean link;

  private Boolean clone;

  private Boolean check;

  @Valid
  private List<StackGresClusterInstalledExtension> toInstallPostgresExtensions;

  @ReferencedField("backupPath")
  interface BackupPath extends FieldReference { }

  @ReferencedField("link")
  interface Link extends FieldReference { }

  @ReferencedField("clone")
  interface Clone extends FieldReference { }

  @JsonIgnore
  @AssertTrue(message = "backupPath must not be blank",
      payload = { BackupPath.class })
  public boolean isBackupPathNotBlank() {
    return backupPath == null || !backupPath.isBlank();
  }

  @JsonIgnore
  @AssertTrue(message = "link and clone are mutually exclusive",
      payload = { Link.class, Clone.class })
  public boolean isOnlyLinkOrOnlyClone() {
    return !Optional.ofNullable(link).orElse(false)
        || !Optional.ofNullable(clone).orElse(false);
  }

  public String getPostgresVersion() {
    return postgresVersion;
  }

  public void setPostgresVersion(String postgresVersion) {
    this.postgresVersion = postgresVersion;
  }

  public List<StackGresClusterExtension> getPostgresExtensions() {
    return postgresExtensions;
  }

  public void setPostgresExtensions(List<StackGresClusterExtension> postgresExtensions) {
    this.postgresExtensions = postgresExtensions;
  }

  public String getSgPostgresConfig() {
    return sgPostgresConfig;
  }

  public void setSgPostgresConfig(String sgPostgresConfig) {
    this.sgPostgresConfig = sgPostgresConfig;
  }

  public String getBackupPath() {
    return backupPath;
  }

  public void setBackupPath(String backupPath) {
    this.backupPath = backupPath;
  }

  public Boolean getLink() {
    return link;
  }

  public void setLink(Boolean link) {
    this.link = link;
  }

  public Boolean getClone() {
    return clone;
  }

  public void setClone(Boolean clone) {
    this.clone = clone;
  }

  public Boolean getCheck() {
    return check;
  }

  public void setCheck(Boolean check) {
    this.check = check;
  }

  public List<StackGresClusterInstalledExtension> getToInstallPostgresExtensions() {
    return toInstallPostgresExtensions;
  }

  public void setToInstallPostgresExtensions(
      List<StackGresClusterInstalledExtension> toInstallPostgresExtensions) {
    this.toInstallPostgresExtensions = toInstallPostgresExtensions;
  }

  @Override
  public int hashCode() {
    return Objects.hash(backupPath, check, clone, postgresExtensions, link, postgresVersion,
        sgPostgresConfig, toInstallPostgresExtensions);
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (!(obj instanceof StackGresDbOpsMajorVersionUpgrade)) {
      return false;
    }
    StackGresDbOpsMajorVersionUpgrade other = (StackGresDbOpsMajorVersionUpgrade) obj;
    return Objects.equals(backupPath, other.backupPath)
        && Objects.equals(check, other.check)
        && Objects.equals(clone, other.clone)
        && Objects.equals(postgresExtensions, other.postgresExtensions)
        && Objects.equals(link, other.link)
        && Objects.equals(postgresVersion, other.postgresVersion)
        && Objects.equals(sgPostgresConfig, other.sgPostgresConfig)
        && Objects.equals(toInstallPostgresExtensions, other.toInstallPostgresExtensions);
  }

  @Override
  public String toString() {
    return StackGresUtil.toPrettyYaml(this);
  }

}
