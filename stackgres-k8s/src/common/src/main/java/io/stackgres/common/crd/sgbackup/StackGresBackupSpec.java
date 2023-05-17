/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.common.crd.sgbackup;

import java.util.Objects;

import javax.validation.constraints.NotNull;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.quarkus.runtime.annotations.RegisterForReflection;
import io.stackgres.common.StackGresUtil;
import io.sundr.builder.annotations.Buildable;

@RegisterForReflection
@JsonInclude(JsonInclude.Include.NON_DEFAULT)
@JsonIgnoreProperties(ignoreUnknown = true)
@Buildable(editableEnabled = false, validationEnabled = false, generateBuilderPackage = false,
    lazyCollectionInitEnabled = false, lazyMapInitEnabled = false,
    builderPackage = "io.fabric8.kubernetes.api.builder")
public class StackGresBackupSpec {

  @JsonProperty("sgCluster")
  @NotNull(message = "The cluster name is required")
  private String sgCluster;

  @JsonProperty("managedLifecycle")
  private Boolean managedLifecycle;

  public String getSgCluster() {
    return sgCluster;
  }

  public void setSgCluster(String sgCluster) {
    this.sgCluster = sgCluster;
  }

  public Boolean getManagedLifecycle() {
    return managedLifecycle;
  }

  public void setManagedLifecycle(Boolean managedLifecycle) {
    this.managedLifecycle = managedLifecycle;
  }

  @Override
  public int hashCode() {
    return Objects.hash(managedLifecycle, sgCluster);
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (!(obj instanceof StackGresBackupSpec)) {
      return false;
    }
    StackGresBackupSpec other = (StackGresBackupSpec) obj;
    return Objects.equals(managedLifecycle, other.managedLifecycle)
        && Objects.equals(sgCluster, other.sgCluster);
  }

  @Override
  public String toString() {
    return StackGresUtil.toPrettyYaml(this);
  }

}
