/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.apiweb.dto.cluster;

import java.util.Objects;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.google.common.base.MoreObjects;
import io.quarkus.runtime.annotations.RegisterForReflection;

@JsonDeserialize
@JsonInclude(JsonInclude.Include.NON_DEFAULT)
@RegisterForReflection
public class ClusterPod {

  @JsonProperty("persistentVolume")
  @NotNull(message = "Pod's persistent volume must be specified")
  @Valid
  private ClusterPodPersistentVolume persistentVolume;

  @JsonProperty("disableConnectionPooling")
  private Boolean disableConnectionPooling;

  @JsonProperty("disableMetricsExporter")
  private Boolean disableMetricsExporter;

  @JsonProperty("disablePostgresUtil")
  private Boolean disablePostgresUtil;

  @JsonProperty("metadata")
  @Valid
  private ClusterPodMetadata metadata;

  @Valid
  private PodScheduling scheduling;

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

  public ClusterPodMetadata getMetadata() {
    return metadata;
  }

  public void setMetadata(ClusterPodMetadata metadata) {
    this.metadata = metadata;
  }

  public PodScheduling getScheduling() {
    return scheduling;
  }

  public void setScheduling(PodScheduling scheduling) {
    this.scheduling = scheduling;
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
        && Objects.equals(metadata, that.metadata)
        && Objects.equals(scheduling, that.scheduling);
  }

  @Override
  public int hashCode() {
    return Objects.hash(persistentVolume, disableConnectionPooling,
        disableMetricsExporter, disablePostgresUtil, metadata, scheduling);
  }

  @Override
  public String toString() {
    return MoreObjects.toStringHelper(this)
        .add("persistentVolume", persistentVolume)
        .add("disableConnectionPooling", disableConnectionPooling)
        .add("disableMetricsExporter", disableMetricsExporter)
        .add("disablePostgresUtil", disablePostgresUtil)
        .add("metadata", metadata)
        .add("scheduling", scheduling)
        .toString();
  }
}
