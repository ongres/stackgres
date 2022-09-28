/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.common.crd.sgcluster;

import java.util.List;
import java.util.Objects;

import javax.validation.Valid;
import javax.validation.constraints.AssertTrue;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.quarkus.runtime.annotations.RegisterForReflection;
import io.stackgres.common.StackGresUtil;
import io.sundr.builder.annotations.Buildable;
import org.jooq.lambda.Seq;

@RegisterForReflection
@JsonInclude(JsonInclude.Include.NON_DEFAULT)
@JsonIgnoreProperties(ignoreUnknown = true)
@Buildable(editableEnabled = false, validationEnabled = false, lazyCollectionInitEnabled = false)
public class StackGresClusterPodStatus {

  @JsonProperty("name")
  private String name;

  @JsonProperty("replicationGroup")
  private Integer replicationGroup;

  @JsonProperty("primary")
  private Boolean primary;

  @JsonProperty("pendingRestart")
  private Boolean pendingRestart;

  @JsonProperty("installedPostgresExtensions")
  @Valid
  private List<StackGresClusterInstalledExtension> installedPostgresExtensions;

  @JsonIgnore
  @AssertTrue(message = "installedPostgresExtensions must contain extensions with unique names.")
  public boolean isInstalledPostgresExtensionsUniqueNames() {
    return Seq.seq(installedPostgresExtensions)
        .grouped(StackGresClusterInstalledExtension::getName)
        .noneMatch(group -> group.v2.count() > 1);
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public Integer getReplicationGroup() {
    return replicationGroup;
  }

  public void setReplicationGroup(Integer replicationGroup) {
    this.replicationGroup = replicationGroup;
  }

  public Boolean getPrimary() {
    return primary;
  }

  public void setPrimary(Boolean primary) {
    this.primary = primary;
  }

  public Boolean getPendingRestart() {
    return pendingRestart;
  }

  public void setPendingRestart(Boolean pendingRestart) {
    this.pendingRestart = pendingRestart;
  }

  public List<StackGresClusterInstalledExtension> getInstalledPostgresExtensions() {
    return installedPostgresExtensions;
  }

  public void setInstalledPostgresExtensions(
      List<StackGresClusterInstalledExtension> installedPostgresExtensions) {
    this.installedPostgresExtensions = installedPostgresExtensions;
  }

  @Override
  public int hashCode() {
    return Objects.hash(installedPostgresExtensions, name, pendingRestart, primary,
        replicationGroup);
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (!(obj instanceof StackGresClusterPodStatus)) {
      return false;
    }
    StackGresClusterPodStatus other = (StackGresClusterPodStatus) obj;
    return Objects.equals(installedPostgresExtensions, other.installedPostgresExtensions)
        && Objects.equals(name, other.name) && Objects.equals(pendingRestart, other.pendingRestart)
        && Objects.equals(primary, other.primary)
        && Objects.equals(replicationGroup, other.replicationGroup);
  }

  @Override
  public String toString() {
    return StackGresUtil.toPrettyYaml(this);
  }

}
