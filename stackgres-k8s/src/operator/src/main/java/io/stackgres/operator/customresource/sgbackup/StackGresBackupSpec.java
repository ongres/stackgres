/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.customresource.sgbackup;

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

  @JsonProperty("clusterName")
  private String clusterName;

  @JsonProperty("isPermenent")
  private Boolean isPermenent;

  public String getClusterName() {
    return clusterName;
  }

  public void setClusterName(String clusterName) {
    this.clusterName = clusterName;
  }

  public Boolean getIsPermenent() {
    return isPermenent;
  }

  public void setIsPermenent(Boolean isPermenent) {
    this.isPermenent = isPermenent;
  }

  @Override
  public String toString() {
    return MoreObjects.toStringHelper(this)
        .omitNullValues()
        .add("clusterName", clusterName)
        .add("isPermenent", isPermenent)
        .toString();
  }

}
