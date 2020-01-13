/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.customresource.sgbackup;

import javax.validation.constraints.NotNull;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.google.common.base.MoreObjects;

import io.fabric8.kubernetes.api.model.KubernetesResource;
import io.quarkus.runtime.annotations.RegisterForReflection;

@JsonDeserialize
@JsonInclude(JsonInclude.Include.NON_DEFAULT)
@RegisterForReflection
public class StackGresBackupSpec implements KubernetesResource {

  private static final long serialVersionUID = 4124027524757318245L;

  @JsonProperty("cluster")
  @NotNull(message = "The cluster name is required")
  private String cluster;

  @JsonProperty("isPermanent")
  private Boolean isPermanent;

  public String getCluster() {
    return cluster;
  }

  public void setCluster(String cluster) {
    this.cluster = cluster;
  }

  public Boolean getIsPermanent() {
    return isPermanent;
  }

  public void setIsPermanent(Boolean isPermanent) {
    this.isPermanent = isPermanent;
  }

  @Override
  public String toString() {
    return MoreObjects.toStringHelper(this)
        .omitNullValues()
        .add("cluster", cluster)
        .add("isPermanent", isPermanent)
        .toString();
  }

}
