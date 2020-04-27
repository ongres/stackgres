/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.rest.dto.cluster;

import java.util.List;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.google.common.base.MoreObjects;
import io.quarkus.runtime.annotations.RegisterForReflection;
import io.stackgres.operator.rest.dto.ResourceDto;

@JsonDeserialize
@JsonInclude(JsonInclude.Include.NON_DEFAULT)
@RegisterForReflection
public class ClusterDto extends ResourceDto {

  @JsonProperty("spec")
  @NotNull(message = "The specification of the cluster is required")
  @Valid
  private ClusterSpec spec;

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
    return MoreObjects.toStringHelper(this)
        .omitNullValues()
        .add("metadata", getMetadata())
        .add("spec", spec)
        .add("pods", pods)
        .add("podsReady", podsReady)
        .add("grafanaEmbedded", grafanaEmbedded)
        .toString();
  }

}
