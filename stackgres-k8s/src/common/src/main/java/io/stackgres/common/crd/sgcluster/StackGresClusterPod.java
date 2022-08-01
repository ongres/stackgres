/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.common.crd.sgcluster;

import java.util.Objects;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import io.quarkus.runtime.annotations.RegisterForReflection;
import io.stackgres.common.StackGresUtil;
import io.stackgres.common.validation.ValidEnum;
import io.sundr.builder.annotations.Buildable;

@JsonDeserialize
@JsonInclude(JsonInclude.Include.NON_DEFAULT)
@RegisterForReflection
@Buildable(editableEnabled = false, validationEnabled = false, lazyCollectionInitEnabled = false)
public class StackGresClusterPod {

  @JsonProperty("persistentVolume")
  @NotNull(message = "Pod's persistent volume must be specified")
  @Valid
  private StackGresPodPersistentVolume persistentVolume;

  @JsonProperty("disableConnectionPooling")
  private Boolean disableConnectionPooling;

  @JsonProperty("disableMetricsExporter")
  private Boolean disableMetricsExporter;

  @JsonProperty("disablePostgresUtil")
  private Boolean disablePostgresUtil;

  @JsonProperty("managementPolicy")
  @ValidEnum(enumClass = StackGresPodManagementPolicy.class, allowNulls = true,
      message = "managementPolicy must be OrderedReady or Parallel")
  private String managementPolicy;

  @Valid
  private StackGresClusterPodScheduling scheduling;

  public StackGresPodPersistentVolume getPersistentVolume() {
    return persistentVolume;
  }

  public void setPersistentVolume(StackGresPodPersistentVolume persistentVolume) {
    this.persistentVolume = persistentVolume;
  }

  public Boolean getDisableConnectionPooling() {
    return disableConnectionPooling;
  }

  public void setDisableConnectionPooling(Boolean disableConnectionPooling) {
    this.disableConnectionPooling = disableConnectionPooling;
  }

  public Boolean getDisableMetricsExporter() {
    return disableMetricsExporter;
  }

  public void setDisableMetricsExporter(Boolean disableMetricsExporter) {
    this.disableMetricsExporter = disableMetricsExporter;
  }

  public Boolean getDisablePostgresUtil() {
    return disablePostgresUtil;
  }

  public void setDisablePostgresUtil(Boolean disablePostgresUtil) {
    this.disablePostgresUtil = disablePostgresUtil;
  }

  public StackGresClusterPodScheduling getScheduling() {
    return scheduling;
  }

  public void setScheduling(StackGresClusterPodScheduling scheduling) {
    this.scheduling = scheduling;
  }

  public String getManagementPolicy() {
    return managementPolicy;
  }

  public void setManagementPolicy(String managementPolicy) {
    this.managementPolicy = managementPolicy;
  }

  @Override
  public int hashCode() {
    return Objects.hash(disableConnectionPooling, disableMetricsExporter, disablePostgresUtil,
        managementPolicy, persistentVolume, scheduling);
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (!(obj instanceof StackGresClusterPod)) {
      return false;
    }
    StackGresClusterPod other = (StackGresClusterPod) obj;
    return Objects.equals(disableConnectionPooling, other.disableConnectionPooling)
        && Objects.equals(disableMetricsExporter, other.disableMetricsExporter)
        && Objects.equals(disablePostgresUtil, other.disablePostgresUtil)
        && Objects.equals(managementPolicy, other.managementPolicy)
        && Objects.equals(persistentVolume, other.persistentVolume)
        && Objects.equals(scheduling, other.scheduling);
  }

  @Override
  public String toString() {
    return StackGresUtil.toPrettyYaml(this);
  }

}
