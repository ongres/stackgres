/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.apiweb.dto.stream;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.quarkus.runtime.annotations.RegisterForReflection;
import io.stackgres.common.StackGresUtil;
import io.stackgres.common.crd.ResourceRequirements;

@RegisterForReflection
@JsonInclude(JsonInclude.Include.NON_DEFAULT)
public class StreamPods {

  private ResourceRequirements resources;

  private StreamPodsScheduling scheduling;

  private StreamPodsPersistentVolume persistentVolume;

  public ResourceRequirements getResources() {
    return resources;
  }

  public void setResources(ResourceRequirements resources) {
    this.resources = resources;
  }

  public StreamPodsScheduling getScheduling() {
    return scheduling;
  }

  public void setScheduling(StreamPodsScheduling scheduling) {
    this.scheduling = scheduling;
  }

  public StreamPodsPersistentVolume getPersistentVolume() {
    return persistentVolume;
  }

  public void setPersistentVolume(StreamPodsPersistentVolume persistentVolume) {
    this.persistentVolume = persistentVolume;
  }

  @Override
  public String toString() {
    return StackGresUtil.toPrettyYaml(this);
  }

}
