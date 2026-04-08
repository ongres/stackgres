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

@RegisterForReflection
@JsonInclude(JsonInclude.Include.NON_DEFAULT)
@JsonIgnoreProperties(ignoreUnknown = true)
@Buildable(editableEnabled = false, validationEnabled = false, generateBuilderPackage = false,
    lazyCollectionInitEnabled = false, lazyMapInitEnabled = false,
    builderPackage = "io.fabric8.kubernetes.api.builder")
public class StackGresClusterPodsPersistentVolumeIoLimits {

  private Integer readIops;

  private Integer writeIops;

  private Integer readMiBps;

  private Integer writeMiBps;

  public Integer getReadIops() {
    return readIops;
  }

  public void setReadIops(Integer readIops) {
    this.readIops = readIops;
  }

  public Integer getWriteIops() {
    return writeIops;
  }

  public void setWriteIops(Integer writeIops) {
    this.writeIops = writeIops;
  }

  public Integer getReadMiBps() {
    return readMiBps;
  }

  public void setReadMiBps(Integer readMiBps) {
    this.readMiBps = readMiBps;
  }

  public Integer getWriteMiBps() {
    return writeMiBps;
  }

  public void setWriteMiBps(Integer writeMiBps) {
    this.writeMiBps = writeMiBps;
  }

  @Override
  public int hashCode() {
    return Objects.hash(readIops, readMiBps, writeIops, writeMiBps);
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (!(obj instanceof StackGresClusterPodsPersistentVolumeIoLimits)) {
      return false;
    }
    StackGresClusterPodsPersistentVolumeIoLimits other = (StackGresClusterPodsPersistentVolumeIoLimits) obj;
    return Objects.equals(readIops, other.readIops) && Objects.equals(readMiBps, other.readMiBps)
        && Objects.equals(writeIops, other.writeIops)
        && Objects.equals(writeMiBps, other.writeMiBps);
  }

  @Override
  public String toString() {
    return StackGresUtil.toPrettyYaml(this);
  }

}
