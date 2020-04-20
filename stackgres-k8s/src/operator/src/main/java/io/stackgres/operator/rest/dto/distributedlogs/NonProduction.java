/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.rest.dto.distributedlogs;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.base.MoreObjects;

public class NonProduction {

  @JsonProperty("disableClusterPodAntiAffinity")
  public Boolean disableClusterPodAntiAffinity;

  public Boolean getDisableClusterPodAntiAffinity() {
    return disableClusterPodAntiAffinity;
  }

  public void setDisableClusterPodAntiAffinity(Boolean disableClusterPodAntiAffinity) {
    this.disableClusterPodAntiAffinity = disableClusterPodAntiAffinity;
  }

  @Override
  public String toString() {
    return MoreObjects.toStringHelper(this)
        .omitNullValues()
        .add("disableClusterPodAntiAffinity", getDisableClusterPodAntiAffinity())
        .toString();
  }
}
