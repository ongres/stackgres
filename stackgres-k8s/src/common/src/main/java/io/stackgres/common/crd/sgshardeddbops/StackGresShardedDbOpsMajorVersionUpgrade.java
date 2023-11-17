/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.common.crd.sgshardeddbops;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import io.quarkus.runtime.annotations.RegisterForReflection;
import io.stackgres.common.StackGresUtil;
import io.stackgres.common.validation.FieldReference;
import io.stackgres.common.validation.FieldReference.ReferencedField;
import io.sundr.builder.annotations.Buildable;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotEmpty;

@RegisterForReflection
@JsonInclude(JsonInclude.Include.NON_DEFAULT)
@JsonIgnoreProperties(ignoreUnknown = true)
@Buildable(editableEnabled = false, validationEnabled = false, generateBuilderPackage = false,
    lazyCollectionInitEnabled = false, lazyMapInitEnabled = false,
    builderPackage = "io.fabric8.kubernetes.api.builder")
public class StackGresShardedDbOpsMajorVersionUpgrade {

  @NotEmpty(message = "postgresVersion must not be empty")
  private String postgresVersion;

  @NotEmpty(message = "sgPostgresConfig must not be empty")
  private String sgPostgresConfig;

  private List<String> backupPaths;

  private Boolean link;

  private Boolean clone;

  private Boolean check;

  @ReferencedField("backupPaths")
  interface BackupPaths extends FieldReference { }

  @ReferencedField("link")
  interface Link extends FieldReference { }

  @ReferencedField("clone")
  interface Clone extends FieldReference { }

  @JsonIgnore
  @AssertTrue(message = "backupPath must not be empty or have any blank element",
      payload = { BackupPaths.class })
  public boolean isBackupPathsNotBlank() {
    return backupPaths == null || (!backupPaths.isEmpty()
        && backupPaths.stream().noneMatch(String::isBlank));
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

  public String getSgPostgresConfig() {
    return sgPostgresConfig;
  }

  public void setSgPostgresConfig(String sgPostgresConfig) {
    this.sgPostgresConfig = sgPostgresConfig;
  }

  public List<String> getBackupPaths() {
    return backupPaths;
  }

  public void setBackupPaths(List<String> backupPaths) {
    this.backupPaths = backupPaths;
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

  @Override
  public int hashCode() {
    return Objects.hash(backupPaths, check, clone, link, postgresVersion, sgPostgresConfig);
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (!(obj instanceof StackGresShardedDbOpsMajorVersionUpgrade)) {
      return false;
    }
    StackGresShardedDbOpsMajorVersionUpgrade other = (StackGresShardedDbOpsMajorVersionUpgrade) obj;
    return Objects.equals(backupPaths, other.backupPaths)
        && Objects.equals(check, other.check)
        && Objects.equals(clone, other.clone)
        && Objects.equals(link, other.link)
        && Objects.equals(postgresVersion, other.postgresVersion)
        && Objects.equals(sgPostgresConfig, other.sgPostgresConfig);
  }

  @Override
  public String toString() {
    return StackGresUtil.toPrettyYaml(this);
  }

}
