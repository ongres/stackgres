/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.common.crd.sgdbops;

import java.util.List;
import java.util.Objects;

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
public class StackGresDbOpsRepack extends StackGresDbOpsRepackConfig
    implements KubernetesResource {

  private static final long serialVersionUID = 1L;

  @JsonProperty("databases")
  private List<StackGresDbOpsRepackDatabase> databases;

  public List<StackGresDbOpsRepackDatabase> getDatabases() {
    return databases;
  }

  public void setDatabases(List<StackGresDbOpsRepackDatabase> databases) {
    this.databases = databases;
  }

  @Override
  public int hashCode() {
    return Objects.hash(databases, excludeExtension, noAnalyze, noKillBackend, noOrder,
        waitTimeout);
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (!(obj instanceof StackGresDbOpsRepack)) {
      return false;
    }
    StackGresDbOpsRepack other = (StackGresDbOpsRepack) obj;
    return Objects.equals(databases, other.databases)
        && Objects.equals(excludeExtension, other.excludeExtension)
        && Objects.equals(noAnalyze, other.noAnalyze)
        && Objects.equals(noKillBackend, other.noKillBackend)
        && Objects.equals(noOrder, other.noOrder)
        && Objects.equals(waitTimeout, other.waitTimeout);
  }

  @Override
  public String toString() {
    return StackGresUtil.toPrettyYaml(this);
  }

}
