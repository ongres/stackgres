/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.customresource.sgcluster;

import java.util.List;
import javax.validation.Valid;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Pattern;
import javax.validation.constraints.Positive;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.google.common.base.MoreObjects;
import io.fabric8.kubernetes.api.model.KubernetesResource;
import io.quarkus.runtime.annotations.RegisterForReflection;

@JsonDeserialize
@JsonInclude(JsonInclude.Include.NON_DEFAULT)
@RegisterForReflection
public class StackGresClusterSpec implements KubernetesResource {

  private static final long serialVersionUID = -5276087851826599719L;

  @JsonProperty("instances")
  @Positive(message = "You need at least 1 instance in the cluster")
  private int instances;

  @JsonProperty("postgresVersion")
  @NotBlank(message = "PostgreSQL version is required")
  private String postgresVersion;

  @JsonProperty("configurations")
  @Valid
  @NotNull(message = "cluster configuration cannot be null")
  private StackgresClusterConfiguration configurations;

  @JsonProperty("sgInstanceProfile")
  @NotNull(message = "resource profile must not be null")
  private String resourceProfile;

  @JsonProperty("restore")
  private ClusterRestore restore;

  @JsonProperty("volumeSize")
  @NotNull(message = "Volume size must be specified")
  @Pattern(regexp = "^[0-9]+(\\.[0-9]+)?(Mi|Gi|Ti)$",
      message = "Volume size must be specified in Mi, Gi or Ti")
  private String volumeSize;

  @JsonProperty("storageClass")
  private String storageClass;

  @JsonProperty("prometheusAutobind")
  private Boolean prometheusAutobind;

  @JsonProperty("sidecars")
  private List<String> sidecars;

  @JsonProperty("nonProduction")
  private NonProduction nonProduction;

  public int getInstances() {
    return instances;
  }

  public void setInstances(int instances) {
    this.instances = instances;
  }

  public String getPostgresVersion() {
    return postgresVersion;
  }

  public void setPostgresVersion(String postgresVersion) {
    this.postgresVersion = postgresVersion;
  }

  public StackgresClusterConfiguration getConfigurations() {
    return configurations;
  }

  public void setConfigurations(StackgresClusterConfiguration configurations) {
    this.configurations = configurations;
  }

  public String getResourceProfile() {
    return resourceProfile;
  }

  public void setResourceProfile(String resourceProfile) {
    this.resourceProfile = resourceProfile;
  }

  public ClusterRestore getRestore() {
    return restore;
  }

  public void setRestore(ClusterRestore restore) {
    this.restore = restore;
  }

  public String getVolumeSize() {
    return volumeSize;
  }

  public void setVolumeSize(String volumeSize) {
    this.volumeSize = volumeSize;
  }

  public String getStorageClass() {
    return storageClass;
  }

  public void setStorageClass(String storageClass) {
    this.storageClass = storageClass;
  }

  public Boolean getPrometheusAutobind() {
    return prometheusAutobind;
  }

  public void setPrometheusAutobind(Boolean prometheusAutobind) {
    this.prometheusAutobind = prometheusAutobind;
  }

  public List<String> getSidecars() {
    return sidecars;
  }

  public void setSidecars(List<String> sidecars) {
    this.sidecars = sidecars;
  }

  public NonProduction getNonProduction() {
    return nonProduction;
  }

  public void setNonProduction(NonProduction nonProduction) {
    this.nonProduction = nonProduction;
  }

  @Override
  public String toString() {
    return MoreObjects.toStringHelper(this)
        .omitNullValues()
        .add("instances", instances)
        .add("pgVersion", postgresVersion)
        .add("configurations", getConfigurations())
        .add("resourceProfile", resourceProfile)
        .add("restore", restore)
        .add("volumeSize", volumeSize)
        .add("storageClass", storageClass)
        .add("sidecars", sidecars)
        .add("nonProduction", nonProduction)
        .toString();
  }

}
