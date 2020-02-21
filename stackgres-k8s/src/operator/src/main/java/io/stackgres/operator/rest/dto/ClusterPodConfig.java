/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.rest.dto;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import io.quarkus.runtime.annotations.RegisterForReflection;

@JsonDeserialize
@JsonInclude(JsonInclude.Include.NON_DEFAULT)
@RegisterForReflection
public class ClusterPodConfig {

  @JsonProperty("pods")
  private List<ClusterPodStatus> pods;

  @JsonProperty("podsReady")
  private String podsReady;

  @JsonProperty("grafanaEmbedded")
  private boolean grafanaEmbedded;

  public List<ClusterPodStatus> getPods() {
    return pods;
  }

  public void setPods(List<ClusterPodStatus> pods) {
    this.pods = pods;
  }

  public String getPodsReady() {
    return podsReady;
  }

  public void setPodsReady(String podsReady) {
    this.podsReady = podsReady;
  }

  public boolean isGrafanaEmbedded() {
    return grafanaEmbedded;
  }

  public void setGrafanaEmbedded(boolean grafanaEmbedded) {
    this.grafanaEmbedded = grafanaEmbedded;
  }
}
