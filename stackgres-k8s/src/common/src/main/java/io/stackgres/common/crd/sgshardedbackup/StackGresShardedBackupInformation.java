/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.common.crd.sgshardedbackup;

import java.util.Objects;
import java.util.Optional;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import io.quarkus.runtime.annotations.RegisterForReflection;
import io.stackgres.common.StackGresUtil;
import io.sundr.builder.annotations.Buildable;
import jakarta.validation.Valid;

@RegisterForReflection
@JsonInclude(JsonInclude.Include.NON_DEFAULT)
@JsonIgnoreProperties(ignoreUnknown = true)
@Buildable(editableEnabled = false, validationEnabled = false, generateBuilderPackage = false,
    lazyCollectionInitEnabled = false, lazyMapInitEnabled = false,
    builderPackage = "io.fabric8.kubernetes.api.builder")
public class StackGresShardedBackupInformation {

  private String postgresVersion;

  @Valid
  private StackGresShardedBackupSize size;

  @JsonIgnore
  public String getPostgresMajorVersion() {
    return Optional.ofNullable(postgresVersion)
        .map(version -> version.substring(0, 2))
        .orElse(null);
  }

  public String getPostgresVersion() {
    return postgresVersion;
  }

  public void setPostgresVersion(String postgresVersion) {
    this.postgresVersion = postgresVersion;
  }

  public StackGresShardedBackupSize getSize() {
    return size;
  }

  public void setSize(StackGresShardedBackupSize size) {
    this.size = size;
  }

  @Override
  public int hashCode() {
    return Objects.hash(postgresVersion, size);
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (!(obj instanceof StackGresShardedBackupInformation)) {
      return false;
    }
    StackGresShardedBackupInformation other = (StackGresShardedBackupInformation) obj;
    return Objects.equals(postgresVersion, other.postgresVersion)
        && Objects.equals(size, other.size);
  }

  @Override
  public String toString() {
    return StackGresUtil.toPrettyYaml(this);
  }

}
