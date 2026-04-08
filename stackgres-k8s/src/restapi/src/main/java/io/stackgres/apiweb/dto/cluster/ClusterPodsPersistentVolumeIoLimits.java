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
public class ClusterPodsPersistentVolumeIoLimits {

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
  public String toString() {
    return StackGresUtil.toPrettyYaml(this);
  }

}
