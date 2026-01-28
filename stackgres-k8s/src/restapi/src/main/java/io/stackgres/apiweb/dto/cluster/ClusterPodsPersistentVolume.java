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
public class ClusterPodsPersistentVolume {

  private String size;

  private String storageClass;

  private String fsGroupChangePolicy;

  public void setStorageClass(String storageClass) {
    this.storageClass = storageClass;
  }

  public String getStorageClass() {
    return storageClass;
  }

  public void setSize(String size) {
    this.size = size;
  }

  public String getSize() {
    return size;
  }

  public String getFsGroupChangePolicy() {
    return fsGroupChangePolicy;
  }

  public void setFsGroupChangePolicy(String fsGroupChangePolicy) {
    this.fsGroupChangePolicy = fsGroupChangePolicy;
  }

  @Override
  public String toString() {
    return StackGresUtil.toPrettyYaml(this);
  }

}
