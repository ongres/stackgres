/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.common.crd.sgdistributedlogs;

import java.util.Objects;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.google.common.base.MoreObjects;
import io.fabric8.kubernetes.api.model.KubernetesResource;
import io.quarkus.runtime.annotations.RegisterForReflection;

@JsonDeserialize
@JsonInclude(JsonInclude.Include.NON_DEFAULT)
@RegisterForReflection
public class StackGresDistributedLogsSpec implements KubernetesResource {

  private static final long serialVersionUID = 1L;

  @JsonProperty("persistentVolume")
  @NotNull(message = "Persistent volume must be specified")
  @Valid
  private StackGresDistributedLogsPersistentVolume persistentVolume;

  @JsonProperty("nonProductionOptions")
  @Valid
  private StackGresDistributedLogsNonProduction nonProduction;

  @JsonProperty("scheduling")
  @Valid
  private StackGresDistributedLogsPodScheduling scheduling;

  @JsonProperty("metadata")
  @Valid
  private StackGresDistributedLogsSpecMetadata metadata;

  public StackGresDistributedLogsPersistentVolume getPersistentVolume() {
    return persistentVolume;
  }

  public void setPersistentVolume(
      StackGresDistributedLogsPersistentVolume persistentVolume) {
    this.persistentVolume = persistentVolume;
  }

  public StackGresDistributedLogsNonProduction getNonProduction() {
    return nonProduction;
  }

  public void setNonProduction(StackGresDistributedLogsNonProduction nonProduction) {
    this.nonProduction = nonProduction;
  }

  public StackGresDistributedLogsPodScheduling getScheduling() {
    return scheduling;
  }

  public void setScheduling(StackGresDistributedLogsPodScheduling scheduling) {
    this.scheduling = scheduling;
  }

  public StackGresDistributedLogsSpecMetadata getMetadata() {
    return metadata;
  }

  public void setMetadata(StackGresDistributedLogsSpecMetadata metadata) {
    this.metadata = metadata;
  }

  @Override
  public int hashCode() {
    return Objects.hash(metadata, nonProduction, persistentVolume, scheduling);
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (!(obj instanceof StackGresDistributedLogsSpec)) {
      return false;
    }
    StackGresDistributedLogsSpec other = (StackGresDistributedLogsSpec) obj;
    return Objects.equals(metadata, other.metadata)
        && Objects.equals(nonProduction, other.nonProduction)
        && Objects.equals(persistentVolume, other.persistentVolume)
        && Objects.equals(scheduling, other.scheduling);
  }

  @Override
  public String toString() {
    return MoreObjects.toStringHelper(this)
        .omitNullValues()
        .add("persistentVolume", persistentVolume)
        .add("scheduling", scheduling)
        .add("metadata", metadata)
        .add("nonProduction", nonProduction)
        .toString();
  }
}
