/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.customresources.sgprofile;

import javax.validation.Valid;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import io.fabric8.kubernetes.api.model.KubernetesResource;
import io.fabric8.kubernetes.api.model.ResourceRequirements;
import io.quarkus.runtime.annotations.RegisterForReflection;

@JsonDeserialize
@JsonInclude(JsonInclude.Include.NON_DEFAULT)
@RegisterForReflection
public class StackGresProfileSpec implements KubernetesResource {

  private static final long serialVersionUID = -1037668102382589521L;

  @JsonProperty("resources")
  @Valid
  private ResourceRequirements resources;

  @JsonProperty("volume")
  @Valid
  private VolumeConfig volume;

  public ResourceRequirements getResources() {
    return resources;
  }

  public void setResources(ResourceRequirements resources) {
    this.resources = resources;
  }

  public VolumeConfig getVolume() {
    return volume;
  }

  public void setVolume(VolumeConfig volume) {
    this.volume = volume;
  }

}
