/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.customresources.sgprofile;

import javax.validation.constraints.NotBlank;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

import io.fabric8.kubernetes.api.model.KubernetesResource;
import io.quarkus.runtime.annotations.RegisterForReflection;

@JsonDeserialize
@JsonInclude(JsonInclude.Include.NON_DEFAULT)
@RegisterForReflection
public class StackGresProfileSpec implements KubernetesResource {

  private static final long serialVersionUID = -1037668102382589521L;

  @JsonProperty("cpuRequests")
  @NotBlank
  private String cpuRequests;

  @JsonProperty("memoryRequests")
  @NotBlank
  private String memoryRequests;

  @JsonProperty("cpuLimits")
  @NotBlank
  private String cpuLimits;

  @JsonProperty("memoryLimits")
  @NotBlank
  private String memoryLimits;

  @JsonProperty("volumeSize")
  @NotBlank
  private String volumeSize;

  @JsonProperty("volumeStorageClass")
  @NotBlank
  private String volumeStorageClass;

  public String getCpuRequests() {
    return cpuRequests;
  }

  public void setCpuRequests(String cpuRequests) {
    this.cpuRequests = cpuRequests;
  }

  public String getMemoryRequests() {
    return memoryRequests;
  }

  public void setMemoryRequests(String memoryRequests) {
    this.memoryRequests = memoryRequests;
  }

  public String getCpuLimits() {
    return cpuLimits;
  }

  public void setCpuLimits(String cpuLimits) {
    this.cpuLimits = cpuLimits;
  }

  public String getMemoryLimits() {
    return memoryLimits;
  }

  public void setMemoryLimits(String memoryLimits) {
    this.memoryLimits = memoryLimits;
  }

  public String getVolumeSize() {
    return volumeSize;
  }

  public void setVolumeSize(String volumeSize) {
    this.volumeSize = volumeSize;
  }

  public String getVolumeStorageClass() {
    return volumeStorageClass;
  }

  public void setVolumeStorageClass(String volumeStorageClass) {
    this.volumeStorageClass = volumeStorageClass;
  }

}
