/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.common.crd.sgcluster;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.quarkus.runtime.annotations.RegisterForReflection;
import io.stackgres.common.StackGresUtil;
import io.stackgres.common.crd.Condition;
import io.sundr.builder.annotations.Buildable;
import jakarta.validation.Valid;

@RegisterForReflection
@JsonInclude(JsonInclude.Include.NON_DEFAULT)
@JsonIgnoreProperties(ignoreUnknown = true)
@Buildable(editableEnabled = false, validationEnabled = false, generateBuilderPackage = false,
    lazyCollectionInitEnabled = false, lazyMapInitEnabled = false,
    builderPackage = "io.fabric8.kubernetes.api.builder")
public class StackGresClusterStatus {

  @JsonProperty("conditions")
  @JsonInclude(JsonInclude.Include.NON_EMPTY)
  @Valid
  private List<Condition> conditions = new ArrayList<>();

  @JsonProperty("podStatuses")
  @Valid
  private List<StackGresClusterPodStatus> podStatuses;

  @JsonProperty("dbOps")
  @Valid
  private StackGresClusterDbOpsStatus dbOps;

  @JsonProperty("managedSql")
  @Valid
  private StackGresClusterManagedSqlStatus managedSql;

  @JsonProperty("arch")
  private String arch;

  @JsonProperty("os")
  private String os;

  @JsonProperty("labelPrefix")
  private String labelPrefix;

  public List<Condition> getConditions() {
    return conditions;
  }

  public void setConditions(List<Condition> conditions) {
    this.conditions = conditions;
  }

  public List<StackGresClusterPodStatus> getPodStatuses() {
    return podStatuses;
  }

  public void setPodStatuses(List<StackGresClusterPodStatus> podStatuses) {
    this.podStatuses = podStatuses;
  }

  public StackGresClusterDbOpsStatus getDbOps() {
    return dbOps;
  }

  public void setDbOps(StackGresClusterDbOpsStatus dbOps) {
    this.dbOps = dbOps;
  }

  public StackGresClusterManagedSqlStatus getManagedSql() {
    return managedSql;
  }

  public void setManagedSql(StackGresClusterManagedSqlStatus managedSql) {
    this.managedSql = managedSql;
  }

  public String getArch() {
    return arch;
  }

  public void setArch(String arch) {
    this.arch = arch;
  }

  public String getOs() {
    return os;
  }

  public void setOs(String os) {
    this.os = os;
  }

  public String getLabelPrefix() {
    return labelPrefix;
  }

  public void setLabelPrefix(String labelPrefix) {
    this.labelPrefix = labelPrefix;
  }

  @Override
  public int hashCode() {
    return Objects.hash(arch, conditions, dbOps, labelPrefix, managedSql, os, podStatuses);
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (!(obj instanceof StackGresClusterStatus)) {
      return false;
    }
    StackGresClusterStatus other = (StackGresClusterStatus) obj;
    return Objects.equals(arch, other.arch) && Objects.equals(conditions, other.conditions)
        && Objects.equals(dbOps, other.dbOps) && Objects.equals(labelPrefix, other.labelPrefix)
        && Objects.equals(managedSql, other.managedSql) && Objects.equals(os, other.os)
        && Objects.equals(podStatuses, other.podStatuses);
  }

  @Override
  public String toString() {
    return StackGresUtil.toPrettyYaml(this);
  }

}
