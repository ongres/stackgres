/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.apiweb.dto.cluster;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.quarkus.runtime.annotations.RegisterForReflection;
import io.stackgres.common.StackGresUtil;

@RegisterForReflection
@JsonInclude(JsonInclude.Include.NON_DEFAULT)
public class ClusterPodsCustomPersistentVolume {

  private String name;

  private String size;

  private String storageClass;

  private String volumeAttributesClassName;

  private ClusterPodsPersistentVolumeIoLimits ioLimits;

  private Boolean coherentData;

  private Boolean allowCoherentDataRemoval;

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public String getSize() {
    return size;
  }

  public void setSize(String size) {
    this.size = size;
  }

  public String getStorageClass() {
    return storageClass;
  }

  public void setStorageClass(String storageClass) {
    this.storageClass = storageClass;
  }

  public String getVolumeAttributesClassName() {
    return volumeAttributesClassName;
  }

  public void setVolumeAttributesClassName(String volumeAttributesClassName) {
    this.volumeAttributesClassName = volumeAttributesClassName;
  }

  public ClusterPodsPersistentVolumeIoLimits getIoLimits() {
    return ioLimits;
  }

  public void setIoLimits(ClusterPodsPersistentVolumeIoLimits ioLimits) {
    this.ioLimits = ioLimits;
  }

  public Boolean getCoherentData() {
    return coherentData;
  }

  public void setCoherentData(Boolean coherentData) {
    this.coherentData = coherentData;
  }

  public Boolean getAllowCoherentDataRemoval() {
    return allowCoherentDataRemoval;
  }

  public void setAllowCoherentDataRemoval(Boolean allowCoherentDataRemoval) {
    this.allowCoherentDataRemoval = allowCoherentDataRemoval;
  }

  @Override
  public String toString() {
    return StackGresUtil.toPrettyYaml(this);
  }

}
