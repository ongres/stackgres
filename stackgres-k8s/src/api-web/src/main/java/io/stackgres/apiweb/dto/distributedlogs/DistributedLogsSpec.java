/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.apiweb.dto.distributedlogs;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import io.fabric8.kubernetes.api.model.KubernetesResource;
import io.quarkus.runtime.annotations.RegisterForReflection;
import io.stackgres.common.StackGresUtil;

@JsonDeserialize
@JsonInclude(JsonInclude.Include.NON_DEFAULT)
@RegisterForReflection
public class DistributedLogsSpec implements KubernetesResource {

  private static final long serialVersionUID = 1L;

  @JsonProperty("persistentVolume")
  private DistributedLogsPersistentVolume persistentVolume;

  @JsonProperty("nonProductionOptions")
  private DistributedLogsNonProduction nonProduction;

  @JsonProperty("scheduling")
  private DistributedLogsPodScheduling scheduling;

  @JsonProperty("metadata")
  private DistributedLogsSpecMetadata metadata;

  public DistributedLogsPersistentVolume getPersistentVolume() {
    return persistentVolume;
  }

  public void setPersistentVolume(
      DistributedLogsPersistentVolume persistentVolume) {
    this.persistentVolume = persistentVolume;
  }

  public DistributedLogsNonProduction getNonProduction() {
    return nonProduction;
  }

  public void setNonProduction(DistributedLogsNonProduction nonProduction) {
    this.nonProduction = nonProduction;
  }

  public DistributedLogsPodScheduling getScheduling() {
    return scheduling;
  }

  public void setScheduling(DistributedLogsPodScheduling scheduling) {
    this.scheduling = scheduling;
  }

  public DistributedLogsSpecMetadata getMetadata() {
    return metadata;
  }

  public void setMetadata(DistributedLogsSpecMetadata metadata) {
    this.metadata = metadata;
  }

  @Override
  public String toString() {
    return StackGresUtil.toPrettyYaml(this);
  }
}
