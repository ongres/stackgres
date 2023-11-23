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

  @Valid
  private List<StackGresShardedClusterClusterStatus> clusterStatuses;

  @Valid
  private List<StackGresClusterInstalledExtension> toInstallPostgresExtensions;

  @Valid
  private StackGresClusterServiceBindingStatus binding;

  @Valid
  private StackGresShardedClusterDbOpsStatus dbOps;

  private List<String> sgBackups;

  public List<String> getSgBackups() {
    return sgBackups;
  }

  public void setSgBackups(List<String> sgBackups) {
    this.sgBackups = sgBackups;
  }

  public List<Condition> getConditions() {
    return conditions;
  }

  public void setConditions(List<Condition> conditions) {
    this.conditions = conditions;
  }

  public List<StackGresShardedClusterClusterStatus> getClusterStatuses() {
    return clusterStatuses;
  }

  public void setClusterStatuses(List<StackGresShardedClusterClusterStatus> clusterStatuses) {
    this.clusterStatuses = clusterStatuses;
  }

  public List<StackGresClusterInstalledExtension> getToInstallPostgresExtensions() {
    return toInstallPostgresExtensions;
  }

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

  @Override
  public int hashCode() {
    return Objects.hash(binding, clusterStatuses, conditions, dbOps, sgBackups,
        toInstallPostgresExtensions);
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
        && Objects.equals(clusterStatuses, other.clusterStatuses)
        && Objects.equals(conditions, other.conditions)
        && Objects.equals(dbOps, other.dbOps)
        && Objects.equals(sgBackups, other.sgBackups)
        && Objects.equals(toInstallPostgresExtensions, other.toInstallPostgresExtensions);
  }

  @Override
  public String toString() {
    return StackGresUtil.toPrettyYaml(this);
  }

}
