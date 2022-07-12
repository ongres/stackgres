/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.common.crd.sgcluster;

import java.util.Objects;

import javax.validation.constraints.NotNull;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import io.fabric8.kubernetes.api.model.KubernetesResource;
import io.quarkus.runtime.annotations.RegisterForReflection;
import io.stackgres.common.StackGresUtil;
import io.sundr.builder.annotations.Buildable;

@JsonDeserialize
@JsonInclude(JsonInclude.Include.NON_DEFAULT)
@RegisterForReflection
@Buildable(editableEnabled = false, validationEnabled = false, lazyCollectionInitEnabled = false)
public class StackGresClusterDbOpsMinorVersionUpgradeStatus extends ClusterDbOpsRestartStatus
    implements KubernetesResource {

  private static final long serialVersionUID = -1;

  @JsonProperty("sourcePostgresVersion")
  @NotNull
  private String sourcePostgresVersion;

  @JsonProperty("targetPostgresVersion")
  @NotNull
  private String targetPostgresVersion;

  public String getSourcePostgresVersion() {
    return sourcePostgresVersion;
  }

  public void setSourcePostgresVersion(String sourcePostgresVersion) {
    this.sourcePostgresVersion = sourcePostgresVersion;
  }

  public String getTargetPostgresVersion() {
    return targetPostgresVersion;
  }

  public void setTargetPostgresVersion(String targetPostgresVersion) {
    this.targetPostgresVersion = targetPostgresVersion;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    if (!super.equals(o)) {
      return false;
    }
    StackGresClusterDbOpsMinorVersionUpgradeStatus that =
        (StackGresClusterDbOpsMinorVersionUpgradeStatus) o;
    return Objects.equals(sourcePostgresVersion, that.sourcePostgresVersion)
        && Objects.equals(targetPostgresVersion, that.targetPostgresVersion);
  }

  @Override
  public int hashCode() {
    return Objects.hash(super.hashCode(), sourcePostgresVersion, targetPostgresVersion);
  }

  @Override
  public String toString() {
    return StackGresUtil.toPrettyYaml(this);
  }

}
