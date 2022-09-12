/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.apiweb.dto.cluster;

import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.quarkus.runtime.annotations.RegisterForReflection;
import io.stackgres.common.StackGresUtil;

@RegisterForReflection
@JsonInclude(JsonInclude.Include.NON_DEFAULT)
//TODO remove once the UI has fixes the sending metadata in this object
@JsonIgnoreProperties(ignoreUnknown = true)
public class ClusterPod {

  @JsonProperty("persistentVolume")
  private ClusterPodPersistentVolume persistentVolume;

  @JsonProperty("disableConnectionPooling")
  private Boolean disableConnectionPooling;

  @JsonProperty("disableMetricsExporter")
  private Boolean disableMetricsExporter;

  @JsonProperty("disablePostgresUtil")
  private Boolean disablePostgresUtil;

  @JsonProperty("scheduling")
  private ClusterPodScheduling scheduling;

  public ClusterPodPersistentVolume getPersistentVolume() {
    return persistentVolume;
  }

  public void setPersistentVolume(ClusterPodPersistentVolume persistentVolume) {
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

  public ClusterPodScheduling getScheduling() {
    return scheduling;
  }

  public void setScheduling(ClusterPodScheduling scheduling) {
    this.scheduling = scheduling;
  }

  @Override
  public String toString() {
    return StackGresUtil.toPrettyYaml(this);
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    ClusterPod that = (ClusterPod) o;
    return Objects.equals(persistentVolume, that.persistentVolume)
        && Objects.equals(disableConnectionPooling, that.disableConnectionPooling)
        && Objects.equals(disableMetricsExporter, that.disableMetricsExporter)
        && Objects.equals(disablePostgresUtil, that.disablePostgresUtil)
        && Objects.equals(scheduling, that.scheduling);
  }

  @Override
  public int hashCode() {
    return Objects.hash(persistentVolume, disableConnectionPooling, disableMetricsExporter,
        disablePostgresUtil, scheduling);
  }
}
