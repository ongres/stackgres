/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.common.crd.sgcluster;

import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import io.quarkus.runtime.annotations.RegisterForReflection;
import io.stackgres.common.StackGresUtil;
import io.sundr.builder.annotations.Buildable;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

@RegisterForReflection
@JsonInclude(JsonInclude.Include.NON_DEFAULT)
@JsonIgnoreProperties(ignoreUnknown = true)
@Buildable(editableEnabled = false, generateBuilderPackage = false,
    lazyCollectionInitEnabled = false, lazyMapInitEnabled = false,
    builderPackage = "io.fabric8.kubernetes.api.builder")
public class StackGresClusterPodsCustomPersistentVolume {

  @NotNull(message = "Custom persistent volume name must be specified")
  @Pattern(regexp = "^[a-z0-9]([-a-z0-9]*[a-z0-9])?$",
      message = "Custom persistent volume name must be a valid DNS-1123 label")
  @Size(max = 56, message = "Custom persistent volume name must not be longer than 56 characters")
  private String name;

  @NotNull(message = "Volume size must be specified")
  @Pattern(regexp = "^[0-9]+(\\.[0-9]+)?(Mi|Gi|Ti)$",
      message = "Volume size must be specified in Mi, Gi or Ti")
  private String size;

  private String storageClass;

  private String volumeAttributesClassName;

  @Valid
  private StackGresClusterPodsPersistentVolumeIoLimits ioLimits;

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

  public StackGresClusterPodsPersistentVolumeIoLimits getIoLimits() {
    return ioLimits;
  }

  public void setIoLimits(StackGresClusterPodsPersistentVolumeIoLimits ioLimits) {
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
  public int hashCode() {
    return Objects.hash(allowCoherentDataRemoval, coherentData, ioLimits, name, size,
        storageClass, volumeAttributesClassName);
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (!(obj instanceof StackGresClusterPodsCustomPersistentVolume)) {
      return false;
    }
    StackGresClusterPodsCustomPersistentVolume other =
        (StackGresClusterPodsCustomPersistentVolume) obj;
    return Objects.equals(allowCoherentDataRemoval, other.allowCoherentDataRemoval)
        && Objects.equals(coherentData, other.coherentData)
        && Objects.equals(ioLimits, other.ioLimits)
        && Objects.equals(name, other.name)
        && Objects.equals(size, other.size)
        && Objects.equals(storageClass, other.storageClass)
        && Objects.equals(volumeAttributesClassName, other.volumeAttributesClassName);
  }

  @Override
  public String toString() {
    return StackGresUtil.toPrettyYaml(this);
  }

}
