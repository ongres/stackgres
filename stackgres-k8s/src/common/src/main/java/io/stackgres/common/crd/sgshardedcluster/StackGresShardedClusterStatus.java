/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.common.crd.sgshardedcluster;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.quarkus.runtime.annotations.RegisterForReflection;
import io.stackgres.common.StackGresUtil;
import io.stackgres.common.crd.Condition;
import io.stackgres.common.crd.sgcluster.StackGresClusterInstalledExtension;
import io.stackgres.common.crd.sgcluster.StackGresClusterServiceBindingStatus;
import io.sundr.builder.annotations.Buildable;
import jakarta.validation.Valid;

@RegisterForReflection
@JsonInclude(JsonInclude.Include.NON_DEFAULT)
@JsonIgnoreProperties(ignoreUnknown = true)
@Buildable(editableEnabled = false, validationEnabled = false, generateBuilderPackage = false,
    lazyCollectionInitEnabled = false, lazyMapInitEnabled = false,
    builderPackage = "io.fabric8.kubernetes.api.builder")
public class StackGresShardedClusterStatus {

  @JsonProperty("conditions")
  @JsonInclude(JsonInclude.Include.NON_EMPTY)
  @Valid
  private List<Condition> conditions = new ArrayList<>();

  private String postgresVersion;

  private String buildVersion;

  @Valid
  private List<StackGresClusterInstalledExtension> extensions;

  @Valid
  private List<StackGresShardedClusterClusterStatus> clusterStatuses;

  @Valid
  @Deprecated(forRemoval = true)
  private List<StackGresClusterInstalledExtension> toInstallPostgresExtensions;

  @Valid
  private StackGresClusterServiceBindingStatus binding;

  @Valid
  private StackGresShardedClusterDbOpsStatus dbOps;

  private List<String> sgBackups;

  public List<Condition> getConditions() {
    return conditions;
  }

  public void setConditions(List<Condition> conditions) {
    this.conditions = conditions;
  }

  public String getPostgresVersion() {
    return postgresVersion;
  }

  public void setPostgresVersion(String postgresVersion) {
    this.postgresVersion = postgresVersion;
  }

  public String getBuildVersion() {
    return buildVersion;
  }

  public void setBuildVersion(String buildVersion) {
    this.buildVersion = buildVersion;
  }

  public List<StackGresClusterInstalledExtension> getExtensions() {
    return extensions;
  }

  public void setExtensions(List<StackGresClusterInstalledExtension> extensions) {
    this.extensions = extensions;
  }

  public List<StackGresShardedClusterClusterStatus> getClusterStatuses() {
    return clusterStatuses;
  }

  public void setClusterStatuses(List<StackGresShardedClusterClusterStatus> clusterStatuses) {
    this.clusterStatuses = clusterStatuses;
  }

  @Deprecated(forRemoval = true)
  public List<StackGresClusterInstalledExtension> getToInstallPostgresExtensions() {
    return toInstallPostgresExtensions;
  }

  @Deprecated(forRemoval = true)
  public void setToInstallPostgresExtensions(
      List<StackGresClusterInstalledExtension> toInstallPostgresExtensions) {
    this.toInstallPostgresExtensions = toInstallPostgresExtensions;
  }

  public StackGresClusterServiceBindingStatus getBinding() {
    return binding;
  }

  public void setBinding(StackGresClusterServiceBindingStatus binding) {
    this.binding = binding;
  }

  public StackGresShardedClusterDbOpsStatus getDbOps() {
    return dbOps;
  }

  public void setDbOps(StackGresShardedClusterDbOpsStatus dbOps) {
    this.dbOps = dbOps;
  }

  public List<String> getSgBackups() {
    return sgBackups;
  }

  public void setSgBackups(List<String> sgBackups) {
    this.sgBackups = sgBackups;
  }

  @Override
  public int hashCode() {
    return Objects.hash(binding, buildVersion, clusterStatuses, conditions, dbOps, extensions,
        postgresVersion, sgBackups, toInstallPostgresExtensions);
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (!(obj instanceof StackGresShardedClusterStatus)) {
      return false;
    }
    StackGresShardedClusterStatus other = (StackGresShardedClusterStatus) obj;
    return Objects.equals(binding, other.binding)
        && Objects.equals(buildVersion, other.buildVersion)
        && Objects.equals(clusterStatuses, other.clusterStatuses)
        && Objects.equals(conditions, other.conditions) && Objects.equals(dbOps, other.dbOps)
        && Objects.equals(extensions, other.extensions)
        && Objects.equals(postgresVersion, other.postgresVersion)
        && Objects.equals(sgBackups, other.sgBackups)
        && Objects.equals(toInstallPostgresExtensions, other.toInstallPostgresExtensions);
  }

  @Override
  public String toString() {
    return StackGresUtil.toPrettyYaml(this);
  }

}
