/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.common.crd.sgshardedcluster;

import java.util.Objects;

import javax.validation.constraints.NotNull;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import io.quarkus.runtime.annotations.RegisterForReflection;
import io.stackgres.common.StackGresUtil;
import io.sundr.builder.annotations.Buildable;

@RegisterForReflection
@JsonInclude(JsonInclude.Include.NON_DEFAULT)
@JsonIgnoreProperties(ignoreUnknown = true)
@Buildable(editableEnabled = false, validationEnabled = false, generateBuilderPackage = false,
    lazyCollectionInitEnabled = false, lazyMapInitEnabled = false,
    builderPackage = "io.fabric8.kubernetes.api.builder")
public class StackGresShardedClusterDbOpsMajorVersionUpgradeStatus {

  @NotNull
  private String sourcePostgresVersion;

  @NotNull
  private String targetPostgresVersion;

  @NotNull
  private String sourceSgPostgresConfig;

  public String getSourcePostgresVersion() {
    return sourcePostgresVersion;
  }

  public void setSourcePostgresVersion(String sourcePostgresVersion) {
    this.sourcePostgresVersion = sourcePostgresVersion;
  }

  public String getTargetPostgresVersion() {
    return targetPostgresVersion;
  }

  public void setTargetPostgresVersion(String targetPostgresVersion) {
    this.targetPostgresVersion = targetPostgresVersion;
  }

  public String getSourceSgPostgresConfig() {
    return sourceSgPostgresConfig;
  }

  public void setSourceSgPostgresConfig(String sourceSgPostgresConfig) {
    this.sourceSgPostgresConfig = sourceSgPostgresConfig;
  }

  @Override
  public int hashCode() {
    return Objects.hash(sourcePostgresVersion, sourceSgPostgresConfig, targetPostgresVersion);
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (!(obj instanceof StackGresShardedClusterDbOpsMajorVersionUpgradeStatus)) {
      return false;
    }
    StackGresShardedClusterDbOpsMajorVersionUpgradeStatus other =
        (StackGresShardedClusterDbOpsMajorVersionUpgradeStatus) obj;
    return Objects.equals(sourcePostgresVersion, other.sourcePostgresVersion)
        && Objects.equals(sourceSgPostgresConfig, other.sourceSgPostgresConfig)
        && Objects.equals(targetPostgresVersion, other.targetPostgresVersion);
  }

  @Override
  public String toString() {
    return StackGresUtil.toPrettyYaml(this);
  }

}
