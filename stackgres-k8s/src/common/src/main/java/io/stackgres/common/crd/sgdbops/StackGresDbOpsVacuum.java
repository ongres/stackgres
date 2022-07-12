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
public class StackGresDbOpsVacuum extends StackGresDbOpsVacuumConfig
    implements KubernetesResource {

  private static final long serialVersionUID = 1L;

  @JsonProperty("databases")
  private List<StackGresDbOpsVacuumDatabase> databases;

  public List<StackGresDbOpsVacuumDatabase> getDatabases() {
    return databases;
  }

  public void setDatabases(List<StackGresDbOpsVacuumDatabase> databases) {
    this.databases = databases;
  }

  @Override
  public int hashCode() {
    return Objects.hash(databases, analyze, disablePageSkipping, freeze, full);
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (!(obj instanceof StackGresDbOpsVacuum)) {
      return false;
    }
    StackGresDbOpsVacuum other = (StackGresDbOpsVacuum) obj;
    return Objects.equals(databases, other.databases)
        && Objects.equals(analyze, other.analyze)
        && Objects.equals(disablePageSkipping, other.disablePageSkipping)
        && Objects.equals(freeze, other.freeze) && Objects.equals(full, other.full);
  }

  @Override
  public String toString() {
    return StackGresUtil.toPrettyYaml(this);
  }

}
