/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.common.crd.sgcluster;

import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import io.quarkus.runtime.annotations.RegisterForReflection;
import io.stackgres.common.StackGresUtil;
import io.sundr.builder.annotations.Buildable;

@RegisterForReflection
@JsonInclude(JsonInclude.Include.NON_DEFAULT)
@JsonIgnoreProperties(ignoreUnknown = true)
@Buildable(editableEnabled = false, validationEnabled = false, generateBuilderPackage = false,
    lazyCollectionInitEnabled = false, lazyMapInitEnabled = false,
    builderPackage = "io.fabric8.kubernetes.api.builder")
public class StackGresClusterDistributedLogs {

  private String sgDistributedLogs;

  private String retention;

  public String getSgDistributedLogs() {
    return sgDistributedLogs;
  }

  public void setSgDistributedLogs(String sgDistributedLogs) {
    this.sgDistributedLogs = sgDistributedLogs;
  }

  public String getRetention() {
    return retention;
  }

  public void setRetention(String retention) {
    this.retention = retention;
  }

  @Override
  public int hashCode() {
    return Objects.hash(sgDistributedLogs, retention);
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (!(obj instanceof StackGresClusterDistributedLogs)) {
      return false;
    }
    StackGresClusterDistributedLogs other = (StackGresClusterDistributedLogs) obj;
    return Objects.equals(sgDistributedLogs, other.sgDistributedLogs)
        && Objects.equals(retention, other.retention);
  }

  @Override
  public String toString() {
    return StackGresUtil.toPrettyYaml(this);
  }
}
