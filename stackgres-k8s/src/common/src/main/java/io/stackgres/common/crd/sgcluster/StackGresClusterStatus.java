/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.common.crd.sgcluster;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import com.fasterxml.jackson.annotation.JsonIgnore;
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
@Buildable(editableEnabled = false, generateBuilderPackage = false,
    lazyCollectionInitEnabled = false, lazyMapInitEnabled = false,
    builderPackage = "io.fabric8.kubernetes.api.builder")
public class StackGresClusterStatus {

  private String postgresVersion;

  private String buildVersion;

  private String latestPostgresMinor;

  private String latestPostgresMajor;

  private List<StackGresClusterInstalledExtension> extensions;

  private String sgPostgresConfig;

  private List<String> backupPaths;

  private String revision;

  private String base;

  private String baseVersion;

  private String baseRevision;

  private String repository;

  private List<@Valid StackGresClusterStatusAddon> addons;

  private Integer instances;

  private String labelSelector;

  @JsonInclude(JsonInclude.Include.NON_EMPTY)
  private List<@Valid Condition> conditions = new ArrayList<>();

  private List<@Valid StackGresClusterPodStatus> podStatuses;

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

  public String getLatestPostgresMinor() {
    return latestPostgresMinor;
  }

  public void setLatestPostgresMinor(String latestPostgresMinor) {
    this.latestPostgresMinor = latestPostgresMinor;
  }

  public String getLatestPostgresMajor() {
    return latestPostgresMajor;
  }

  public void setLatestPostgresMajor(String latestPostgresMajor) {
    this.latestPostgresMajor = latestPostgresMajor;
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

  public String getRevision() {
    return revision;
  }

  public void setRevision(String revision) {
    this.revision = revision;
  }

  public String getBase() {
    return base;
  }

  public void setBase(String base) {
    this.base = base;
  }

  public String getBaseVersion() {
    return baseVersion;
  }

  public void setBaseVersion(String baseVersion) {
    this.baseVersion = baseVersion;
  }

  public String getBaseRevision() {
    return baseRevision;
  }

  public void setBaseRevision(String baseRevision) {
    this.baseRevision = baseRevision;
  }

  public String getRepository() {
    return repository;
  }

  public void setRepository(String repository) {
    this.repository = repository;
  }

  public List<StackGresClusterStatusAddon> getAddons() {
    return addons;
  }

  public void setAddons(List<StackGresClusterStatusAddon> addons) {
    this.addons = addons;
  }

  @JsonIgnore
  public Optional<StackGresClusterStatusAddon> findAddon(String name) {
    return Optional.ofNullable(addons)
        .stream()
        .flatMap(List::stream)
        .filter(addon -> Objects.equals(addon.getName(), name))
        .findFirst();
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
    return Objects.hash(addons, arch, backupPaths, base, baseRevision, baseVersion, binding,
        buildVersion, conditions, dbOps, extensions, instances, labelPrefix, labelSelector,
        latestPostgresMajor, latestPostgresMinor, managedSql, os, podStatuses, postgresVersion,
        replicationInitializationFailedSgBackup, repository, revision, sgPostgresConfig);
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
    return Objects.equals(addons, other.addons) && Objects.equals(arch, other.arch)
        && Objects.equals(backupPaths, other.backupPaths)
        && Objects.equals(base, other.base) && Objects.equals(baseRevision, other.baseRevision)
        && Objects.equals(baseVersion, other.baseVersion) && Objects.equals(binding, other.binding)
        && Objects.equals(buildVersion, other.buildVersion)
        && Objects.equals(conditions, other.conditions) && Objects.equals(dbOps, other.dbOps)
        && Objects.equals(extensions, other.extensions)
        && Objects.equals(instances, other.instances)
        && Objects.equals(labelPrefix, other.labelPrefix)
        && Objects.equals(labelSelector, other.labelSelector)
        && Objects.equals(latestPostgresMajor, other.latestPostgresMajor)
        && Objects.equals(latestPostgresMinor, other.latestPostgresMinor)
        && Objects.equals(managedSql, other.managedSql) && Objects.equals(os, other.os)
        && Objects.equals(podStatuses, other.podStatuses)
        && Objects.equals(postgresVersion, other.postgresVersion)
        && Objects.equals(replicationInitializationFailedSgBackup,
            other.replicationInitializationFailedSgBackup)
        && Objects.equals(repository, other.repository) && Objects.equals(revision, other.revision)
        && Objects.equals(sgPostgresConfig, other.sgPostgresConfig);
  }

  @Override
  public String toString() {
    return StackGresUtil.toPrettyYaml(this);
  }

}
