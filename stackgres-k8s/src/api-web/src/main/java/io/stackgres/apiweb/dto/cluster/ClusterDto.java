/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.apiweb.dto.cluster;

import java.util.List;
import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.quarkus.runtime.annotations.RegisterForReflection;
import io.stackgres.apiweb.dto.ResourceDto;
import io.stackgres.common.StackGresUtil;

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

  @JsonProperty("info")
  private ClusterInfoDto info;

  public ClusterSpec getSpec() {
    return spec;
  }

  public void setSpec(ClusterSpec spec) {
    this.spec = spec;
  }

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

  public ClusterInfoDto getInfo() {
    return info;
  }

  public void setInfo(ClusterInfoDto info) {
    this.info = info;
  }

  @Override
  public String toString() {
    return StackGresUtil.toPrettyYaml(this);
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    ClusterDto that = (ClusterDto) o;
    return grafanaEmbedded == that.grafanaEmbedded && Objects.equals(spec, that.spec)
        && Objects.equals(status, that.status) && Objects.equals(pods, that.pods)
        && Objects.equals(podsReady, that.podsReady)
        && Objects.equals(info, that.info);
  }

  @Override
  public int hashCode() {
    return Objects.hash(spec, status, pods, podsReady, grafanaEmbedded, info);
  }
}
