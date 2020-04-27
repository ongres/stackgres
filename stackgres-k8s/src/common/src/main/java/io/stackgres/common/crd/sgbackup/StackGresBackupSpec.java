/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.common.crd.sgbackup;

import java.util.Objects;
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

  @JsonProperty("sgCluster")
  @NotNull(message = "The cluster name is required")
  private String sgCluster;

  @JsonProperty("subjectToRetentionPolicy")
  private Boolean subjectToRetentionPolicy;

  public String getSgCluster() {
    return sgCluster;
  }

  public void setSgCluster(String sgCluster) {
    this.sgCluster = sgCluster;
  }

  public Boolean getSubjectToRetentionPolicy() {
    return subjectToRetentionPolicy;
  }

  public void setSubjectToRetentionPolicy(Boolean subjectToRetentionPolicy) {
    this.subjectToRetentionPolicy = subjectToRetentionPolicy;
  }

  @Override
  public String toString() {
    return MoreObjects.toStringHelper(this)
        .omitNullValues()
        .add("cluster", sgCluster)
        .add("isPermanent", subjectToRetentionPolicy)
        .toString();
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    StackGresBackupSpec that = (StackGresBackupSpec) o;
    return Objects.equals(sgCluster, that.sgCluster)
        && Objects.equals(subjectToRetentionPolicy, that.subjectToRetentionPolicy);
  }

  @Override
  public int hashCode() {
    return Objects.hash(sgCluster, subjectToRetentionPolicy);
  }
}
