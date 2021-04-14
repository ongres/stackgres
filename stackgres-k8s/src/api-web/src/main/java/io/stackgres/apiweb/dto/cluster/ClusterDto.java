/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.apiweb.dto.cluster;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.quarkus.runtime.annotations.RegisterForReflection;
import io.stackgres.apiweb.dto.ResourceDto;
import io.stackgres.common.StackGresUtil;
import org.jetbrains.annotations.Nullable;

@RegisterForReflection
public class ClusterDto extends ResourceDto {

  @JsonProperty("spec")
  private ClusterSpec spec;

  @JsonProperty("status")
  private ClusterStatus status;

  @JsonProperty("pods")
  private List<KubernetesPod> pods;

  @JsonProperty("podsReady")
  private Integer podsReady;

  @JsonProperty("grafanaEmbedded")
  private boolean grafanaEmbedded;

  public ClusterSpec getSpec() {
    return spec;
  }

  public void setSpec(ClusterSpec spec) {
    this.spec = spec;
  }

  @Nullable
  public ClusterStatus getStatus() {
    return status;
  }

  public void setStatus(ClusterStatus status) {
    this.status = status;
  }

  public List<KubernetesPod> getPods() {
    return pods;
  }

  public void setPods(List<KubernetesPod> pods) {
    this.pods = pods;
  }

  public Integer getPodsReady() {
    return podsReady;
  }

  public void setPodsReady(Integer podsReady) {
    this.podsReady = podsReady;
  }

  public boolean isGrafanaEmbedded() {
    return grafanaEmbedded;
  }

  public void setGrafanaEmbedded(boolean grafanaEmbedded) {
    this.grafanaEmbedded = grafanaEmbedded;
  }

  @Override
  public String toString() {
    return StackGresUtil.toPrettyYaml(this);
  }

}
