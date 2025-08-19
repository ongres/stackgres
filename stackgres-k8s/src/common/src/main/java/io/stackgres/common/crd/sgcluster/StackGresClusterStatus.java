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

  private String postgresVersion;

  private String buildVersion;

  private List<StackGresClusterInstalledExtension> extensions;

  private String sgPostgresConfig;

  private List<String> backupPaths;

  private Integer instances;

  private String labelSelector;

  @JsonInclude(JsonInclude.Include.NON_EMPTY)
  @Valid
  private List<Condition> conditions = new ArrayList<>();

  @Valid
  private List<StackGresClusterPodStatus> podStatuses;

  @Valid
  private StackGresClusterDbOpsStatus dbOps;

  @Valid
  private StackGresClusterManagedSqlStatus managedSql;

  private String arch;

  private String os;

  private String labelPrefix;

  @JsonProperty("replicationInitializationFailedSGBackup")
  private String replicationInitializationFailedSgBackup;

  @Valid
  private StackGresClusterServiceBindingStatus binding;

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

  public StackGresClusterServiceBindingStatus getBinding() {
    return binding;
  }

  public void setBinding(StackGresClusterServiceBindingStatus binding) {
    this.binding = binding;
  }

  public String getReplicationInitializationFailedSgBackup() {
    return replicationInitializationFailedSgBackup;
  }

  public void setReplicationInitializationFailedSgBackup(String replicationInitializationFailedSgBackup) {
    this.replicationInitializationFailedSgBackup = replicationInitializationFailedSgBackup;
  }

  @Override
  public int hashCode() {
    return Objects.hash(arch, backupPaths, binding, buildVersion, conditions, dbOps, extensions,
        instances, labelPrefix, labelSelector, managedSql, os, podStatuses, postgresVersion,
        replicationInitializationFailedSgBackup, sgPostgresConfig);
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
    return Objects.equals(arch, other.arch) && Objects.equals(backupPaths, other.backupPaths)
        && Objects.equals(binding, other.binding)
        && Objects.equals(buildVersion, other.buildVersion)
        && Objects.equals(conditions, other.conditions) && Objects.equals(dbOps, other.dbOps)
        && Objects.equals(extensions, other.extensions)
        && Objects.equals(instances, other.instances)
        && Objects.equals(labelPrefix, other.labelPrefix)
        && Objects.equals(labelSelector, other.labelSelector)
        && Objects.equals(managedSql, other.managedSql) && Objects.equals(os, other.os)
        && Objects.equals(podStatuses, other.podStatuses)
        && Objects.equals(postgresVersion, other.postgresVersion)
        && Objects.equals(replicationInitializationFailedSgBackup,
            other.replicationInitializationFailedSgBackup)
        && Objects.equals(sgPostgresConfig, other.sgPostgresConfig);
  }

  @Override
  public String toString() {
    return StackGresUtil.toPrettyYaml(this);
  }

}
