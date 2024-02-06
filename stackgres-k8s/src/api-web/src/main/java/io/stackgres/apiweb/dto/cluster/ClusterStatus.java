/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.apiweb.dto.cluster;

import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.quarkus.runtime.annotations.RegisterForReflection;
import io.stackgres.common.StackGresUtil;

@RegisterForReflection
@JsonInclude(JsonInclude.Include.NON_DEFAULT)
public class ClusterStatus {

  private Integer instances;

  private String labelSelector;

  private List<ClusterCondition> conditions = new ArrayList<>();

  private List<ClusterPodStatus> podStatuses;

  private ClusterDbOpsStatus dbOps;

  private ClusterManagedSqlStatus managedSql;

  private String arch;

  private String os;

  private String labelPrefix;

  private ClusterServiceBindingStatus binding;

  public Integer getInstances() {
    return instances;
  }

  public void setInstances(Integer instances) {
    this.instances = instances;
  }

  public String getLabelSelector() {
    return labelSelector;
  }

  public void setLabelSelector(String labelSelector) {
    this.labelSelector = labelSelector;
  }

  public List<ClusterCondition> getConditions() {
    return conditions;
  }

  public void setConditions(List<ClusterCondition> conditions) {
    this.conditions = conditions;
  }

  public List<ClusterPodStatus> getPodStatuses() {
    return podStatuses;
  }

  public void setPodStatuses(List<ClusterPodStatus> podStatuses) {
    this.podStatuses = podStatuses;
  }

  public ClusterDbOpsStatus getDbOps() {
    return dbOps;
  }

  public void setDbOps(ClusterDbOpsStatus dbOps) {
    this.dbOps = dbOps;
  }

  public ClusterManagedSqlStatus getManagedSql() {
    return managedSql;
  }

  public void setManagedSql(ClusterManagedSqlStatus managedSql) {
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

  public ClusterServiceBindingStatus getBinding() {
    return binding;
  }

  public void setBinding(ClusterServiceBindingStatus binding) {
    this.binding = binding;
  }

  @Override
  public String toString() {
    return StackGresUtil.toPrettyYaml(this);
  }
}
