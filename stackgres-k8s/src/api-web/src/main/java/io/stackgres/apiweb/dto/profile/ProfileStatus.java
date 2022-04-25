/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.apiweb.dto.profile;

import java.util.List;
import java.util.Objects;

import javax.validation.constraints.NotNull;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import io.quarkus.runtime.annotations.RegisterForReflection;
import io.stackgres.common.StackGresUtil;

@JsonDeserialize
@JsonInclude(JsonInclude.Include.NON_DEFAULT)
@RegisterForReflection
public class ProfileStatus {

  @JsonProperty("clusters")
  @NotNull(message = "clusters is required")
  private List<String> clusters;

  public List<String> getClusters() {
    return clusters;
  }

  public void setClusters(List<String> clusters) {
    this.clusters = clusters;
  }

  @Override
  public int hashCode() {
    return Objects.hash(clusters);
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (!(obj instanceof ProfileStatus)) {
      return false;
    }
    ProfileStatus other = (ProfileStatus) obj;
    return Objects.equals(clusters, other.clusters);
  }

  @Override
  public String toString() {
    return StackGresUtil.toPrettyYaml(this);
  }

}
