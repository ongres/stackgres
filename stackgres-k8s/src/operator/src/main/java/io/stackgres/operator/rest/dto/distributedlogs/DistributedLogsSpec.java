/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.rest.dto.distributedlogs;

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
public class DistributedLogsSpec implements KubernetesResource {

  private static final long serialVersionUID = 1L;

  @JsonProperty("persistentVolume")
  @Valid
  @NotNull(message = "Persistent volume must be specified")
  private DistributedLogsPersistentVolume persistentVolume;

  public DistributedLogsPersistentVolume getPersistentVolume() {
    return persistentVolume;
  }

  public void setPersistentVolume(
      DistributedLogsPersistentVolume persistentVolume) {
    this.persistentVolume = persistentVolume;
  }

  @Override
  public String toString() {
    return MoreObjects.toStringHelper(this)
        .omitNullValues()
        .add("persistentVolume", persistentVolume)
        .toString();
  }
}
